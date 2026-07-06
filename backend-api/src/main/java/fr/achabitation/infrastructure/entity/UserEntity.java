package fr.achabitation.infrastructure.entity;

import fr.achabitation.domain.model.WeightMode;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "session_token_hash", length = 128)
    private String sessionTokenHash;

    private Instant sessionTokenIssuedAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal livingRest = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WeightMode weightMode = WeightMode.LIVING_REST;

    @Column
    private boolean advancedLivingRest = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal netIncomeAfterTax = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal rent = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal credits = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal fixedCharges = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal transport = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal insurance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal otherMandatoryExpenses = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal menstrualProtection = BigDecimal.ZERO;

    @Column
    private boolean vegetarian = false;

    @Column
    private boolean noAlcohol = false;

    @Column
    private boolean livingRestPublic = false;

    @ElementCollection
    @CollectionTable(name = "user_known_custom_constraint", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "constraint_name", nullable = false, length = 120)
    private Set<String> knownCustomConstraints = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "user_custom_constraint", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "constraint_name", nullable = false, length = 120)
    private Set<String> customConstraints = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSessionTokenHash() { return sessionTokenHash; }
    public void setSessionTokenHash(String sessionTokenHash) { this.sessionTokenHash = sessionTokenHash; }
    public Instant getSessionTokenIssuedAt() { return sessionTokenIssuedAt; }
    public void setSessionTokenIssuedAt(Instant sessionTokenIssuedAt) { this.sessionTokenIssuedAt = sessionTokenIssuedAt; }
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
    public boolean isVegetarian() { return vegetarian; }
    public void setVegetarian(boolean vegetarian) { this.vegetarian = vegetarian; }
    public boolean isNoAlcohol() { return noAlcohol; }
    public void setNoAlcohol(boolean noAlcohol) { this.noAlcohol = noAlcohol; }
    public boolean isLivingRestPublic() { return livingRestPublic; }
    public void setLivingRestPublic(boolean livingRestPublic) { this.livingRestPublic = livingRestPublic; }
    public Set<String> getKnownCustomConstraints() { return knownCustomConstraints; }
    public void setKnownCustomConstraints(Set<String> knownCustomConstraints) { this.knownCustomConstraints = knownCustomConstraints == null ? new LinkedHashSet<>() : knownCustomConstraints; }
    public Set<String> getCustomConstraints() { return customConstraints; }
    public void setCustomConstraints(Set<String> customConstraints) { this.customConstraints = customConstraints == null ? new LinkedHashSet<>() : customConstraints; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
