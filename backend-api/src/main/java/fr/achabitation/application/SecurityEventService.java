package fr.achabitation.application;

import fr.achabitation.infrastructure.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityEventService {
    private static final Logger log = LoggerFactory.getLogger(SecurityEventService.class);

    public void log(UserEntity user, String event) {
        if (user == null) {
            log.info("security_event={} user_id=anonymous", safe(event));
            return;
        }
        log.info("security_event={} user_id={} email_hash={}", safe(event), user.getId(), Integer.toHexString((user.getEmail() == null ? "" : user.getEmail()).hashCode()));
    }

    public void logEmailOnly(String email, String event) {
        log.info("security_event={} email_hash={}", safe(event), Integer.toHexString((email == null ? "" : email.toLowerCase()).hashCode()));
    }

    private String safe(String event) {
        return event == null ? "unknown" : event.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
