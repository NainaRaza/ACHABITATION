package fr.achabitation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PresencePeriodTest {
    @Test
    void containsIsInclusiveOnStartAndEndDates() {
        PresencePeriod period = new PresencePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15));

        assertThat(period.contains(LocalDate.of(2026, 8, 1))).isTrue();
        assertThat(period.contains(LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(period.contains(LocalDate.of(2026, 8, 8))).isTrue();
    }

    @Test
    void containsRejectsDatesOutsidePeriod() {
        PresencePeriod period = new PresencePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15));

        assertThat(period.contains(LocalDate.of(2026, 7, 31))).isFalse();
        assertThat(period.contains(LocalDate.of(2026, 8, 16))).isFalse();
    }

    @Test
    void containsReturnsFalseWhenDateOrBoundsAreNull() {
        assertThat(new PresencePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15)).contains(null)).isFalse();
        assertThat(new PresencePeriod(null, LocalDate.of(2026, 8, 15)).contains(LocalDate.of(2026, 8, 5))).isFalse();
        assertThat(new PresencePeriod(LocalDate.of(2026, 8, 1), null).contains(LocalDate.of(2026, 8, 5))).isFalse();
    }
}
