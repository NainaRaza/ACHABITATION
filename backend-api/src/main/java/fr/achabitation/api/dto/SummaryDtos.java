package fr.achabitation.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class SummaryDtos {
    private SummaryDtos() {}

    public record BalanceResponse(
            UUID personId,
            String personName,
            BigDecimal totalPaid,
            BigDecimal totalOwed,
            BigDecimal balance
    ) {}

    public record SettlementResponse(
            UUID fromPersonId,
            String fromPersonName,
            UUID toPersonId,
            String toPersonName,
            BigDecimal amount
    ) {}

    public record SummaryResponse(
            String referenceCurrency,
            List<BalanceResponse> balances,
            List<SettlementResponse> settlements
    ) {}
}
