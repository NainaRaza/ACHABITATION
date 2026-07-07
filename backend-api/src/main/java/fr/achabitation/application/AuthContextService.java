package fr.achabitation.application;

import fr.achabitation.config.SessionTokenAuthenticationFilter;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthContextService {
    private final UserRepository userRepository;

    public AuthContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> optionalUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        Optional<UUID> userId = parseUserId(authentication.getPrincipal());
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        Optional<UserEntity> user = userRepository.findById(userId.get());
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

    public Optional<UUID> currentSessionId(HttpServletRequest request) {
        Object value = request == null ? null : request.getAttribute(SessionTokenAuthenticationFilter.CURRENT_SESSION_ID_ATTRIBUTE);
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        return Optional.empty();
    }

    private Optional<UUID> parseUserId(Object principal) {
        if (principal instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (principal instanceof String value && !value.isBlank() && !"anonymousUser".equals(value)) {
            try {
                return Optional.of(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
