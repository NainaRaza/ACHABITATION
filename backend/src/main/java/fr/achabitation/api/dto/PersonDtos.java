package fr.achabitation.api.dto;

import fr.achabitation.domain.model.WeightMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PersonDtos {
    private PersonDtos() {}

    public record PresencePeriodRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record PresencePeriodResponse(
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record PersonCreateRequest(
            @NotBlank @Size(max = 120) String name,
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
            Set<String> customConstraints,
            @Valid @NotEmpty List<PresencePeriodRequest> presencePeriods
    ) {}

    public record PersonUpdateRequest(
            @NotBlank @Size(max = 120) String name,
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
            Set<String> customConstraints,
            boolean active,
            @Valid @NotEmpty List<PresencePeriodRequest> presencePeriods
    ) {}

    public record LinkGuestRequest(
            UUID userId,
            boolean applyProfileToGuest
    ) {}

    public record PersonResponse(
            UUID id,
            String name,
            UUID linkedUserId,
            String linkedUserEmail,
            boolean guest,
            BigDecimal livingRest,
            boolean livingRestHidden,
            boolean livingRestPublic,
            boolean canEditFinancialProfile,
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
            Set<String> customConstraints,
            boolean active,
            List<PresencePeriodResponse> presencePeriods
    ) {}
}
