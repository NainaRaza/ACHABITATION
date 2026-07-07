package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByTripIdOrderByCreatedAtDesc(UUID tripId);
    List<AuditLogEntity> findByActorIdOrderByCreatedAtDesc(UUID actorId);
}
