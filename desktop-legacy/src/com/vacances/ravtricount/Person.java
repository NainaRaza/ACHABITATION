package com.vacances.ravtricount;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private BigDecimal livingRest;
    private boolean averageWeight;
    private boolean advancedLivingRest;
    private BigDecimal netIncomeAfterTax;
    private BigDecimal rent;
    private BigDecimal credits;
    private BigDecimal fixedCharges;
    private BigDecimal transport;
    private BigDecimal insurance;
    private BigDecimal otherMandatoryExpenses;
    private boolean vegetarian;
    private boolean noAlcohol;
    private LocalDate presenceStart;
    private LocalDate presenceEnd;
    private boolean active;

    public Person(String name, BigDecimal livingRest, boolean vegetarian, boolean noAlcohol,
                  LocalDate presenceStart, LocalDate presenceEnd) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.livingRest = scale(livingRest);
        this.averageWeight = false;
        this.advancedLivingRest = false;
        this.netIncomeAfterTax = zero();
        this.rent = zero();
        this.credits = zero();
        this.fixedCharges = zero();
        this.transport = zero();
        this.insurance = zero();
        this.otherMandatoryExpenses = zero();
        this.vegetarian = vegetarian;
        this.noAlcohol = noAlcohol;
        this.presenceStart = presenceStart;
        this.presenceEnd = presenceEnd;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getLivingRest() {
        return scale(livingRest);
    }

    public void setLivingRest(BigDecimal livingRest) {
        this.livingRest = scale(livingRest);
    }

    public boolean isAverageWeight() {
        return averageWeight;
    }

    public void setAverageWeight(boolean averageWeight) {
        this.averageWeight = averageWeight;
    }

    public boolean isAdvancedLivingRest() {
        return advancedLivingRest;
    }

    public void setAdvancedLivingRest(boolean advancedLivingRest) {
        this.advancedLivingRest = advancedLivingRest;
    }

    public BigDecimal getNetIncomeAfterTax() {
        return scale(netIncomeAfterTax);
    }

    public void setNetIncomeAfterTax(BigDecimal netIncomeAfterTax) {
        this.netIncomeAfterTax = scale(netIncomeAfterTax);
    }

    public BigDecimal getRent() {
        return scale(rent);
    }

    public void setRent(BigDecimal rent) {
        this.rent = scale(rent);
    }

    public BigDecimal getCredits() {
        return scale(credits);
    }

    public void setCredits(BigDecimal credits) {
        this.credits = scale(credits);
    }

    public BigDecimal getFixedCharges() {
        return scale(fixedCharges);
    }

    public void setFixedCharges(BigDecimal fixedCharges) {
        this.fixedCharges = scale(fixedCharges);
    }

    public BigDecimal getTransport() {
        return scale(transport);
    }

    public void setTransport(BigDecimal transport) {
        this.transport = scale(transport);
    }

    public BigDecimal getInsurance() {
        return scale(insurance);
    }

    public void setInsurance(BigDecimal insurance) {
        this.insurance = scale(insurance);
    }

    public BigDecimal getOtherMandatoryExpenses() {
        return scale(otherMandatoryExpenses);
    }

    public void setOtherMandatoryExpenses(BigDecimal otherMandatoryExpenses) {
        this.otherMandatoryExpenses = scale(otherMandatoryExpenses);
    }

    public BigDecimal calculateAdvancedLivingRest() {
        return getNetIncomeAfterTax()
                .subtract(getRent())
                .subtract(getCredits())
                .subtract(getFixedCharges())
                .subtract(getTransport())
                .subtract(getInsurance())
                .subtract(getOtherMandatoryExpenses())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public boolean isNoAlcohol() {
        return noAlcohol;
    }

    public void setNoAlcohol(boolean noAlcohol) {
        this.noAlcohol = noAlcohol;
    }

    public LocalDate getPresenceStart() {
        return presenceStart;
    }

    public void setPresenceStart(LocalDate presenceStart) {
        this.presenceStart = presenceStart;
    }

    public LocalDate getPresenceEnd() {
        return presenceEnd;
    }

    public void setPresenceEnd(LocalDate presenceEnd) {
        this.presenceEnd = presenceEnd;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPresentOn(LocalDate date) {
        if (date == null || presenceStart == null || presenceEnd == null) {
            return false;
        }
        return !date.isBefore(presenceStart) && !date.isAfter(presenceEnd);
    }

    @Override
    public String toString() {
        return name == null || name.isBlank() ? "Personne sans nom" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person person)) return false;
        return Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }
}
