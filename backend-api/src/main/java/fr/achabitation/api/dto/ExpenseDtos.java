package fr.achabitation.api.dto;

import fr.achabitation.domain.model.ExpenseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExpenseDtos {
    private ExpenseDtos() {}

    public record ExpenseCreateRequest(
            @NotBlank @Size(max = 180) String title,
            @NotNull LocalDate date,
            @NotNull UUID payerPersonId,
            @NotNull @Positive BigDecimal totalAmount,
            @PositiveOrZero BigDecimal meatAmount,
            @PositiveOrZero BigDecimal alcoholAmount,
            Map<String, BigDecimal> customConstraintAmounts,
            ExpenseType type,
            boolean advancedMode,
            Set<UUID> manualParticipantIds,
            @Size(min = 3, max = 3) String currency,
            @Positive BigDecimal exchangeRateToTripCurrency
    ) {}

    public record ExpenseResponse(
            UUID id,
            String title,
            LocalDate date,
            UUID payerPersonId,
            String payerName,
            BigDecimal totalAmount,
            BigDecimal meatAmount,
            BigDecimal alcoholAmount,
            Map<String, BigDecimal> customConstraintAmounts,
            ExpenseType type,
            boolean advancedMode,
            Set<UUID> manualParticipantIds,
            String currency,
            BigDecimal exchangeRateToTripCurrency
    ) {}
}
