package fr.achabitation.application;

import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthContextService {
    private final UserRepository userRepository;

    public AuthContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> optionalUser(HttpServletRequest request) {
        String token = tokenFromRequest(request);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<UserEntity> user = userRepository.findBySessionToken(token)
                .filter(this::tokenStillValid);
        user.ifPresent(value -> {
            value.getKnownCustomConstraints().size();
            value.getCustomConstraints().size();
        });
        return user;
    }

    @Transactional(readOnly = true)
    public UserEntity requiredUser(HttpServletRequest request) {
        return optionalUser(request).orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentification requise."));
    }

    private boolean tokenStillValid(UserEntity user) {
        if (user.getSessionTokenIssuedAt() == null) {
            return false;
        }
        return user.getSessionTokenIssuedAt().plus(Duration.ofDays(30)).isAfter(Instant.now());
    }

    private String tokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        String devToken = request.getHeader("X-Dev-Token");
        return devToken == null ? null : devToken.trim();
    }
}
