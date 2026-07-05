package fr.achabitation.domain;

import fr.achabitation.domain.model.Balance;
import fr.achabitation.domain.model.DomainExpense;
import fr.achabitation.domain.model.DomainPerson;
import fr.achabitation.domain.model.ExpenseType;
import fr.achabitation.domain.model.Settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class BalanceCalculator {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Map<UUID, BigDecimal> calculateSharesForExpense(DomainExpense expense, List<DomainPerson> persons) {
        Map<UUID, BigDecimal> shares = new LinkedHashMap<>();
        for (DomainPerson person : persons) {
            shares.put(person.id(), ZERO);
        }

        if (expense == null || persons == null || persons.isEmpty()) {
            return shares;
        }

        if (expense.advancedMode()) {
            List<DomainPerson> manualParticipants = persons.stream()
                    .filter(DomainPerson::active)
                    .filter(p -> expense.manualParticipantIds() != null && expense.manualParticipantIds().contains(p.id()))
                    .filter(this::canParticipateInAllocation)
                    .toList();
            addAllocation(shares, amountInTripCurrency(expense.totalAmount(), expense.exchangeRateToTripCurrency()), manualParticipants);
            return shares;
        }

        if (expense.type() == ExpenseType.GLOBAL) {
            List<DomainPerson> globalParticipants = persons.stream()
                    .filter(DomainPerson::active)
                    .filter(this::canParticipateInAllocation)
                    .toList();
            addAllocation(shares, amountInTripCurrency(expense.totalAmount(), expense.exchangeRateToTripCurrency()), globalParticipants);
            return shares;
        }

        BigDecimal rate = safeRate(expense.exchangeRateToTripCurrency());
        BigDecimal meat = amountInTripCurrency(expense.meatAmount(), rate);
        BigDecimal alcohol = amountInTripCurrency(expense.alcoholAmount(), rate);
        Map<String, BigDecimal> customAmounts = customAmountsInTripCurrency(expense, rate);
        BigDecimal customTotal = customAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = amountInTripCurrency(expense.totalAmount(), rate);
        BigDecimal general = total.subtract(meat).subtract(alcohol).subtract(customTotal);
        if (general.signum() < 0) {
            general = ZERO;
        }

        List<DomainPerson> present = persons.stream()
                .filter(DomainPerson::active)
                .filter(p -> p.isPresentOn(expense.date()))
                .filter(this::canParticipateInAllocation)
                .toList();

        List<DomainPerson> meatParticipants = present.stream()
                .filter(p -> !p.vegetarian())
                .toList();

        List<DomainPerson> alcoholParticipants = present.stream()
                .filter(p -> !p.noAlcohol())
                .toList();

        addAllocation(shares, general, present);
        addAllocation(shares, meat, meatParticipants);
        addAllocation(shares, alcohol, alcoholParticipants);
        customAmounts.forEach((constraintName, amount) -> {
            List<DomainPerson> customParticipants = present.stream()
                    .filter(person -> !person.hasCustomConstraint(constraintName))
                    .toList();
            addAllocation(shares, amount, customParticipants);
        });

        return shares;
    }

    public List<Balance> calculateBalances(List<DomainExpense> expenses, List<DomainPerson> persons) {
        Map<UUID, BigDecimal> paid = new LinkedHashMap<>();
        Map<UUID, BigDecimal> owed = new LinkedHashMap<>();

        for (DomainPerson person : persons) {
            paid.put(person.id(), ZERO);
            owed.put(person.id(), ZERO);
        }

        for (DomainExpense expense : expenses) {
            if (paid.containsKey(expense.payerId())) {
                paid.put(expense.payerId(), paid.get(expense.payerId()).add(amountInTripCurrency(expense.totalAmount(), expense.exchangeRateToTripCurrency())));
            }

            Map<UUID, BigDecimal> shares = calculateSharesForExpense(expense, persons);
            for (Map.Entry<UUID, BigDecimal> entry : shares.entrySet()) {
                owed.put(entry.getKey(), owed.getOrDefault(entry.getKey(), ZERO).add(entry.getValue()));
            }
        }

        List<Balance> balances = new ArrayList<>();
        for (DomainPerson person : persons) {
            BigDecimal totalPaid = scale(paid.getOrDefault(person.id(), ZERO));
            BigDecimal totalOwed = scale(owed.getOrDefault(person.id(), ZERO));
            balances.add(new Balance(person, totalPaid, totalOwed, scale(totalPaid.subtract(totalOwed))));
        }
        return balances;
    }

    public List<Settlement> calculateSettlements(List<Balance> balances) {
        List<MutableAmount> debtors = balances.stream()
                .filter(b -> b.balance().signum() < 0)
                .map(b -> new MutableAmount(b.person(), b.balance().abs()))
                .sorted(Comparator.comparing(MutableAmount::amount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        List<MutableAmount> creditors = balances.stream()
                .filter(b -> b.balance().signum() > 0)
                .map(b -> new MutableAmount(b.person(), b.balance()))
                .sorted(Comparator.comparing(MutableAmount::amount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        List<Settlement> settlements = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;

        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            MutableAmount debtor = debtors.get(debtorIndex);
            MutableAmount creditor = creditors.get(creditorIndex);
            BigDecimal amount = debtor.amount.min(creditor.amount).setScale(2, RoundingMode.HALF_UP);

            if (amount.signum() > 0) {
                settlements.add(new Settlement(debtor.person, creditor.person, amount));
            }

            debtor.amount = debtor.amount.subtract(amount).setScale(2, RoundingMode.HALF_UP);
            creditor.amount = creditor.amount.subtract(amount).setScale(2, RoundingMode.HALF_UP);

            if (debtor.amount.compareTo(new BigDecimal("0.01")) < 0) {
                debtorIndex++;
            }
            if (creditor.amount.compareTo(new BigDecimal("0.01")) < 0) {
                creditorIndex++;
            }
        }

        return settlements;
    }

    public void validateExpenseHasParticipants(DomainExpense expense, List<DomainPerson> persons) {
        if (expense == null) {
            throw new IllegalArgumentException("La dépense est invalide.");
        }

        List<DomainPerson> eligibleActivePersons = persons == null
                ? List.of()
                : persons.stream()
                .filter(DomainPerson::active)
                .filter(this::canParticipateInAllocation)
                .toList();

        if (expense.advancedMode()) {
            List<DomainPerson> manualParticipants = eligibleActivePersons.stream()
                    .filter(p -> expense.manualParticipantIds() != null && expense.manualParticipantIds().contains(p.id()))
                    .toList();
            if (manualParticipants.isEmpty()) {
                throw new IllegalArgumentException("Dépense impossible : aucune personne active avec un RAV positif ou en mode moyenne n'est sélectionnée en mode avancé.");
            }
            return;
        }

        if (expense.type() == ExpenseType.GLOBAL) {
            if (eligibleActivePersons.isEmpty()) {
                throw new IllegalArgumentException("Dépense globale impossible : aucune personne active avec un RAV positif ou en mode moyenne ne peut participer à la répartition.");
            }
            return;
        }

        BigDecimal meat = safe(expense.meatAmount());
        BigDecimal alcohol = safe(expense.alcoholAmount());
        Map<String, BigDecimal> customAmounts = safeCustomAmounts(expense);
        BigDecimal customTotal = customAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = safe(expense.totalAmount());
        if (meat.add(alcohol).add(customTotal).compareTo(total) > 0) {
            throw new IllegalArgumentException("Dépense impossible : la somme viande + alcool + contraintes personnalisées dépasse le total.");
        }
        BigDecimal general = total.subtract(meat).subtract(alcohol).subtract(customTotal);
        if (general.signum() < 0) {
            general = ZERO;
        }

        List<DomainPerson> present = eligibleActivePersons.stream()
                .filter(p -> p.isPresentOn(expense.date()))
                .toList();
        List<DomainPerson> meatParticipants = present.stream().filter(p -> !p.vegetarian()).toList();
        List<DomainPerson> alcoholParticipants = present.stream().filter(p -> !p.noAlcohol()).toList();

        List<String> blockingReasons = new ArrayList<>();
        if (general.signum() > 0 && present.isEmpty()) {
            blockingReasons.add("la part générale ne concerne personne à la date indiquée");
        }
        if (meat.signum() > 0 && meatParticipants.isEmpty()) {
            blockingReasons.add("la part viande est positive mais aucune personne concernée ne peut la payer");
        }
        if (alcohol.signum() > 0 && alcoholParticipants.isEmpty()) {
            blockingReasons.add("la part alcool est positive mais aucune personne concernée ne peut la payer");
        }
        customAmounts.forEach((constraintName, amount) -> {
            if (amount.signum() > 0) {
                List<DomainPerson> customParticipants = present.stream()
                        .filter(person -> !person.hasCustomConstraint(constraintName))
                        .toList();
                if (customParticipants.isEmpty()) {
                    blockingReasons.add("la part " + constraintName + " est positive mais aucune personne concernée ne peut la payer");
                }
            }
        });

        if (!blockingReasons.isEmpty()) {
            throw new IllegalArgumentException("Dépense impossible : " + String.join(" ; ", blockingReasons) + ".");
        }
    }


    private Map<String, BigDecimal> customAmountsInTripCurrency(DomainExpense expense, BigDecimal rate) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        safeCustomAmounts(expense).forEach((constraintName, amount) -> {
            BigDecimal converted = amountInTripCurrency(amount, rate);
            if (converted.signum() > 0) {
                result.put(constraintName, converted);
            }
        });
        return result;
    }

    private Map<String, BigDecimal> safeCustomAmounts(DomainExpense expense) {
        if (expense == null || expense.customConstraintAmounts() == null || expense.customConstraintAmounts().isEmpty()) {
            return Map.of();
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        expense.customConstraintAmounts().forEach((constraintName, amount) -> {
            if (constraintName != null && !constraintName.isBlank()) {
                BigDecimal safeAmount = safe(amount);
                if (safeAmount.signum() > 0) {
                    result.put(constraintName, safeAmount);
                }
            }
        });
        return result;
    }

    private void addAllocation(Map<UUID, BigDecimal> shares, BigDecimal amount, List<DomainPerson> participants) {
        Map<UUID, BigDecimal> allocation = allocateProportionally(amount, participants);
        for (Map.Entry<UUID, BigDecimal> entry : allocation.entrySet()) {
            shares.put(entry.getKey(), shares.getOrDefault(entry.getKey(), ZERO).add(entry.getValue()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private Map<UUID, BigDecimal> allocateProportionally(BigDecimal amount, List<DomainPerson> participants) {
        Map<UUID, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal scaledAmount = safe(amount);
        if (scaledAmount.signum() <= 0 || participants == null || participants.isEmpty()) {
            return result;
        }

        List<DomainPerson> eligible = participants.stream().filter(this::canParticipateInAllocation).toList();
        if (eligible.isEmpty()) {
            return result;
        }

        Map<UUID, BigDecimal> weights = calculateEffectiveWeights(eligible);
        BigDecimal totalWeight = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() <= 0) {
            return result;
        }

        long cents = scaledAmount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        List<AllocationLine> lines = new ArrayList<>();
        long allocated = 0L;

        for (DomainPerson person : eligible) {
            BigDecimal weight = weights.getOrDefault(person.id(), BigDecimal.ZERO);
            if (weight.signum() <= 0) {
                continue;
            }
            BigDecimal exactCents = weight.multiply(BigDecimal.valueOf(cents)).divide(totalWeight, 20, RoundingMode.HALF_UP);
            long floor = exactCents.setScale(0, RoundingMode.DOWN).longValue();
            BigDecimal remainder = exactCents.subtract(BigDecimal.valueOf(floor));
            lines.add(new AllocationLine(person.id(), floor, remainder));
            allocated += floor;
        }

        long remaining = cents - allocated;
        lines.sort(Comparator.comparing(AllocationLine::remainder).reversed());
        for (int i = 0; i < remaining; i++) {
            lines.get(i % lines.size()).cents += 1;
        }

        lines.sort(Comparator.comparing(line -> line.personId));
        for (AllocationLine line : lines) {
            result.put(line.personId, BigDecimal.valueOf(line.cents).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private Map<UUID, BigDecimal> calculateEffectiveWeights(List<DomainPerson> eligible) {
        Map<UUID, BigDecimal> weights = new LinkedHashMap<>();
        List<DomainPerson> ravBasedPersons = eligible.stream()
                .filter(p -> !p.usesAverageWeight())
                .filter(this::hasPositiveLivingRest)
                .toList();

        if (ravBasedPersons.isEmpty()) {
            for (DomainPerson person : eligible) {
                weights.put(person.id(), BigDecimal.ONE);
            }
            return weights;
        }

        BigDecimal referenceTotal = ravBasedPersons.stream().map(DomainPerson::livingRest).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageWeight = referenceTotal.divide(BigDecimal.valueOf(ravBasedPersons.size()), 10, RoundingMode.HALF_UP);

        for (DomainPerson person : eligible) {
            if (person.usesAverageWeight()) {
                weights.put(person.id(), averageWeight);
            } else {
                weights.put(person.id(), person.livingRest());
            }
        }
        return weights;
    }

    private boolean canParticipateInAllocation(DomainPerson person) {
        return person != null && (person.usesAverageWeight() || hasPositiveLivingRest(person));
    }

    private boolean hasPositiveLivingRest(DomainPerson person) {
        return person != null && person.livingRest() != null && person.livingRest().signum() > 0;
    }

    private BigDecimal amountInTripCurrency(BigDecimal amount, BigDecimal exchangeRateToTripCurrency) {
        return safe(amount).multiply(safeRate(exchangeRateToTripCurrency)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeRate(BigDecimal rate) {
        return rate == null || rate.signum() <= 0 ? BigDecimal.ONE : rate;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value);
    }

    private static final class AllocationLine {
        private final UUID personId;
        private long cents;
        private final BigDecimal remainder;

        private AllocationLine(UUID personId, long cents, BigDecimal remainder) {
            this.personId = personId;
            this.cents = cents;
            this.remainder = remainder;
        }

        private BigDecimal remainder() {
            return remainder;
        }
    }

    private static final class MutableAmount {
        private final DomainPerson person;
        private BigDecimal amount;

        private MutableAmount(DomainPerson person, BigDecimal amount) {
            this.person = person;
            this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal amount() {
            return amount;
        }
    }
}
