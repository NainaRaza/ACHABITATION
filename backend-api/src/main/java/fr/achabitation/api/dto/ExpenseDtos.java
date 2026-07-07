package fr.achabitation.api.dto;

import fr.achabitation.domain.model.ExpenseType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
            @NotNull @Positive @DecimalMax("999999999.99") BigDecimal totalAmount,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal meatAmount,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal alcoholAmount,
            @Size(max = 50) Map<@NotBlank @Size(max = 120) String, @PositiveOrZero @DecimalMax("999999999.99") BigDecimal> customConstraintAmounts,
            ExpenseType type,
            boolean advancedMode,
            @Size(max = 200) Set<UUID> manualParticipantIds,
            @Pattern(regexp = "[A-Z]{3}", message = "doit être une devise ISO 4217 sur 3 lettres majuscules") String currency,
            @Positive @DecimalMax("999999.99999999") BigDecimal exchangeRateToTripCurrency
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
