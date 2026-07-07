package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.UserSessionResponse;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.entity.UserSessionEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
import fr.achabitation.infrastructure.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountSessionService {
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final SessionTokenService sessionTokenService;
    private final SecurityEventService securityEventService;

    public AccountSessionService(
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            SessionTokenService sessionTokenService,
            SecurityEventService securityEventService
    ) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
        this.sessionTokenService = sessionTokenService;
        this.securityEventService = securityEventService;
    }

    @Transactional
    public String createSession(UserEntity user, String deviceLabel) {
        String rawToken = sessionTokenService.newRawToken();
        String tokenHash = sessionTokenService.hashToken(rawToken);
        Instant now = Instant.now();
        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setTokenHash(tokenHash);
        session.setDeviceLabel(deviceLabel == null || deviceLabel.isBlank() ? "Session" : deviceLabel);
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plus(Duration.ofDays(30)));
        userSessionRepository.save(session);
        // Colonnes legacy conservées pour compatibilité migration/tests ; l'auth réelle utilise user_session.
        user.setSessionTokenHash(tokenHash);
        user.setSessionTokenIssuedAt(now);
        userRepository.save(user);
        return rawToken;
    }

    @Transactional
    public void logoutCurrent(UserEntity user, UUID currentSessionId) {
        Instant now = Instant.now();
        if (currentSessionId != null) {
            userSessionRepository.findById(currentSessionId)
                    .filter(session -> session.getUser().getId().equals(user.getId()))
                    .ifPresent(session -> {
                        session.setRevokedAt(now);
                        userSessionRepository.save(session);
                        securityEventService.log(user, "session.logout_current");
                    });
            return;
        }
        logoutAll(user);
    }

    @Transactional
    public void logoutAll(UserEntity user) {
        Instant now = Instant.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(user.getId()).forEach(session -> {
            session.setRevokedAt(now);
            userSessionRepository.save(session);
        });
        user.setSessionTokenHash(null);
        user.setSessionTokenIssuedAt(null);
        userRepository.save(user);
        securityEventService.log(user, "session.logout_all");
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listSessions(UserEntity user, UUID currentSessionId) {
        return userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(user.getId()).stream()
                .filter(session -> session.isActive(Instant.now()))
                .map(session -> new UserSessionResponse(
                        session.getId(),
                        session.getDeviceLabel(),
                        session.getCreatedAt(),
                        session.getLastUsedAt(),
                        session.getExpiresAt(),
                        currentSessionId != null && currentSessionId.equals(session.getId())
                ))
                .toList();
    }

    @Transactional
    public void revokeSession(UserEntity user, UUID sessionId) {
        UserSessionEntity session = userSessionRepository.findById(sessionId)
                .filter(value -> value.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable."));
        session.setRevokedAt(Instant.now());
        userSessionRepository.save(session);
        securityEventService.log(user, "session.revoked_specific");
    }
}
