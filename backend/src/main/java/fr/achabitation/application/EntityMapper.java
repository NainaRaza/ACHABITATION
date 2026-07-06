package fr.achabitation.application;

import fr.achabitation.api.dto.AuditDtos.AuditLogResponse;
import fr.achabitation.api.dto.ExpenseDtos.ExpenseResponse;
import fr.achabitation.api.dto.PersonDtos.PersonResponse;
import fr.achabitation.api.dto.PersonDtos.PresencePeriodResponse;
import fr.achabitation.api.dto.TripDtos.TripResponse;
import fr.achabitation.domain.model.DomainExpense;
import fr.achabitation.domain.model.DomainPerson;
import fr.achabitation.domain.model.ExpenseType;
import fr.achabitation.domain.model.PresencePeriod;
import fr.achabitation.domain.model.WeightMode;
import fr.achabitation.infrastructure.entity.AuditLogEntity;
import fr.achabitation.infrastructure.entity.ExpenseEntity;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EntityMapper {
    public String normalizeName(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal positiveRate(BigDecimal value) {
        return value == null || value.signum() <= 0 ? BigDecimal.ONE : value;
    }

    public Map<String, BigDecimal> moneyMap(Map<String, BigDecimal> input) {
        if (input == null || input.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank()) {
                result.put(key, money(value));
            }
        });
        return result;
    }

    public Set<String> stringSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return java.util.Set.of();
        }
        return new LinkedHashSet<>(input);
    }

    public TripResponse toTripResponse(TripEntity trip) {
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getReferenceCurrency(),
                stringSet(trip.getCustomConstraints()),
                trip.isActive()
        );
    }

    public PersonResponse toPersonResponse(PersonEntity person) {
        return toPersonResponse(person, null);
    }

    public PersonResponse toPersonResponse(PersonEntity person, UserEntity viewer) {
        boolean guest = person.getLinkedUser() == null;
        UUID linkedUserId = guest ? null : person.getLinkedUser().getId();
        boolean ownProfile = viewer != null && linkedUserId != null && linkedUserId.equals(viewer.getId());
        boolean hidden = !guest && !ownProfile && !person.isLivingRestPublic();
        BigDecimal visibleLivingRest = hidden ? null : money(person.getLivingRest());
        return new PersonResponse(
                person.getId(),
                person.getName(),
                linkedUserId,
                guest ? null : person.getLinkedUser().getEmail(),
                guest,
                visibleLivingRest,
                hidden,
                person.isLivingRestPublic(),
                guest || ownProfile,
                hidden ? null : person.getWeightMode(),
                !hidden && person.isAdvancedLivingRest(),
                hidden ? null : money(person.getNetIncomeAfterTax()),
                hidden ? null : money(person.getRent()),
                hidden ? null : money(person.getCredits()),
                hidden ? null : money(person.getFixedCharges()),
                hidden ? null : money(person.getTransport()),
                hidden ? null : money(person.getInsurance()),
                hidden ? null : money(person.getOtherMandatoryExpenses()),
                hidden ? null : money(person.getMenstrualProtection()),
                person.isVegetarian(),
                person.isNoAlcohol(),
                stringSet(person.getCustomConstraints()),
                person.isActive(),
                person.getPresencePeriods().stream()
                        .map(p -> new PresencePeriodResponse(p.getStartDate(), p.getEndDate()))
                        .toList()
        );
    }

    public ExpenseResponse toExpenseResponse(ExpenseEntity expense) {
        Set<java.util.UUID> manualParticipantIds = expense.getManualParticipants().stream()
                .map(p -> p.getPerson().getId())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getDate(),
                expense.getPayer().getId(),
                expense.getPayer().getName(),
                money(expense.getTotalAmount()),
                money(expense.getMeatAmount()),
                money(expense.getAlcoholAmount()),
                moneyMap(expense.getCustomConstraintAmounts()),
                expense.getType(),
                expense.isAdvancedMode(),
                manualParticipantIds,
                expense.getCurrency(),
                expense.getExchangeRateToTripCurrency()
        );
    }

    public DomainPerson toDomainPerson(PersonEntity person) {
        return new DomainPerson(
                person.getId(),
                person.getName(),
                money(person.getLivingRest()),
                person.getWeightMode() == null ? WeightMode.LIVING_REST : person.getWeightMode(),
                person.isVegetarian(),
                person.isNoAlcohol(),
                stringSet(person.getCustomConstraints()),
                person.isActive(),
                person.getPresencePeriods().stream()
                        .map(p -> new PresencePeriod(p.getStartDate(), p.getEndDate()))
                        .toList()
        );
    }

    public DomainExpense toDomainExpense(ExpenseEntity expense) {
        return new DomainExpense(
                expense.getId(),
                expense.getTitle(),
                expense.getDate(),
                expense.getPayer().getId(),
                money(expense.getTotalAmount()),
                money(expense.getMeatAmount()),
                money(expense.getAlcoholAmount()),
                moneyMap(expense.getCustomConstraintAmounts()),
                expense.getType() == null ? ExpenseType.NORMAL : expense.getType(),
                expense.isAdvancedMode(),
                expense.getManualParticipants().stream().map(p -> p.getPerson().getId()).collect(Collectors.toSet()),
                expense.getCurrency(),
                positiveRate(expense.getExchangeRateToTripCurrency())
        );
    }

    public AuditLogResponse toAuditResponse(AuditLogEntity log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getActor() == null ? null : log.getActor().getId(),
                log.getCreatedAt()
        );
    }
}
