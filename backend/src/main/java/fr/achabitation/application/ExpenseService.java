package fr.achabitation.application;

import fr.achabitation.api.dto.ExpenseDtos.ExpenseCreateRequest;
import fr.achabitation.api.dto.ExpenseDtos.ExpenseResponse;
import fr.achabitation.domain.BalanceCalculator;
import fr.achabitation.domain.model.ExpenseType;
import fr.achabitation.domain.util.ConstraintNameUtils;
import fr.achabitation.infrastructure.entity.AuditAction;
import fr.achabitation.infrastructure.entity.ExpenseEntity;
import fr.achabitation.infrastructure.entity.ExpenseParticipantEntity;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.ExpenseRepository;
import fr.achabitation.infrastructure.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;
    private final PersonService personService;
    private final TripService tripService;
    private final EntityMapper mapper;
    private final AuditService auditService;
    private final BalanceCalculator balanceCalculator = new BalanceCalculator();
    private final AuthorizationService authorizationService;

    public ExpenseService(ExpenseRepository expenseRepository, PersonRepository personRepository, PersonService personService, TripService tripService, EntityMapper mapper, AuditService auditService, AuthorizationService authorizationService) {
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
        this.personService = personService;
        this.tripService = tripService;
        this.mapper = mapper;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ExpenseResponse create(UUID tripId, ExpenseCreateRequest request, UserEntity actor) {
        authorizationService.requireWritable(tripId, actor);
        TripEntity trip = tripService.getRequired(tripId);
        PersonEntity payer = personService.getRequiredInTrip(tripId, request.payerPersonId());

        ExpenseEntity expense = new ExpenseEntity();
        expense.setTrip(trip);
        expense.setPayer(payer);
        apply(expense, request, tripId);

        validateExpense(expense, tripId);
        expense = expenseRepository.save(expense);
        auditService.log(trip, actor, AuditAction.EXPENSE_CREATED, "EXPENSE", expense.getId(), "Dépense créée : " + expense.getTitle());
        return mapper.toExpenseResponse(expense);
    }

    @Transactional
    public ExpenseResponse update(UUID tripId, UUID expenseId, ExpenseCreateRequest request, UserEntity actor) {
        authorizationService.requireWritable(tripId, actor);
        ExpenseEntity expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Dépense introuvable."));
        if (!expense.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette dépense n'appartient pas au voyage indiqué.");
        }
        PersonEntity payer = personService.getRequiredInTrip(tripId, request.payerPersonId());
        expense.setPayer(payer);
        apply(expense, request, tripId);
        validateExpense(expense, tripId);
        expense = expenseRepository.save(expense);
        auditService.log(expense.getTrip(), actor, AuditAction.EXPENSE_UPDATED, "EXPENSE", expense.getId(), "Dépense modifiée : " + expense.getTitle());
        return mapper.toExpenseResponse(expense);
    }

    @Transactional
    public void delete(UUID tripId, UUID expenseId, UserEntity actor) {
        authorizationService.requireWritable(tripId, actor);
        ExpenseEntity expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Dépense introuvable."));
        if (!expense.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette dépense n'appartient pas au voyage indiqué.");
        }
        expenseRepository.delete(expense);
        auditService.log(expense.getTrip(), actor, AuditAction.EXPENSE_DELETED, "EXPENSE", expense.getId(), "Dépense supprimée : " + expense.getTitle());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID tripId, UserEntity actor) {
        authorizationService.requireReadable(tripId, actor);
        return expenseRepository.findByTripIdOrderByDateAscTitleAsc(tripId).stream()
                .map(mapper::toExpenseResponse)
                .toList();
    }

    private void apply(ExpenseEntity expense, ExpenseCreateRequest request, UUID tripId) {
        BigDecimal meat = mapper.money(request.meatAmount());
        BigDecimal alcohol = mapper.money(request.alcoholAmount());
        BigDecimal total = mapper.money(request.totalAmount());
        Map<String, BigDecimal> customConstraintAmounts = normalizeCustomConstraintAmounts(expense.getTrip(), request.customConstraintAmounts());
        BigDecimal detailsTotal = meat.add(alcohol).add(customConstraintAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        if (detailsTotal.compareTo(total) > 0) {
            throw new IllegalArgumentException("La somme viande + alcool + contraintes personnalisées ne peut pas dépasser le total.");
        }

        expense.setTitle(request.title().trim());
        expense.setDate(request.date());
        expense.setTotalAmount(total);
        expense.setMeatAmount(meat);
        expense.setAlcoholAmount(alcohol);
        expense.getCustomConstraintAmounts().clear();
        if (request.type() == null || request.type() == ExpenseType.NORMAL) {
            expense.getCustomConstraintAmounts().putAll(customConstraintAmounts);
        }
        expense.setType(request.type() == null ? ExpenseType.NORMAL : request.type());
        expense.setAdvancedMode(request.advancedMode());
        expense.setCurrency(request.currency() == null || request.currency().isBlank() ? expense.getTrip().getReferenceCurrency() : request.currency().trim().toUpperCase());
        expense.setExchangeRateToTripCurrency(mapper.positiveRate(request.exchangeRateToTripCurrency()));

        expense.getManualParticipants().clear();
        if (request.advancedMode()) {
            Set<UUID> participantIds = request.manualParticipantIds() == null ? Set.of() : new LinkedHashSet<>(request.manualParticipantIds());
            for (UUID participantId : participantIds) {
                PersonEntity person = personService.getRequiredInTrip(tripId, participantId);
                ExpenseParticipantEntity participant = new ExpenseParticipantEntity();
                participant.setExpense(expense);
                participant.setPerson(person);
                expense.getManualParticipants().add(participant);
            }
        }
    }


    private Map<String, BigDecimal> normalizeCustomConstraintAmounts(TripEntity trip, Map<String, BigDecimal> input) {
        LinkedHashMap<String, BigDecimal> result = new LinkedHashMap<>();
        if (input == null || input.isEmpty()) {
            return result;
        }
        Set<String> seenKeys = new LinkedHashSet<>();
        Set<String> allowedKeys = new LinkedHashSet<>();
        if (trip.getCustomConstraints() != null) {
            trip.getCustomConstraints().forEach(value -> allowedKeys.add(ConstraintNameUtils.key(value)));
        }
        input.forEach((rawName, rawAmount) -> {
            String displayName = ConstraintNameUtils.canonicalDisplayName(rawName);
            String key = ConstraintNameUtils.key(displayName);
            if (key.isBlank()) {
                return;
            }
            if (displayName.length() > 120) {
                throw new IllegalArgumentException("Le nom d'une contrainte personnalisée ne doit pas dépasser 120 caractères.");
            }
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("La contrainte \"" + displayName + "\" n'est pas déclarée dans ce voyage.");
            }
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("Une contrainte personnalisée est renseignée plusieurs fois sur la dépense.");
            }
            BigDecimal amount = mapper.money(rawAmount);
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("Le montant d'une contrainte personnalisée ne peut pas être négatif.");
            }
            if (amount.signum() > 0) {
                result.put(displayName, amount);
            }
        });
        return result;
    }

    private void validateExpenseDateWithinTrip(ExpenseEntity expense) {
        if (expense.getDate() == null) {
            throw new IllegalArgumentException("La date de dépense est obligatoire.");
        }
        TripEntity trip = expense.getTrip();
        if (trip.getStartDate() != null && expense.getDate().isBefore(trip.getStartDate())) {
            throw new IllegalArgumentException("La date de dépense est antérieure au début du voyage.");
        }
        if (trip.getEndDate() != null && expense.getDate().isAfter(trip.getEndDate())) {
            throw new IllegalArgumentException("La date de dépense est postérieure à la fin du voyage.");
        }
    }

    private void validateExpense(ExpenseEntity expense, UUID tripId) {
        validateExpenseDateWithinTrip(expense);
        List<PersonEntity> persons = personRepository.findByTripIdOrderByNameAsc(tripId);
        balanceCalculator.validateExpenseHasParticipants(
                mapper.toDomainExpense(expense),
                persons.stream().map(mapper::toDomainPerson).toList()
        );
    }
}
