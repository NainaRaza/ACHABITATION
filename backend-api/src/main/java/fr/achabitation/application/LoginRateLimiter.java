package fr.achabitation.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 12;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public void check(HttpServletRequest request, String action) {
        String key = action + ":" + clientIp(request);
        Instant now = Instant.now();
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            Instant limit = now.minus(WINDOW);
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(limit)) {
                attempts.removeFirst();
            }
            if (attempts.size() >= MAX_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives. Réessaie dans quelques minutes.");
            }
            attempts.addLast(now);
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
