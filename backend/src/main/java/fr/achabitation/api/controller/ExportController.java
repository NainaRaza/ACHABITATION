package fr.achabitation.api.controller;

import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/exports")
public class ExportController {
    private final ExportService exportService;
    private final AuthContextService authContextService;

    public ExportController(ExportService exportService, AuthContextService authContextService) {
        this.exportService = exportService;
        this.authContextService = authContextService;
    }

    @GetMapping("/expenses.csv")
    public ResponseEntity<byte[]> expensesCsv(@PathVariable UUID tripId, HttpServletRequest request) {
        return csv("depenses.csv", exportService.expensesCsv(tripId, authContextService.requiredUser(request)));
    }

    @GetMapping("/summary.csv")
    public ResponseEntity<byte[]> summaryCsv(@PathVariable UUID tripId, HttpServletRequest request) {
        return csv("resume.csv", exportService.summaryCsv(tripId, authContextService.requiredUser(request)));
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] payload) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv"))
                .body(payload);
    }
}
