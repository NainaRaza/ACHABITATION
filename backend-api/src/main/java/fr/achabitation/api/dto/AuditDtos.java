package fr.achabitation.api.dto;

import fr.achabitation.infrastructure.entity.AuditAction;
import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {}

    public record AuditLogResponse(
            UUID id,
            AuditAction action,
            String entityType,
            UUID entityId,
            String description,
            UUID actorUserId,
            Instant createdAt
    ) {}
}
