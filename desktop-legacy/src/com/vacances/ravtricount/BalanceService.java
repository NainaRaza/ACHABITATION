package com.vacances.ravtricount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BalanceService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Map<String, BigDecimal> calculateSharesForExpense(Expense expense, List<Person> persons) {
        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        for (Person person : persons) {
            shares.put(person.getId(), ZERO);
        }

        if (expense == null || persons == null || persons.isEmpty()) {
            return shares;
        }

        if (expense.isAdvancedMode()) {
            List<Person> manualParticipants = persons.stream()
                    .filter(Person::isActive)
                    .filter(p -> expense.getManualParticipantIds().contains(p.getId()))
                    .filter(this::canParticipateInAllocation)
                    .toList();
            addAllocation(shares, expense.getTotalAmount(), manualParticipants);
            return shares;
        }

        if (expense.getType() == ExpenseType.GLOBAL) {
            List<Person> globalParticipants = persons.stream()
                    .filter(Person::isActive)
                    .filter(this::canParticipateInAllocation)
                    .toList();
            addAllocation(shares, expense.getTotalAmount(), globalParticipants);
            return shares;
        }

        BigDecimal meat = safe(expense.getMeatAmount());
        BigDecimal alcohol = safe(expense.getAlcoholAmount());
        BigDecimal total = safe(expense.getTotalAmount());
        BigDecimal general = total.subtract(meat).subtract(alcohol);
        if (general.signum() < 0) {
            general = ZERO;
        }

        List<Person> present = persons.stream()
                .filter(Person::isActive)
                .filter(p -> p.isPresentOn(expense.getDate()))
                .filter(this::canParticipateInAllocation)
                .toList();

        List<Person> meatParticipants = present.stream()
                .filter(p -> !p.isVegetarian())
                .toList();

        List<Person> alcoholParticipants = present.stream()
                .filter(p -> !p.isNoAlcohol())
                .toList();

        addAllocation(shares, general, present);
        addAllocation(shares, meat, meatParticipants);
        addAllocation(shares, alcohol, alcoholParticipants);

        return shares;
    }

    public List<Balance> calculateBalances(List<Expense> expenses, List<Person> persons) {
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        Map<String, BigDecimal> owed = new LinkedHashMap<>();

        for (Person person : persons) {
            paid.put(person.getId(), ZERO);
            owed.put(person.getId(), ZERO);
        }

        for (Expense expense : expenses) {
            if (paid.containsKey(expense.getPayerId())) {
                paid.put(expense.getPayerId(), paid.get(expense.getPayerId()).add(safe(expense.getTotalAmount())));
            }

            Map<String, BigDecimal> shares = calculateSharesForExpense(expense, persons);
            for (Map.Entry<String, BigDecimal> entry : shares.entrySet()) {
                owed.put(entry.getKey(), owed.getOrDefault(entry.getKey(), ZERO).add(entry.getValue()));
            }
        }

        List<Balance> balances = new ArrayList<>();
        for (Person person : persons) {
            balances.add(new Balance(person, scale(paid.getOrDefault(person.getId(), ZERO)), scale(owed.getOrDefault(person.getId(), ZERO))));
        }
        return balances;
    }

    public List<Settlement> calculateSettlements(List<Balance> balances) {
        List<MutableAmount> debtors = balances.stream()
                .filter(b -> b.getBalance().signum() < 0)
                .map(b -> new MutableAmount(b.getPerson(), b.getBalance().abs()))
                .sorted(Comparator.comparing(MutableAmount::amount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        List<MutableAmount> creditors = balances.stream()
                .filter(b -> b.getBalance().signum() > 0)
                .map(b -> new MutableAmount(b.getPerson(), b.getBalance()))
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

    public Optional<Person> findPersonById(List<Person> persons, String id) {
        if (id == null) {
            return Optional.empty();
        }
        return persons.stream().filter(p -> Objects.equals(p.getId(), id)).findFirst();
    }

    public void validateExpenseHasParticipants(Expense expense, List<Person> persons) {
        if (expense == null) {
            throw new IllegalArgumentException("La dépense est invalide.");
        }

        List<Person> eligibleActivePersons = persons == null
                ? List.of()
                : persons.stream()
                .filter(Person::isActive)
                .filter(this::canParticipateInAllocation)
                .toList();

        if (expense.isAdvancedMode()) {
            List<Person> manualParticipants = eligibleActivePersons.stream()
                    .filter(p -> expense.getManualParticipantIds().contains(p.getId()))
                    .toList();
            if (manualParticipants.isEmpty()) {
                throw new IllegalArgumentException("Dépense impossible : aucune personne active avec un reste à vivre positif ou en mode moyenne n'est sélectionnée en mode avancé.");
            }
            return;
        }

        if (expense.getType() == ExpenseType.GLOBAL) {
            if (eligibleActivePersons.isEmpty()) {
                throw new IllegalArgumentException("Dépense globale impossible : aucune personne active avec un reste à vivre positif ou en mode moyenne ne peut participer à la répartition.");
            }
            return;
        }

        BigDecimal meat = safe(expense.getMeatAmount());
        BigDecimal alcohol = safe(expense.getAlcoholAmount());
        BigDecimal total = safe(expense.getTotalAmount());
        BigDecimal general = total.subtract(meat).subtract(alcohol);
        if (general.signum() < 0) {
            general = ZERO;
        }

        List<Person> present = eligibleActivePersons.stream()
                .filter(p -> p.isPresentOn(expense.getDate()))
                .toList();
        List<Person> meatParticipants = present.stream()
                .filter(p -> !p.isVegetarian())
                .toList();
        List<Person> alcoholParticipants = present.stream()
                .filter(p -> !p.isNoAlcohol())
                .toList();

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

        if (!blockingReasons.isEmpty()) {
            throw new IllegalArgumentException("Dépense impossible : " + String.join(" ; ", blockingReasons) + ".");
        }
    }

    public String describeExpenseParticipants(Expense expense, List<Person> persons) {
        if (expense.isAdvancedMode()) {
            return persons.stream()
                    .filter(Person::isActive)
                    .filter(p -> expense.getManualParticipantIds().contains(p.getId()))
                    .map(Person::getName)
                    .collect(Collectors.joining(", "));
        }
        if (expense.getType() == ExpenseType.GLOBAL) {
            return persons.stream()
                    .filter(Person::isActive)
                    .map(Person::getName)
                    .collect(Collectors.joining(", "));
        }
        return persons.stream()
                .filter(Person::isActive)
                .filter(p -> p.isPresentOn(expense.getDate()))
                .map(Person::getName)
                .collect(Collectors.joining(", "));
    }

    private void addAllocation(Map<String, BigDecimal> shares, BigDecimal amount, List<Person> participants) {
        Map<String, BigDecimal> allocation = allocateProportionally(amount, participants);
        for (Map.Entry<String, BigDecimal> entry : allocation.entrySet()) {
            shares.put(entry.getKey(), shares.getOrDefault(entry.getKey(), ZERO).add(entry.getValue()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private Map<String, BigDecimal> allocateProportionally(BigDecimal amount, List<Person> participants) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal scaledAmount = safe(amount);
        if (scaledAmount.signum() <= 0 || participants == null || participants.isEmpty()) {
            return result;
        }

        List<Person> eligible = participants.stream()
                .filter(this::canParticipateInAllocation)
                .toList();
        if (eligible.isEmpty()) {
            return result;
        }

        Map<String, BigDecimal> weights = calculateEffectiveWeights(eligible);
        BigDecimal totalWeight = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() <= 0) {
            return result;
        }

        long cents = scaledAmount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        List<AllocationLine> lines = new ArrayList<>();
        long allocated = 0L;

        for (Person person : eligible) {
            BigDecimal weight = weights.getOrDefault(person.getId(), BigDecimal.ZERO);
            if (weight.signum() <= 0) {
                continue;
            }
            BigDecimal exactCents = weight
                    .multiply(BigDecimal.valueOf(cents))
                    .divide(totalWeight, 20, RoundingMode.HALF_UP);
            long floor = exactCents.setScale(0, RoundingMode.DOWN).longValue();
            BigDecimal remainder = exactCents.subtract(BigDecimal.valueOf(floor));
            lines.add(new AllocationLine(person.getId(), floor, remainder));
            allocated += floor;
        }

        long remaining = cents - allocated;
        lines.sort(Comparator.comparing(AllocationLine::remainder).reversed());
        for (int i = 0; i < remaining; i++) {
            AllocationLine line = lines.get(i % lines.size());
            line.cents += 1;
        }

        lines.sort(Comparator.comparing(AllocationLine::personId));
        for (AllocationLine line : lines) {
            result.put(line.personId, BigDecimal.valueOf(line.cents).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
        }

        return result;
    }

    private Map<String, BigDecimal> calculateEffectiveWeights(List<Person> eligible) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        List<Person> ravBasedPersons = eligible.stream()
                .filter(p -> !p.isAverageWeight())
                .filter(this::hasPositiveLivingRest)
                .toList();

        if (ravBasedPersons.isEmpty()) {
            for (Person person : eligible) {
                weights.put(person.getId(), BigDecimal.ONE);
            }
            return weights;
        }

        BigDecimal referenceTotal = ravBasedPersons.stream()
                .map(Person::getLivingRest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageWeight = referenceTotal.divide(BigDecimal.valueOf(ravBasedPersons.size()), 10, RoundingMode.HALF_UP);

        for (Person person : eligible) {
            if (person.isAverageWeight()) {
                weights.put(person.getId(), averageWeight);
            } else {
                weights.put(person.getId(), person.getLivingRest());
            }
        }
        return weights;
    }

    private boolean canParticipateInAllocation(Person person) {
        return person != null && (person.isAverageWeight() || hasPositiveLivingRest(person));
    }

    private boolean hasPositiveLivingRest(Person person) {
        return person != null && person.getLivingRest() != null && person.getLivingRest().signum() > 0;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value);
    }

    private static class AllocationLine {
        private final String personId;
        private long cents;
        private final BigDecimal remainder;

        private AllocationLine(String personId, long cents, BigDecimal remainder) {
            this.personId = personId;
            this.cents = cents;
            this.remainder = remainder;
        }

        private String personId() {
            return personId;
        }

        private BigDecimal remainder() {
            return remainder;
        }
    }

    private static class MutableAmount {
        private final Person person;
        private BigDecimal amount;

        private MutableAmount(Person person, BigDecimal amount) {
            this.person = person;
            this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal amount() {
            return amount;
        }
    }
}
