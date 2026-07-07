package fr.achabitation.domain;

import fr.achabitation.domain.model.Balance;
import fr.achabitation.domain.model.DomainExpense;
import fr.achabitation.domain.model.DomainPerson;
import fr.achabitation.domain.model.ExpenseType;
import fr.achabitation.domain.model.PresencePeriod;
import fr.achabitation.domain.model.Settlement;
import fr.achabitation.domain.model.WeightMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceCalculatorTest {
    private static final LocalDate AUG_02 = LocalDate.of(2026, 8, 2);
    private static final LocalDate AUG_03 = LocalDate.of(2026, 8, 3);
    private static final LocalDate AUG_10 = LocalDate.of(2026, 8, 10);

    private final BalanceCalculator calculator = new BalanceCalculator();

    private final UUID sofiaId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID karimId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID linaId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID inactiveId = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void normalExpenseSplitsGeneralMeatAndAlcoholWithPresenceAndDietRules() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "2000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(karimId, "Karim", "1000", WeightMode.LIVING_REST, true, false, true, fullStay()),
                person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, true, true, fullStay())
        );
        DomainExpense expense = expense("Courses", AUG_02, sofiaId, "120", "30", "20", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("68.33");
        assertThat(shares.get(karimId)).isEqualByComparingTo("24.17");
        assertThat(shares.get(linaId)).isEqualByComparingTo("27.50");
        assertThat(sum(shares)).isEqualByComparingTo("120.00");
    }

    @Test
    void averageModeUsesAverageWeightOfRavBasedParticipantsForEachBlock() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "2000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(karimId, "Karim", "0", WeightMode.AVERAGE, false, false, true, fullStay()),
                person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, false, true, fullStay())
        );
        DomainExpense expense = expense("Activité", AUG_02, sofiaId, "450", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("200.00");
        assertThat(shares.get(karimId)).isEqualByComparingTo("150.00");
        assertThat(shares.get(linaId)).isEqualByComparingTo("100.00");
        assertThat(sum(shares)).isEqualByComparingTo("450.00");
    }

    @Test
    void whenAllParticipantsUseAverageModeExpenseIsSplitEqually() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "0", WeightMode.AVERAGE, false, false, true, fullStay()),
                person(karimId, "Karim", "0", WeightMode.AVERAGE, false, false, true, fullStay()),
                person(linaId, "Lina", "0", WeightMode.AVERAGE, false, false, true, fullStay())
        );
        DomainExpense expense = expense("Logement", AUG_02, sofiaId, "99.99", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("33.33");
        assertThat(shares.get(karimId)).isEqualByComparingTo("33.33");
        assertThat(shares.get(linaId)).isEqualByComparingTo("33.33");
        assertThat(sum(shares)).isEqualByComparingTo("99.99");
    }

    @Test
    void globalExpenseIgnoresPresenceButKeepsRavWeightingWhenNoSpecificAmountsAreSet() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "2000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(karimId, "Karim", "1000", WeightMode.LIVING_REST, true, false, true, List.of(period(AUG_10, AUG_10))),
                person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, true, true, List.of(period(AUG_10, AUG_10)))
        );
        DomainExpense expense = expense("Essence mutualisée", AUG_03, karimId, "400", "0", "0", ExpenseType.GLOBAL, false, Set.of(), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("200.00");
        assertThat(shares.get(karimId)).isEqualByComparingTo("100.00");
        assertThat(shares.get(linaId)).isEqualByComparingTo("100.00");
        assertThat(sum(shares)).isEqualByComparingTo("400.00");
    }

    @Test
    void globalExpenseIgnoresPresenceButAppliesMeatAlcoholAndCustomConstraints() {
        DomainPerson sofia = new DomainPerson(sofiaId, "Sofia", new BigDecimal("1000"), WeightMode.LIVING_REST, false, false, Set.of(), true, fullStay());
        DomainPerson karim = new DomainPerson(karimId, "Karim", new BigDecimal("1000"), WeightMode.LIVING_REST, true, false, Set.of("Sans porc"), true, List.of(period(AUG_10, AUG_10)));
        DomainPerson lina = new DomainPerson(linaId, "Lina", new BigDecimal("1000"), WeightMode.LIVING_REST, false, true, Set.of(), true, List.of(period(AUG_10, AUG_10)));
        DomainExpense expense = new DomainExpense(
                UUID.randomUUID(),
                "Courses pour tout le séjour",
                AUG_03,
                sofiaId,
                new BigDecimal("120"),
                new BigDecimal("30"),
                new BigDecimal("20"),
                Map.of("Sans porc", new BigDecimal("30")),
                ExpenseType.GLOBAL,
                false,
                Set.of(),
                "EUR",
                BigDecimal.ONE
        );

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, List.of(sofia, karim, lina));

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("53.34");
        assertThat(shares.get(karimId)).isEqualByComparingTo("23.33");
        assertThat(shares.get(linaId)).isEqualByComparingTo("43.33");
        assertThat(sum(shares)).isEqualByComparingTo("120.00");
    }

    @Test
    void advancedExpenseUsesOnlyManualParticipants() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "3000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(karimId, "Karim", "1000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, false, true, fullStay())
        );
        DomainExpense expense = expense("Taxi", AUG_02, sofiaId, "80", "0", "0", ExpenseType.NORMAL, true, Set.of(karimId, linaId), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("0.00");
        assertThat(shares.get(karimId)).isEqualByComparingTo("40.00");
        assertThat(shares.get(linaId)).isEqualByComparingTo("40.00");
        assertThat(sum(shares)).isEqualByComparingTo("80.00");
    }

    @Test
    void expenseAmountsAreConvertedToTripCurrencyBeforeAllocation() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "1000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(karimId, "Karim", "1000", WeightMode.LIVING_REST, false, false, true, fullStay())
        );
        DomainExpense expense = expense("Restaurant Londres", AUG_02, sofiaId, "100", "0", "0", ExpenseType.NORMAL, false, Set.of(), "GBP", "1.20");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("60.00");
        assertThat(shares.get(karimId)).isEqualByComparingTo("60.00");
        assertThat(sum(shares)).isEqualByComparingTo("120.00");
    }

    @Test
    void roundingDistributesRemainingCentsSoSharesAlwaysEqualExpenseTotal() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "1", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(karimId, "Karim", "1", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(linaId, "Lina", "1", WeightMode.LIVING_REST, false, false, true, fullStay())
        );
        DomainExpense expense = expense("Arrondi", AUG_02, sofiaId, "10", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(sum(shares)).isEqualByComparingTo("10.00");
        assertThat(shares.values()).allSatisfy(value -> assertThat(value).isBetween(new BigDecimal("3.33"), new BigDecimal("3.34")));
    }

    @Test
    void inactivePersonsAreKeptInOutputButNeverReceiveShares() {
        List<DomainPerson> persons = List.of(
                person(sofiaId, "Sofia", "1000", WeightMode.LIVING_REST, false, false, true, fullStay()),
                person(inactiveId, "Ancien", "1000", WeightMode.LIVING_REST, false, false, false, fullStay())
        );
        DomainExpense expense = expense("Courses", AUG_02, sofiaId, "100", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, persons);

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("100.00");
        assertThat(shares.get(inactiveId)).isEqualByComparingTo("0.00");
        assertThat(sum(shares)).isEqualByComparingTo("100.00");
    }

    @Test
    void balancesUseAmountPaidMinusAmountOwed() {
        DomainPerson sofia = person(sofiaId, "Sofia", "2000", WeightMode.LIVING_REST, false, false, true, fullStay());
        DomainPerson karim = person(karimId, "Karim", "1000", WeightMode.LIVING_REST, false, false, true, fullStay());
        List<DomainPerson> persons = List.of(sofia, karim);
        List<DomainExpense> expenses = List.of(
                expense("Courses", AUG_02, sofiaId, "300", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1"),
                expense("Taxi", AUG_02, karimId, "60", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1")
        );

        List<Balance> balances = calculator.calculateBalances(expenses, persons);

        Balance sofiaBalance = findBalance(balances, sofiaId);
        Balance karimBalance = findBalance(balances, karimId);
        assertThat(sofiaBalance.totalPaid()).isEqualByComparingTo("300.00");
        assertThat(sofiaBalance.totalOwed()).isEqualByComparingTo("240.00");
        assertThat(sofiaBalance.balance()).isEqualByComparingTo("60.00");
        assertThat(karimBalance.totalPaid()).isEqualByComparingTo("60.00");
        assertThat(karimBalance.totalOwed()).isEqualByComparingTo("120.00");
        assertThat(karimBalance.balance()).isEqualByComparingTo("-60.00");
    }

    @Test
    void settlementsMatchDebtorsToCreditors() {
        DomainPerson sofia = person(sofiaId, "Sofia", "1000", WeightMode.LIVING_REST, false, false, true, fullStay());
        DomainPerson karim = person(karimId, "Karim", "1000", WeightMode.LIVING_REST, false, false, true, fullStay());
        DomainPerson lina = person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, false, true, fullStay());
        List<Balance> balances = List.of(
                new Balance(sofia, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00")),
                new Balance(karim, new BigDecimal("0.00"), new BigDecimal("70.00"), new BigDecimal("-70.00")),
                new Balance(lina, new BigDecimal("0.00"), new BigDecimal("20.00"), new BigDecimal("-20.00"))
        );

        List<Settlement> settlements = calculator.calculateSettlements(balances);

        assertThat(settlements).hasSize(2);
        assertThat(settlements.get(0).from().id()).isEqualTo(karimId);
        assertThat(settlements.get(0).to().id()).isEqualTo(sofiaId);
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("70.00");
        assertThat(settlements.get(1).from().id()).isEqualTo(linaId);
        assertThat(settlements.get(1).to().id()).isEqualTo(sofiaId);
        assertThat(settlements.get(1).amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void validationBlocksNormalExpenseWhenNoOneIsPresentForGeneralPart() {
        List<DomainPerson> persons = List.of(person(sofiaId, "Sofia", "1000", WeightMode.LIVING_REST, false, false, true, List.of(period(AUG_10, AUG_10))));
        DomainExpense expense = expense("Hors séjour", AUG_02, sofiaId, "50", "0", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("part générale ne concerne personne");
    }

    @Test
    void validationBlocksMeatAmountWhenNoEligibleMeatParticipantExists() {
        List<DomainPerson> persons = List.of(person(karimId, "Karim", "1000", WeightMode.LIVING_REST, true, false, true, fullStay()));
        DomainExpense expense = expense("Barbecue", AUG_02, karimId, "30", "30", "0", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("part viande est positive");
    }

    @Test
    void validationBlocksAlcoholAmountWhenNoEligibleAlcoholParticipantExists() {
        List<DomainPerson> persons = List.of(person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, true, true, fullStay()));
        DomainExpense expense = expense("Vin", AUG_02, linaId, "20", "0", "20", ExpenseType.NORMAL, false, Set.of(), "EUR", "1");

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("part alcool est positive");
    }

    @Test
    void validationBlocksAdvancedExpenseWithoutManualParticipants() {
        List<DomainPerson> persons = List.of(person(sofiaId, "Sofia", "1000", WeightMode.LIVING_REST, false, false, true, fullStay()));
        DomainExpense expense = expense("Taxi", AUG_02, sofiaId, "20", "0", "0", ExpenseType.NORMAL, true, Set.of(), "EUR", "1");

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mode avancé");
    }

    @Test
    void validationBlocksGlobalExpenseWithoutAnyEligibleActivePerson() {
        List<DomainPerson> persons = List.of(person(sofiaId, "Sofia", "0", WeightMode.LIVING_REST, false, false, true, fullStay()));
        DomainExpense expense = expense("Essence", AUG_02, sofiaId, "20", "0", "0", ExpenseType.GLOBAL, false, Set.of(), "EUR", "1");

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dépense globale impossible");
    }

    @Test
    void validationBlocksGlobalAlcoholAmountWhenNoEligibleAlcoholParticipantExistsEvenWithoutPresenceDate() {
        List<DomainPerson> persons = List.of(
                person(linaId, "Lina", "1000", WeightMode.LIVING_REST, false, true, true, List.of(period(AUG_10, AUG_10)))
        );
        DomainExpense expense = expense("Alcool pour le séjour", AUG_02, linaId, "20", "0", "20", ExpenseType.GLOBAL, false, Set.of(), "EUR", "1");

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dépense globale impossible")
                .hasMessageContaining("part alcool est positive");
    }


    @Test
    void customConstraintAmountExcludesPersonsWhoHaveThatConstraint() {
        DomainPerson sofia = new DomainPerson(sofiaId, "Sofia", new BigDecimal("1000"), WeightMode.LIVING_REST, false, false, Set.of("Sans porc"), true, fullStay());
        DomainPerson karim = new DomainPerson(karimId, "Karim", new BigDecimal("1000"), WeightMode.LIVING_REST, false, false, Set.of(), true, fullStay());
        DomainPerson lina = new DomainPerson(linaId, "Lina", new BigDecimal("1000"), WeightMode.LIVING_REST, false, false, Set.of(), true, fullStay());
        DomainExpense expense = new DomainExpense(UUID.randomUUID(), "Courses", AUG_02, sofiaId, new BigDecimal("90"), BigDecimal.ZERO, BigDecimal.ZERO, Map.of("Sans porc", new BigDecimal("30")), ExpenseType.NORMAL, false, Set.of(), "EUR", BigDecimal.ONE);

        Map<UUID, BigDecimal> shares = calculator.calculateSharesForExpense(expense, List.of(sofia, karim, lina));

        assertThat(shares.get(sofiaId)).isEqualByComparingTo("20.00");
        assertThat(shares.get(karimId)).isEqualByComparingTo("35.00");
        assertThat(shares.get(linaId)).isEqualByComparingTo("35.00");
        assertThat(sum(shares)).isEqualByComparingTo("90.00");
    }

    @Test
    void validationBlocksCustomConstraintAmountWhenNoEligibleParticipantExists() {
        List<DomainPerson> persons = List.of(
                new DomainPerson(sofiaId, "Sofia", new BigDecimal("1000"), WeightMode.LIVING_REST, false, false, Set.of("Sans porc"), true, fullStay()),
                new DomainPerson(karimId, "Karim", new BigDecimal("1000"), WeightMode.LIVING_REST, false, false, Set.of("sans porc"), true, fullStay())
        );
        DomainExpense expense = new DomainExpense(UUID.randomUUID(), "Courses", AUG_02, sofiaId, new BigDecimal("30"), BigDecimal.ZERO, BigDecimal.ZERO, Map.of("Sans porc", new BigDecimal("30")), ExpenseType.NORMAL, false, Set.of(), "EUR", BigDecimal.ONE);

        assertThatThrownBy(() -> calculator.validateExpenseHasParticipants(expense, persons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("part Sans porc est positive");
    }

    private DomainPerson person(UUID id, String name, String livingRest, WeightMode weightMode, boolean vegetarian, boolean noAlcohol, boolean active, List<PresencePeriod> periods) {
        return new DomainPerson(id, name, new BigDecimal(livingRest), weightMode, vegetarian, noAlcohol, Set.of(), active, periods);
    }

    private DomainExpense expense(String title, LocalDate date, UUID payerId, String total, String meat, String alcohol, ExpenseType type, boolean advancedMode, Set<UUID> manualParticipantIds, String currency, String exchangeRate) {
        return new DomainExpense(UUID.randomUUID(), title, date, payerId, new BigDecimal(total), new BigDecimal(meat), new BigDecimal(alcohol), Map.of(), type, advancedMode, manualParticipantIds, currency, new BigDecimal(exchangeRate));
    }

    private PresencePeriod period(LocalDate start, LocalDate end) {
        return new PresencePeriod(start, end);
    }

    private List<PresencePeriod> fullStay() {
        return List.of(period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15)));
    }

    private BigDecimal sum(Map<UUID, BigDecimal> shares) {
        return shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Balance findBalance(List<Balance> balances, UUID personId) {
        return balances.stream()
                .filter(balance -> balance.person().id().equals(personId))
                .findFirst()
                .orElseThrow();
    }
}
