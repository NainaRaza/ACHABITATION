package fr.achabitation.infrastructure.entity;

import fr.achabitation.domain.model.ExpenseType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "expense")
public class ExpenseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private TripEntity trip;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payer_person_id")
    private PersonEntity payer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal meatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal alcoholAmount = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "expense_custom_constraint_amount", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "constraint_name", length = 120)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private Map<String, BigDecimal> customConstraintAmounts = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseType type = ExpenseType.NORMAL;

    @Column(nullable = false)
    private boolean advancedMode = false;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(nullable = false, precision = 16, scale = 8)
    private BigDecimal exchangeRateToTripCurrency = BigDecimal.ONE;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("person.id ASC")
    private List<ExpenseParticipantEntity> manualParticipants = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TripEntity getTrip() { return trip; }
    public void setTrip(TripEntity trip) { this.trip = trip; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public PersonEntity getPayer() { return payer; }
    public void setPayer(PersonEntity payer) { this.payer = payer; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getMeatAmount() { return meatAmount; }
    public void setMeatAmount(BigDecimal meatAmount) { this.meatAmount = meatAmount; }
    public BigDecimal getAlcoholAmount() { return alcoholAmount; }
    public void setAlcoholAmount(BigDecimal alcoholAmount) { this.alcoholAmount = alcoholAmount; }
    public Map<String, BigDecimal> getCustomConstraintAmounts() { return customConstraintAmounts; }
    public void setCustomConstraintAmounts(Map<String, BigDecimal> customConstraintAmounts) { this.customConstraintAmounts = customConstraintAmounts; }
    public ExpenseType getType() { return type; }
    public void setType(ExpenseType type) { this.type = type; }
    public boolean isAdvancedMode() { return advancedMode; }
    public void setAdvancedMode(boolean advancedMode) { this.advancedMode = advancedMode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getExchangeRateToTripCurrency() { return exchangeRateToTripCurrency; }
    public void setExchangeRateToTripCurrency(BigDecimal exchangeRateToTripCurrency) { this.exchangeRateToTripCurrency = exchangeRateToTripCurrency; }
    public List<ExpenseParticipantEntity> getManualParticipants() { return manualParticipants; }
    public void setManualParticipants(List<ExpenseParticipantEntity> manualParticipants) { this.manualParticipants = manualParticipants; }
}
