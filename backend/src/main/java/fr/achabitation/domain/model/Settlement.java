package fr.achabitation.domain.model;

import java.math.BigDecimal;

public record Settlement(
        DomainPerson from,
        DomainPerson to,
        BigDecimal amount
) {
}
