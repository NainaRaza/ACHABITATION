package com.vacances.ravtricount;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SummaryPanel extends JPanel {
    private final AppState state;
    private final BalanceService balanceService;
    private final JTextArea textArea = new JTextArea();

    public SummaryPanel(AppState state, BalanceService balanceService) {
        this.state = state;
        this.balanceService = balanceService;
        buildLayout();
        refresh();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Recalculer");
        refreshButton.addActionListener(e -> refresh());
        JPanel top = new JPanel(new BorderLayout());
        top.add(refreshButton, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
    }

    public void refresh() {
        StringBuilder sb = new StringBuilder();
        sb.append("RÉSUMÉ GLOBAL\n");
        sb.append("=============" + "\n\n");

        if (state.getPersons().isEmpty()) {
            sb.append("Aucune personne enregistrée.\n");
            textArea.setText(sb.toString());
            return;
        }

        List<Balance> balances = balanceService.calculateBalances(state.getExpenses(), state.getPersons());
        balances.sort(Comparator.comparing(b -> b.getPerson().getName().toLowerCase()));

        sb.append("Par personne\n");
        sb.append("------------\n");
        for (Balance balance : balances) {
            sb.append(String.format("%-24s payé : %10s | dû : %10s | solde : %10s%n",
                    balance.getPerson().getName(),
                    UiUtils.money(balance.getTotalPaid()),
                    UiUtils.money(balance.getTotalOwed()),
                    formatSigned(balance.getBalance())));
        }

        sb.append("\nRemboursements à faire\n");
        sb.append("----------------------\n");
        List<Settlement> settlements = balanceService.calculateSettlements(balances);
        if (settlements.isEmpty()) {
            sb.append("Aucun remboursement nécessaire.\n");
        } else {
            for (Settlement settlement : settlements) {
                sb.append(settlement.getFrom().getName())
                        .append(" rembourse ")
                        .append(UiUtils.money(settlement.getAmount()))
                        .append(" à ")
                        .append(settlement.getTo().getName())
                        .append("\n");
            }
        }

        sb.append("\nDétail des dépenses\n");
        sb.append("-------------------\n");
        if (state.getExpenses().isEmpty()) {
            sb.append("Aucune dépense enregistrée.\n");
        } else {
            for (Expense expense : state.getExpenses()) {
                appendExpenseDetail(sb, expense);
            }
        }

        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
    }

    private void appendExpenseDetail(StringBuilder sb, Expense expense) {
        Optional<Person> payer = balanceService.findPersonById(state.getPersons(), expense.getPayerId());
        sb.append("\n")
                .append(expense.getDate())
                .append(" - ")
                .append(expense.getTitle())
                .append(" - ")
                .append(expense.getType().getLabel())
                .append(" - payé par ")
                .append(payer.map(Person::getName).orElse("?"))
                .append(" - ")
                .append(UiUtils.money(expense.getTotalAmount()))
                .append("\n");

        if (expense.getType() == ExpenseType.NORMAL && !expense.isAdvancedMode()) {
            BigDecimal general = expense.getTotalAmount()
                    .subtract(expense.getMeatAmount())
                    .subtract(expense.getAlcoholAmount())
                    .setScale(2, RoundingMode.HALF_UP);
            sb.append("  Général : ").append(UiUtils.money(general))
                    .append(" | Viande : ").append(UiUtils.money(expense.getMeatAmount()))
                    .append(" | Alcool : ").append(UiUtils.money(expense.getAlcoholAmount()))
                    .append("\n");
        }

        String participants = balanceService.describeExpenseParticipants(expense, state.getPersons());
        sb.append("  Participant·es pris·es en compte : ")
                .append(participants.isBlank() ? "aucun·e" : participants)
                .append("\n");

        Map<String, BigDecimal> shares = balanceService.calculateSharesForExpense(expense, state.getPersons());
        shares.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .sorted(Comparator.comparing(e -> balanceService.findPersonById(state.getPersons(), e.getKey()).map(Person::getName).orElse("")))
                .forEach(e -> {
                    String name = balanceService.findPersonById(state.getPersons(), e.getKey()).map(Person::getName).orElse("?");
                    sb.append("    - ").append(name).append(" doit ").append(UiUtils.money(e.getValue())).append("\n");
                });
    }

    private String formatSigned(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        String formatted = UiUtils.money(scaled.abs());
        if (scaled.signum() > 0) {
            return "+" + formatted;
        }
        if (scaled.signum() < 0) {
            return "-" + formatted;
        }
        return formatted;
    }
}
