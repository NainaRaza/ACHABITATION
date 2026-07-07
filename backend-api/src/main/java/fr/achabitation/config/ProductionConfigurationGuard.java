package fr.achabitation.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ProductionConfigurationGuard implements ApplicationRunner {
    private static final List<String> REQUIRED_PROD_PROPERTIES = List.of(
            "DATABASE_URL",
            "DATABASE_USER",
            "DATABASE_PASSWORD",
            "CORS_ALLOWED_ORIGINS",
            "APP_PUBLIC_URL",
            "SMTP_HOST",
            "SMTP_USERNAME",
            "SMTP_PASSWORD",
            "MAIL_FROM"
    );

    private final Environment environment;

    public ProductionConfigurationGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!prod) {
            return;
        }
        for (String key : REQUIRED_PROD_PROPERTIES) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Configuration production incomplète : variable obligatoire absente " + key);
            }
        }
        String origins = environment.getProperty("CORS_ALLOWED_ORIGINS", "");
        if (origins.contains("*")) {
            throw new IllegalStateException("Configuration production invalide : CORS_ALLOWED_ORIGINS ne doit pas contenir de wildcard.");
        }
        String publicUrl = environment.getProperty("APP_PUBLIC_URL", "");
        if (!publicUrl.startsWith("https://")) {
            throw new IllegalStateException("Configuration production invalide : APP_PUBLIC_URL doit être en HTTPS.");
        }
        String mailFrom = environment.getProperty("MAIL_FROM", "");
        if (!mailFrom.contains("@")) {
            throw new IllegalStateException("Configuration production invalide : MAIL_FROM doit être un email valide.");
        }
    }
}
