package fr.achabitation.domain.model;

import java.time.LocalDate;

public record PresencePeriod(LocalDate startDate, LocalDate endDate) {
    public boolean contains(LocalDate date) {
        return date != null
                && startDate != null
                && endDate != null
                && !date.isBefore(startDate)
                && !date.isAfter(endDate);
    }
}
