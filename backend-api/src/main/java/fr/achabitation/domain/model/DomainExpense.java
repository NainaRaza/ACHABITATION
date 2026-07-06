package fr.achabitation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record DomainExpense(
        UUID id,
        String title,
        LocalDate date,
        UUID payerId,
        BigDecimal totalAmount,
        BigDecimal meatAmount,
        BigDecimal alcoholAmount,
        Map<String, BigDecimal> customConstraintAmounts,
        ExpenseType type,
        boolean advancedMode,
        Set<UUID> manualParticipantIds,
        String currency,
        BigDecimal exchangeRateToTripCurrency
) {
}
