package fr.achabitation.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {
    private final EntityMapper mapper = new EntityMapper();

    @Test
    void normalizeNameIgnoresCaseAccentsAndRepeatedSpaces() {
        assertThat(mapper.normalizeName("  Élodie   Dupré  ")).isEqualTo("elodie dupre");
        assertThat(mapper.normalizeName("elodie dupre")).isEqualTo("elodie dupre");
        assertThat(mapper.normalizeName("ÉLODIE DUPRÉ")).isEqualTo("elodie dupre");
    }

    @Test
    void normalizeNameReturnsEmptyStringForNull() {
        assertThat(mapper.normalizeName(null)).isEmpty();
    }

    @Test
    void moneyScalesToTwoDecimalsAndUsesZeroForNull() {
        assertThat(mapper.money(null)).isEqualByComparingTo("0.00");
        assertThat(mapper.money(new BigDecimal("12.345"))).isEqualByComparingTo("12.35");
        assertThat(mapper.money(new BigDecimal("12.344"))).isEqualByComparingTo("12.34");
    }

    @Test
    void positiveRateFallsBackToOneWhenNullZeroOrNegative() {
        assertThat(mapper.positiveRate(null)).isEqualByComparingTo("1");
        assertThat(mapper.positiveRate(BigDecimal.ZERO)).isEqualByComparingTo("1");
        assertThat(mapper.positiveRate(new BigDecimal("-2"))).isEqualByComparingTo("1");
        assertThat(mapper.positiveRate(new BigDecimal("1.17"))).isEqualByComparingTo("1.17");
    }
}
