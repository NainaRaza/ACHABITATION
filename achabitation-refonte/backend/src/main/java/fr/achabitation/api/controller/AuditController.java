package fr.achabitation.api.controller;

import fr.achabitation.api.dto.AuditDtos.AuditLogResponse;
import fr.achabitation.application.AuditQueryService;
import fr.achabitation.application.AuthContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/audit-logs")
public class AuditController {
    private final AuditQueryService auditQueryService;
    private final AuthContextService authContextService;

    public AuditController(AuditQueryService auditQueryService, AuthContextService authContextService) {
        this.auditQueryService = auditQueryService;
        this.authContextService = authContextService;
    }

    @GetMapping
    public List<AuditLogResponse> list(@PathVariable UUID tripId, HttpServletRequest httpRequest) {
        return auditQueryService.list(tripId, authContextService.requiredUser(httpRequest));
    }
}
