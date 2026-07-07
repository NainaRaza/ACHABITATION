package fr.achabitation.application;

import fr.achabitation.api.dto.AuditDtos.AuditLogResponse;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditQueryService {
    private final AuditLogRepository auditLogRepository;
    private final EntityMapper mapper;
    private final AuthorizationService authorizationService;

    public AuditQueryService(AuditLogRepository auditLogRepository, EntityMapper mapper, AuthorizationService authorizationService) {
        this.auditLogRepository = auditLogRepository;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> list(UUID tripId, UserEntity actor) {
        authorizationService.requireAdmin(tripId, actor);
        return auditLogRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(mapper::toAuditResponse)
                .toList();
    }
}
