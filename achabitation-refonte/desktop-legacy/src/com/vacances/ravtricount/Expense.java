package com.vacances.ravtricount;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Expense implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String title;
    private LocalDate date;
    private String payerId;
    private BigDecimal totalAmount;
    private BigDecimal meatAmount;
    private BigDecimal alcoholAmount;
    private ExpenseType type;
    private boolean advancedMode;
    private Set<String> manualParticipantIds;

    public Expense(String title, LocalDate date, String payerId, BigDecimal totalAmount,
                   BigDecimal meatAmount, BigDecimal alcoholAmount, ExpenseType type,
                   boolean advancedMode, Set<String> manualParticipantIds) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.date = date;
        this.payerId = payerId;
        this.totalAmount = totalAmount;
        this.meatAmount = meatAmount;
        this.alcoholAmount = alcoholAmount;
        this.type = type == null ? ExpenseType.NORMAL : type;
        this.advancedMode = advancedMode;
        this.manualParticipantIds = manualParticipantIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(manualParticipantIds);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getMeatAmount() {
        return meatAmount;
    }

    public void setMeatAmount(BigDecimal meatAmount) {
        this.meatAmount = meatAmount;
    }

    public BigDecimal getAlcoholAmount() {
        return alcoholAmount;
    }

    public void setAlcoholAmount(BigDecimal alcoholAmount) {
        this.alcoholAmount = alcoholAmount;
    }

    public ExpenseType getType() {
        return type;
    }

    public void setType(ExpenseType type) {
        this.type = type == null ? ExpenseType.NORMAL : type;
    }

    public boolean isAdvancedMode() {
        return advancedMode;
    }

    public void setAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
    }

    public Set<String> getManualParticipantIds() {
        return manualParticipantIds;
    }

    public void setManualParticipantIds(Set<String> manualParticipantIds) {
        this.manualParticipantIds = manualParticipantIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(manualParticipantIds);
    }
}
