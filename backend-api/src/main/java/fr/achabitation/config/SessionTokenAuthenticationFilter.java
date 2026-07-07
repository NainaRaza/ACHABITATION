package fr.achabitation.config;

import fr.achabitation.application.SessionTokenService;
import fr.achabitation.infrastructure.repository.UserRepository;
import fr.achabitation.infrastructure.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

@Component
public class SessionTokenAuthenticationFilter extends OncePerRequestFilter {
    public static final String CURRENT_SESSION_ID_ATTRIBUTE = "achabitation.currentSessionId";

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final SessionTokenService sessionTokenService;

    public SessionTokenAuthenticationFilter(UserRepository userRepository, UserSessionRepository userSessionRepository, SessionTokenService sessionTokenService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String rawToken = tokenFromRequest(request);
        if (rawToken != null && !rawToken.isBlank()) {
            String tokenHash = sessionTokenService.hashToken(rawToken);
            Instant now = Instant.now();
            userSessionRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                    .filter(session -> session.isActive(now))
                    .ifPresent(session -> {
                        session.setLastUsedAt(now);
                        userSessionRepository.save(session);
                        request.setAttribute(CURRENT_SESSION_ID_ATTRIBUTE, session.getId());
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(session.getUser().getId().toString(), null, Collections.emptyList())
                        );
                    });

            // Fallback transitoire pour les tokens créés avant la migration user_session.
            // Si une ligne user_session existe, elle est la source d'autorité : un token expiré
            // ou révoqué ne doit pas être réhabilité par les colonnes legacy de app_user.
            if (SecurityContextHolder.getContext().getAuthentication() == null && !userSessionRepository.existsByTokenHash(tokenHash)) {
                userRepository.findBySessionTokenHash(tokenHash)
                        .filter(user -> user.getSessionTokenIssuedAt() != null)
                        .filter(user -> user.getSessionTokenIssuedAt().plus(Duration.ofDays(30)).isAfter(now))
                        .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user.getId().toString(), null, Collections.emptyList())
                        ));
            }
        }
        filterChain.doFilter(request, response);
    }

    private String tokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        String sessionToken = request.getHeader("X-Session-Token");
        return sessionToken == null ? null : sessionToken.trim();
    }
}
