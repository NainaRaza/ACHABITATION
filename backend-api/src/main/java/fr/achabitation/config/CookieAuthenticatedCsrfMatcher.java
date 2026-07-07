package fr.achabitation.config;

import fr.achabitation.application.SessionCookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CookieAuthenticatedCsrfMatcher implements RequestMatcher {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

    @Override
    public boolean matches(HttpServletRequest request) {
        if (request == null || SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }
        if (hasHeader(request, "Authorization") || hasHeader(request, "X-Session-Token")) {
            return false;
        }
        return hasCookie(request, SessionCookieService.SESSION_COOKIE_NAME);
    }

    private boolean hasHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null && !value.isBlank();
    }

    private boolean hasCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }
}
