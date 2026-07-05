package fr.achabitation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import fr.achabitation.domain.util.ConstraintNameUtils;

public record DomainPerson(
        UUID id,
        String name,
        BigDecimal livingRest,
        WeightMode weightMode,
        boolean vegetarian,
        boolean noAlcohol,
        Set<String> customConstraints,
        boolean active,
        List<PresencePeriod> presencePeriods
) {
    public boolean isPresentOn(LocalDate date) {
        if (presencePeriods == null || presencePeriods.isEmpty()) {
            return false;
        }
        return presencePeriods.stream().anyMatch(period -> period.contains(date));
    }

    public boolean usesAverageWeight() {
        return weightMode == WeightMode.AVERAGE;
    }

    public boolean hasCustomConstraint(String constraintName) {
        String key = ConstraintNameUtils.key(constraintName);
        if (key.isBlank() || customConstraints == null || customConstraints.isEmpty()) {
            return false;
        }
        return customConstraints.stream().anyMatch(value -> ConstraintNameUtils.key(value).equals(key));
    }
}
