package fr.achabitation.application;

import fr.achabitation.api.dto.SummaryDtos.SummaryResponse;
import fr.achabitation.infrastructure.entity.AuditAction;
import fr.achabitation.infrastructure.entity.ExpenseEntity;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ExportService {
    private final ExpenseRepository expenseRepository;
    private final TripService tripService;
    private final SummaryService summaryService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public ExportService(ExpenseRepository expenseRepository, TripService tripService, SummaryService summaryService, AuthorizationService authorizationService, AuditService auditService) {
        this.expenseRepository = expenseRepository;
        this.tripService = tripService;
        this.summaryService = summaryService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public byte[] expensesCsv(UUID tripId, UserEntity actor) {
        authorizationService.requireReadable(tripId, actor);
        TripEntity trip = tripService.getRequired(tripId);
        StringBuilder csv = new StringBuilder();
        csv.append("Titre;Date;Payeur;Montant total;Viande;Alcool;Contraintes;Type;Devise;Taux vers devise voyage\n");
        for (ExpenseEntity expense : expenseRepository.findByTripIdOrderByDateAscTitleAsc(tripId)) {
            csv.append(cell(expense.getTitle())).append(';')
                    .append(cell(String.valueOf(expense.getDate()))).append(';')
                    .append(cell(expense.getPayer().getName())).append(';')
                    .append(number(expense.getTotalAmount())).append(';')
                    .append(number(expense.getMeatAmount())).append(';')
                    .append(number(expense.getAlcoholAmount())).append(';')
                    .append(cell(expense.getCustomConstraintAmounts().toString())).append(';')
                    .append(cell(String.valueOf(expense.getType()))).append(';')
                    .append(cell(expense.getCurrency())).append(';')
                    .append(expense.getExchangeRateToTripCurrency()).append('\n');
        }
        auditService.log(trip, actor, AuditAction.EXPORT_GENERATED, "EXPORT", trip.getId(), "Export CSV des dépenses généré.");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public byte[] summaryCsv(UUID tripId, UserEntity actor) {
        authorizationService.requireReadable(tripId, actor);
        TripEntity trip = tripService.getRequired(tripId);
        SummaryResponse summary = summaryService.summary(tripId, actor);
        StringBuilder csv = new StringBuilder();
        csv.append("Personne;Total payé;Total dû;Solde\n");
        summary.balances().forEach(balance -> csv.append(cell(balance.personName())).append(';')
                .append(number(balance.totalPaid())).append(';')
                .append(number(balance.totalOwed())).append(';')
                .append(number(balance.balance())).append('\n'));
        csv.append('\n').append("Rembourseur;Bénéficiaire;Montant\n");
        summary.settlements().forEach(settlement -> csv.append(cell(settlement.fromPersonName())).append(';')
                .append(cell(settlement.toPersonName())).append(';')
                .append(number(settlement.amount())).append('\n'));
        auditService.log(trip, actor, AuditAction.EXPORT_GENERATED, "EXPORT", trip.getId(), "Export CSV du résumé généré.");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String number(BigDecimal value) {
        return value == null ? "0,00" : value.toPlainString().replace('.', ',');
    }

    private String cell(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
