package fr.achabitation.api.controller;

import fr.achabitation.api.dto.SummaryDtos.SummaryResponse;
import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.SummaryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/summary")
public class SummaryController {
    private final SummaryService summaryService;
    private final AuthContextService authContextService;

    public SummaryController(SummaryService summaryService, AuthContextService authContextService) {
        this.summaryService = summaryService;
        this.authContextService = authContextService;
    }

    @GetMapping
    public SummaryResponse summary(@PathVariable UUID tripId, HttpServletRequest httpRequest) {
        return summaryService.summary(tripId, authContextService.requiredUser(httpRequest));
    }
}
