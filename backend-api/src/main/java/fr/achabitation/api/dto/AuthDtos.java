package fr.achabitation.api.dto;

import fr.achabitation.domain.model.WeightMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 2, max = 120) String displayName,
            @NotBlank @Size(min = 8, max = 120) String password
    ) {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AccountUpdateRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 2, max = 120) String displayName
    ) {}

    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 120) String newPassword
    ) {}

    public record PasswordResetRequest(
            @Email @NotBlank String email
    ) {}

    public record EmailVerificationRequest(
            @Email @NotBlank String email
    ) {}

    public record EmailVerificationConfirmRequest(
            @NotBlank String token
    ) {}

    public record PasswordResetConfirmRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 120) String newPassword
    ) {}

    public record OperationResponse(
            String note
    ) {}

    public record AuthResponse(
            UUID userId,
            String email,
            String displayName,
            boolean emailVerified,
            String accessToken,
            String note
    ) {}

    public record UserProfileRequest(
            @Size(max = 120) String displayName,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal livingRest,
            WeightMode weightMode,
            boolean advancedLivingRest,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal netIncomeAfterTax,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal rent,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal credits,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal fixedCharges,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal transport,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal insurance,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal otherMandatoryExpenses,
            @PositiveOrZero @DecimalMax("999999999.99") BigDecimal menstrualProtection,
            boolean vegetarian,
            boolean noAlcohol,
            boolean livingRestPublic,
            @Size(max = 50) Set<@NotBlank @Size(max = 120) String> customConstraints
    ) {}

    public record ApplyProfileToLinkedPersonsRequest(
            Set<UUID> personIds
    ) {}

    public record LinkedProfilePersonResponse(
            UUID personId,
            String personName,
            UUID tripId,
            String tripName
    ) {}

    public record UserProfileResponse(
            UUID userId,
            String email,
            String displayName,
            boolean emailVerified,
            BigDecimal livingRest,
            WeightMode weightMode,
            boolean advancedLivingRest,
            BigDecimal netIncomeAfterTax,
            BigDecimal rent,
            BigDecimal credits,
            BigDecimal fixedCharges,
            BigDecimal transport,
            BigDecimal insurance,
            BigDecimal otherMandatoryExpenses,
            BigDecimal menstrualProtection,
            boolean vegetarian,
            boolean noAlcohol,
            boolean livingRestPublic,
            Set<String> knownCustomConstraints,
            Set<String> customConstraints,
            List<LinkedProfilePersonResponse> linkedPersons
    ) {}

    public record AccountExportTrip(
            UUID tripId,
            String tripName,
            String role,
            Instant joinedAt
    ) {}

    public record AccountExportLinkedPerson(
            UUID personId,
            String personName,
            UUID tripId,
            String tripName,
            boolean livingRestPublic
    ) {}

    public record AccountExportInvitation(
            UUID invitationId,
            UUID tripId,
            String tripName,
            String roleToGrant,
            Instant createdAt,
            Instant expiresAt,
            boolean revoked
    ) {}

    public record AccountExportAuditLog(
            UUID auditLogId,
            UUID tripId,
            String tripName,
            String action,
            String entityType,
            UUID entityId,
            Instant createdAt
    ) {}

    public record AccountExportExpense(
            UUID expenseId,
            UUID tripId,
            String tripName,
            String title,
            BigDecimal totalAmount,
            String currency
    ) {}

    public record AccountExportResponse(
            UUID userId,
            String email,
            String displayName,
            UserProfileResponse profile,
            List<AccountExportTrip> trips,
            List<AccountExportLinkedPerson> linkedPersons,
            List<AccountExportInvitation> createdInvitations,
            List<AccountExportAuditLog> auditLogs,
            List<AccountExportExpense> paidExpenses,
            Instant exportedAt
    ) {}

    public record UserSessionResponse(
            UUID sessionId,
            String deviceLabel,
            Instant createdAt,
            Instant lastUsedAt,
            Instant expiresAt,
            boolean current
    ) {}
}
