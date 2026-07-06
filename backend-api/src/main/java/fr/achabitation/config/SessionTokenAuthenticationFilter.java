package fr.achabitation.config;

import fr.achabitation.application.SessionTokenService;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final SessionTokenService sessionTokenService;

    public SessionTokenAuthenticationFilter(UserRepository userRepository, SessionTokenService sessionTokenService) {
        this.userRepository = userRepository;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String rawToken = tokenFromRequest(request);
        if (rawToken != null && !rawToken.isBlank()) {
            userRepository.findBySessionTokenHash(sessionTokenService.hashToken(rawToken))
                    .filter(this::tokenStillValid)
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user.getId().toString(), null, Collections.emptyList())
                    ));
        }
        filterChain.doFilter(request, response);
    }

    private boolean tokenStillValid(UserEntity user) {
        return user.getSessionTokenIssuedAt() != null
                && user.getSessionTokenIssuedAt().plus(Duration.ofDays(30)).isAfter(Instant.now());
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
