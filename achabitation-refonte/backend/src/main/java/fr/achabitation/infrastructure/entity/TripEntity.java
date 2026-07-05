package fr.achabitation.infrastructure.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "trip")
public class TripEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false, length = 3)
    private String referenceCurrency = "EUR";

    @ElementCollection
    @CollectionTable(name = "trip_custom_constraint", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "constraint_name", nullable = false, length = 120)
    private Set<String> customConstraints = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getReferenceCurrency() { return referenceCurrency; }
    public void setReferenceCurrency(String referenceCurrency) { this.referenceCurrency = referenceCurrency; }
    public Set<String> getCustomConstraints() { return customConstraints; }
    public void setCustomConstraints(Set<String> customConstraints) { this.customConstraints = customConstraints == null ? new LinkedHashSet<>() : customConstraints; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
