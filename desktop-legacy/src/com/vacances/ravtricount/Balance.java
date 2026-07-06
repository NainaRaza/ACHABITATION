package com.vacances.ravtricount;

import java.math.BigDecimal;

public class Balance {
    private final Person person;
    private final BigDecimal totalPaid;
    private final BigDecimal totalOwed;
    private final BigDecimal balance;

    public Balance(Person person, BigDecimal totalPaid, BigDecimal totalOwed) {
        this.person = person;
        this.totalPaid = totalPaid;
        this.totalOwed = totalOwed;
        this.balance = totalPaid.subtract(totalOwed);
    }

    public Person getPerson() {
        return person;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public BigDecimal getTotalOwed() {
        return totalOwed;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
