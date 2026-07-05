package fr.achabitation.api.controller;

import fr.achabitation.api.dto.ExpenseDtos.ExpenseCreateRequest;
import fr.achabitation.api.dto.ExpenseDtos.ExpenseResponse;
import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.ExpenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;
    private final AuthContextService authContextService;

    public ExpenseController(ExpenseService expenseService, AuthContextService authContextService) {
        this.expenseService = expenseService;
        this.authContextService = authContextService;
    }

    @PostMapping
    public ExpenseResponse create(@PathVariable UUID tripId, @Valid @RequestBody ExpenseCreateRequest request, HttpServletRequest httpRequest) {
        return expenseService.create(tripId, request, authContextService.requiredUser(httpRequest));
    }

    @GetMapping
    public List<ExpenseResponse> list(@PathVariable UUID tripId, HttpServletRequest httpRequest) {
        return expenseService.list(tripId, authContextService.requiredUser(httpRequest));
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse update(@PathVariable UUID tripId, @PathVariable UUID expenseId, @Valid @RequestBody ExpenseCreateRequest request, HttpServletRequest httpRequest) {
        return expenseService.update(tripId, expenseId, request, authContextService.requiredUser(httpRequest));
    }

    @DeleteMapping("/{expenseId}")
    public void delete(@PathVariable UUID tripId, @PathVariable UUID expenseId, HttpServletRequest httpRequest) {
        expenseService.delete(tripId, expenseId, authContextService.requiredUser(httpRequest));
    }
}
