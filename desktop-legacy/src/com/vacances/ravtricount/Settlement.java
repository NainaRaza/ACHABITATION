package com.vacances.ravtricount;

import java.math.BigDecimal;

public class Settlement {
    private final Person from;
    private final Person to;
    private final BigDecimal amount;

    public Settlement(Person from, Person to, BigDecimal amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public Person getFrom() {
        return from;
    }

    public Person getTo() {
        return to;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
