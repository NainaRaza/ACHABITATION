package fr.achabitation.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "application", "ACHABITATION",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/readiness")
    public Map<String, Object> readiness() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
        }
        return Map.of(
                "status", "READY",
                "database", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
