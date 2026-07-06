package fr.achabitation.infrastructure.entity;

import fr.achabitation.domain.model.WeightMode;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "person", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "normalized_name"}))
public class PersonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private TripEntity trip;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne
    @JoinColumn(name = "linked_user_id")
    private UserEntity linkedUser;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal livingRest = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WeightMode weightMode = WeightMode.LIVING_REST;

    @Column(nullable = false)
    private boolean advancedLivingRest = false;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netIncomeAfterTax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal credits = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedCharges = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal transport = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal insurance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal otherMandatoryExpenses = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "numeric(12,2) default 0")
    private BigDecimal menstrualProtection = BigDecimal.ZERO;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean livingRestPublic = false;

    @Column(nullable = false)
    private boolean vegetarian = false;

    @Column(nullable = false)
    private boolean noAlcohol = false;

    @ElementCollection
    @CollectionTable(name = "person_custom_constraint", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "constraint_name", nullable = false, length = 120)
    private Set<String> customConstraints = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate ASC")
    private List<PresencePeriodEntity> presencePeriods = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TripEntity getTrip() { return trip; }
    public void setTrip(TripEntity trip) { this.trip = trip; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UserEntity getLinkedUser() { return linkedUser; }
    public void setLinkedUser(UserEntity linkedUser) { this.linkedUser = linkedUser; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public BigDecimal getLivingRest() { return livingRest; }
    public void setLivingRest(BigDecimal livingRest) { this.livingRest = livingRest; }
    public WeightMode getWeightMode() { return weightMode; }
    public void setWeightMode(WeightMode weightMode) { this.weightMode = weightMode; }
    public boolean isAdvancedLivingRest() { return advancedLivingRest; }
    public void setAdvancedLivingRest(boolean advancedLivingRest) { this.advancedLivingRest = advancedLivingRest; }
    public BigDecimal getNetIncomeAfterTax() { return netIncomeAfterTax; }
    public void setNetIncomeAfterTax(BigDecimal netIncomeAfterTax) { this.netIncomeAfterTax = netIncomeAfterTax; }
    public BigDecimal getRent() { return rent; }
    public void setRent(BigDecimal rent) { this.rent = rent; }
    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }
    public BigDecimal getFixedCharges() { return fixedCharges; }
    public void setFixedCharges(BigDecimal fixedCharges) { this.fixedCharges = fixedCharges; }
    public BigDecimal getTransport() { return transport; }
    public void setTransport(BigDecimal transport) { this.transport = transport; }
    public BigDecimal getInsurance() { return insurance; }
    public void setInsurance(BigDecimal insurance) { this.insurance = insurance; }
    public BigDecimal getOtherMandatoryExpenses() { return otherMandatoryExpenses; }
    public void setOtherMandatoryExpenses(BigDecimal otherMandatoryExpenses) { this.otherMandatoryExpenses = otherMandatoryExpenses; }
    public BigDecimal getMenstrualProtection() { return menstrualProtection; }
    public void setMenstrualProtection(BigDecimal menstrualProtection) { this.menstrualProtection = menstrualProtection; }
    public boolean isLivingRestPublic() { return livingRestPublic; }
    public void setLivingRestPublic(boolean livingRestPublic) { this.livingRestPublic = livingRestPublic; }
    public boolean isVegetarian() { return vegetarian; }
    public void setVegetarian(boolean vegetarian) { this.vegetarian = vegetarian; }
    public boolean isNoAlcohol() { return noAlcohol; }
    public void setNoAlcohol(boolean noAlcohol) { this.noAlcohol = noAlcohol; }
    public Set<String> getCustomConstraints() { return customConstraints; }
    public void setCustomConstraints(Set<String> customConstraints) {
        this.customConstraints = customConstraints == null ? new LinkedHashSet<>() : customConstraints;
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<PresencePeriodEntity> getPresencePeriods() { return presencePeriods; }
    public void setPresencePeriods(List<PresencePeriodEntity> presencePeriods) { this.presencePeriods = presencePeriods; }
}
