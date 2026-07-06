package fr.achabitation.api.dto;

import fr.achabitation.infrastructure.entity.TripRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public final class TripDtos {
    private TripDtos() {}

    public record TripCreateRequest(
            @NotBlank @Size(max = 160) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Size(min = 3, max = 3) String referenceCurrency,
            UUID ownerUserId,
            Set<String> customConstraints
    ) {}

    public record TripConstraintUpdateRequest(
            Set<String> customConstraints
    ) {}

    public record JoinTripRequest(
            UUID guestPersonId,
            boolean applyProfileToGuest,
            String invitationCode
    ) {}

    public record JoinTripByCodeRequest(
            UUID guestPersonId,
            boolean applyProfileToGuest,
            String invitationCode
    ) {}

    public record TripInvitationCreateRequest(
            TripRole roleToGrant,
            @Min(1) @Max(30) Integer expiresInDays
    ) {}

    public record TripInvitationResponse(
            UUID id,
            UUID tripId,
            String code,
            TripRole roleToGrant,
            Instant createdAt,
            Instant expiresAt,
            boolean revoked,
            boolean usable
    ) {}

    public record TripResponse(
            UUID id,
            String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String referenceCurrency,
            Set<String> customConstraints,
            boolean active
    ) {}
}
