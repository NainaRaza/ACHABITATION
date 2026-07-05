package fr.achabitation.domain.model;

import java.math.BigDecimal;

public record Balance(
        DomainPerson person,
        BigDecimal totalPaid,
        BigDecimal totalOwed,
        BigDecimal balance
) {
}
