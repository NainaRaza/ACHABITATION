package fr.achabitation.application;

import fr.achabitation.infrastructure.entity.AuditAction;
import fr.achabitation.infrastructure.entity.AuditLogEntity;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(TripEntity trip, UserEntity actor, AuditAction action, String entityType, UUID entityId, String description) {
        AuditLogEntity log = new AuditLogEntity();
        log.setTrip(trip);
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        auditLogRepository.save(log);
    }
}
