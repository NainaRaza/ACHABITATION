package fr.achabitation.api.dto;

import fr.achabitation.domain.model.WeightMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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

    public record AuthResponse(
            UUID userId,
            String email,
            String displayName,
            String devToken,
            String note
    ) {}

    public record UserProfileRequest(
            String displayName,
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
            Set<String> customConstraints
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
}
