package fr.achabitation.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_invitation")
public class TripInvitationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private TripEntity trip;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_user_id")
    private UserEntity createdBy;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripRole roleToGrant = TripRole.PARTICIPANT;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TripEntity getTrip() { return trip; }
    public void setTrip(TripEntity trip) { this.trip = trip; }
    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public TripRole getRoleToGrant() { return roleToGrant; }
    public void setRoleToGrant(TripRole roleToGrant) { this.roleToGrant = roleToGrant; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}
