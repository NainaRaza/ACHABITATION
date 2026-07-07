package fr.achabitation.application;

import fr.achabitation.api.dto.SummaryDtos.BalanceResponse;
import fr.achabitation.api.dto.SummaryDtos.SettlementResponse;
import fr.achabitation.api.dto.SummaryDtos.SummaryResponse;
import fr.achabitation.domain.BalanceCalculator;
import fr.achabitation.domain.model.Balance;
import fr.achabitation.domain.model.DomainExpense;
import fr.achabitation.domain.model.DomainPerson;
import fr.achabitation.domain.model.Settlement;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.ExpenseRepository;
import fr.achabitation.infrastructure.repository.PersonRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SummaryService {
    private final PersonRepository personRepository;
    private final ExpenseRepository expenseRepository;
    private final TripService tripService;
    private final EntityMapper mapper;
    private final BalanceCalculator balanceCalculator = new BalanceCalculator();
    private final AuthorizationService authorizationService;

    public SummaryService(PersonRepository personRepository, ExpenseRepository expenseRepository, TripService tripService, EntityMapper mapper, AuthorizationService authorizationService) {
        this.personRepository = personRepository;
        this.expenseRepository = expenseRepository;
        this.tripService = tripService;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID tripId, UserEntity actor) {
        authorizationService.requireReadable(tripId, actor);
        TripEntity trip = tripService.getRequired(tripId);
        List<DomainPerson> persons = personRepository.findByTripIdOrderByNameAsc(tripId).stream()
                .map(mapper::toDomainPerson)
                .toList();
        List<DomainExpense> expenses = expenseRepository.findByTripIdOrderByDateAscTitleAsc(tripId).stream()
                .map(mapper::toDomainExpense)
                .toList();

        List<Balance> balances = balanceCalculator.calculateBalances(expenses, persons);
        List<Settlement> settlements = balanceCalculator.calculateSettlements(balances);

        if (!authorizationService.isAdmin(tripId, actor)) {
            Set<UUID> visiblePersonIds = personRepository.findByTripIdAndLinkedUserId(tripId, actor.getId())
                    .map(person -> Set.of(person.getId()))
                    .orElse(Set.of());
            balances = balances.stream()
                    .filter(balance -> visiblePersonIds.contains(balance.person().id()))
                    .toList();
            settlements = settlements.stream()
                    .filter(settlement -> visiblePersonIds.contains(settlement.from().id()) || visiblePersonIds.contains(settlement.to().id()))
                    .toList();
        }

        return new SummaryResponse(
                trip.getReferenceCurrency(),
                balances.stream().map(b -> new BalanceResponse(
                        b.person().id(),
                        b.person().name(),
                        b.totalPaid(),
                        b.totalOwed(),
                        b.balance()
                )).toList(),
                settlements.stream().map(s -> new SettlementResponse(
                        s.from().id(),
                        s.from().name(),
                        s.to().id(),
                        s.to().name(),
                        s.amount()
                )).toList()
        );
    }
}
