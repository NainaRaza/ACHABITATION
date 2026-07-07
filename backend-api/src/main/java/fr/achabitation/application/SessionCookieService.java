package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SessionCookieService {
    public static final String SESSION_COOKIE_NAME = "ACHABITATION_SESSION";

    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public SessionCookieService(
            @Value("${achabitation.auth.cookie.secure:false}") boolean secure,
            @Value("${achabitation.auth.cookie.same-site:Strict}") String sameSite,
            @Value("${achabitation.auth.cookie.max-age-days:30}") long maxAgeDays
    ) {
        this.secure = secure;
        this.sameSite = normalizeSameSite(sameSite);
        this.maxAge = Duration.ofDays(Math.max(1, maxAgeDays));
    }

    public void writeSessionCookie(HttpServletResponse response, AuthResponse authResponse) {
        if (authResponse == null || authResponse.accessToken() == null || authResponse.accessToken().isBlank()) {
            return;
        }
        response.addHeader("Set-Cookie", buildCookie(authResponse.accessToken(), maxAge.getSeconds()));
    }

    public void clearSessionCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("", 0));
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        StringBuilder cookie = new StringBuilder(SESSION_COOKIE_NAME)
                .append("=")
                .append(value == null ? "" : value)
                .append("; Path=/")
                .append("; Max-Age=")
                .append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=")
                .append(sameSite);
        if (secure) {
            cookie.append("; Secure");
        }
        return cookie.toString();
    }

    private String normalizeSameSite(String value) {
        if (value == null || value.isBlank()) {
            return "Strict";
        }
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("Strict")) {
            return "Strict";
        }
        if (normalized.equalsIgnoreCase("Lax")) {
            return "Lax";
        }
        if (normalized.equalsIgnoreCase("None")) {
            return "None";
        }
        return "Strict";
    }
}
