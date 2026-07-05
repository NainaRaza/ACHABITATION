package com.vacances.ravtricount;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExpensePanel extends JPanel {
    private final AppState state;
    private final BalanceService balanceService;
    private final Runnable onDataChanged;
    private final ExpenseTableModel tableModel;
    private final JTable table;

    private final JTextField titleField = new JTextField(20);
    private final JTextField dateField = new JTextField(10);
    private final JComboBox<Person> payerCombo = new JComboBox<>();
    private final JTextField totalField = new JTextField(10);
    private final JTextField meatField = new JTextField(10);
    private final JTextField alcoholField = new JTextField(10);
    private final JComboBox<ExpenseType> typeCombo = new JComboBox<>(ExpenseType.values());
    private final JCheckBox advancedBox = new JCheckBox("Mode avancé : choisir manuellement les participant·es");
    private final DefaultListModel<Person> participantsModel = new DefaultListModel<>();
    private final JList<Person> participantsList = new JList<>(participantsModel);

    public ExpensePanel(AppState state, BalanceService balanceService, Runnable onDataChanged) {
        this.state = state;
        this.balanceService = balanceService;
        this.onDataChanged = onDataChanged;
        this.tableModel = new ExpenseTableModel(state.getExpenses(), state.getPersons(), balanceService);
        this.table = new JTable(tableModel);
        buildLayout();
        configureSelection();
        refreshPersonInputs();
        clearForm();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Dépense"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, c, row++, "Titre", titleField);
        addRow(form, c, row++, "Date", dateField);
        addComboRow(form, c, row++, "Payé par", payerCombo);
        addRow(form, c, row++, "Prix total", totalField);
        addRow(form, c, row++, "Prix viande", meatField);
        addRow(form, c, row++, "Prix alcool", alcoholField);
        addComboRow(form, c, row++, "Type", typeCombo);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(advancedBox, c);
        row++;

        participantsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane participantsScroll = new JScrollPane(participantsList);
        participantsScroll.setBorder(BorderFactory.createTitledBorder("Participant·es en mode avancé"));
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        form.add(participantsScroll, c);
        row++;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel buttons = new JPanel();
        JButton addButton = new JButton("Ajouter");
        JButton updateButton = new JButton("Modifier");
        JButton deleteButton = new JButton("Supprimer");
        JButton clearButton = new JButton("Vider");

        addButton.addActionListener(e -> addExpense());
        updateButton.addActionListener(e -> updateSelectedExpense());
        deleteButton.addActionListener(e -> deleteSelectedExpense());
        clearButton.addActionListener(e -> clearForm());

        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(buttons, c);

        advancedBox.addActionListener(e -> updateAdvancedState());
        typeCombo.addActionListener(e -> updateAdvancedState());

        add(form, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    private void addComboRow(JPanel panel, GridBagConstraints c, int row, String label, JComboBox<?> combo) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(combo, c);
    }

    private void configureSelection() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    fillForm(tableModel.getExpenseAt(row));
                }
            }
        });
    }

    private void addExpense() {
        try {
            Expense expense = readExpenseFromForm(null);
            state.getExpenses().add(expense);
            afterMutation();
            clearForm();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void updateSelectedExpense() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtils.showInfo(this, "Sélectionne une dépense à modifier.");
            return;
        }
        try {
            Expense selected = tableModel.getExpenseAt(row);
            readExpenseFromForm(selected);
            afterMutation();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void deleteSelectedExpense() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtils.showInfo(this, "Sélectionne une dépense à supprimer.");
            return;
        }
        state.getExpenses().remove(tableModel.getExpenseAt(row));
        afterMutation();
        clearForm();
    }

    private Expense readExpenseFromForm(Expense existing) {
        String title = titleField.getText().trim();
        if (title.isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire.");
        }
        Person payer = (Person) payerCombo.getSelectedItem();
        if (payer == null) {
            throw new IllegalArgumentException("Il faut au moins une personne active pour saisir une dépense.");
        }

        LocalDate date = UiUtils.parseDate(dateField.getText(), "Date");
        BigDecimal total = UiUtils.parseMoney(totalField.getText(), "Prix total");
        BigDecimal meat = UiUtils.parseMoney(meatField.getText(), "Prix viande");
        BigDecimal alcohol = UiUtils.parseMoney(alcoholField.getText(), "Prix alcool");
        ExpenseType type = (ExpenseType) typeCombo.getSelectedItem();
        boolean advanced = advancedBox.isSelected();
        Set<String> participantIds = selectedParticipantIds();

        if (total.signum() <= 0) {
            throw new IllegalArgumentException("Le prix total doit être strictement positif.");
        }
        if (meat.signum() < 0 || alcohol.signum() < 0) {
            throw new IllegalArgumentException("Les montants viande et alcool ne peuvent pas être négatifs.");
        }
        if (type == ExpenseType.GLOBAL) {
            meat = BigDecimal.ZERO.setScale(2);
            alcohol = BigDecimal.ZERO.setScale(2);
        } else if (meat.add(alcohol).compareTo(total) > 0) {
            throw new IllegalArgumentException("La somme viande + alcool ne peut pas dépasser le prix total.");
        }
        if (advanced && participantIds.isEmpty()) {
            throw new IllegalArgumentException("En mode avancé, sélectionne au moins une personne.");
        }

        Expense candidate = new Expense(title, date, payer.getId(), total, meat, alcohol, type, advanced, participantIds);
        balanceService.validateExpenseHasParticipants(candidate, state.getPersons());

        if (existing == null) {
            return candidate;
        }

        existing.setTitle(candidate.getTitle());
        existing.setDate(candidate.getDate());
        existing.setPayerId(candidate.getPayerId());
        existing.setTotalAmount(candidate.getTotalAmount());
        existing.setMeatAmount(candidate.getMeatAmount());
        existing.setAlcoholAmount(candidate.getAlcoholAmount());
        existing.setType(candidate.getType());
        existing.setAdvancedMode(candidate.isAdvancedMode());
        existing.setManualParticipantIds(candidate.getManualParticipantIds());
        return existing;
    }

    private Set<String> selectedParticipantIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Person p : participantsList.getSelectedValuesList()) {
            ids.add(p.getId());
        }
        return ids;
    }

    private void fillForm(Expense e) {
        titleField.setText(e.getTitle());
        dateField.setText(String.valueOf(e.getDate()));
        selectPayer(e.getPayerId());
        totalField.setText(e.getTotalAmount().toPlainString());
        meatField.setText(e.getMeatAmount().toPlainString());
        alcoholField.setText(e.getAlcoholAmount().toPlainString());
        typeCombo.setSelectedItem(e.getType());
        advancedBox.setSelected(e.isAdvancedMode());
        selectParticipants(e.getManualParticipantIds());
        updateAdvancedState();
    }

    private void selectPayer(String payerId) {
        for (int i = 0; i < payerCombo.getItemCount(); i++) {
            Person p = payerCombo.getItemAt(i);
            if (p.getId().equals(payerId)) {
                payerCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectParticipants(Set<String> ids) {
        participantsList.clearSelection();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int[] selectedIndices = java.util.stream.IntStream.range(0, participantsModel.size())
                .filter(i -> ids.contains(participantsModel.get(i).getId()))
                .toArray();
        participantsList.setSelectedIndices(selectedIndices);
    }

    private void clearForm() {
        titleField.setText("");
        dateField.setText(LocalDate.now().toString());
        if (payerCombo.getItemCount() > 0) {
            payerCombo.setSelectedIndex(0);
        }
        totalField.setText("");
        meatField.setText("0");
        alcoholField.setText("0");
        typeCombo.setSelectedItem(ExpenseType.NORMAL);
        advancedBox.setSelected(false);
        participantsList.clearSelection();
        table.clearSelection();
        updateAdvancedState();
    }

    private void updateAdvancedState() {
        boolean advanced = advancedBox.isSelected();
        boolean global = typeCombo.getSelectedItem() == ExpenseType.GLOBAL;
        participantsList.setEnabled(advanced);
        meatField.setEnabled(!global && !advanced);
        alcoholField.setEnabled(!global && !advanced);
        if (global) {
            meatField.setText("0");
            alcoholField.setText("0");
        }
    }

    private void afterMutation() {
        tableModel.refresh();
        onDataChanged.run();
    }

    public void refresh() {
        refreshPersonInputs();
        tableModel.refresh();
    }

    private void refreshPersonInputs() {
        DefaultComboBoxModel<Person> comboModel = new DefaultComboBoxModel<>();
        participantsModel.clear();
        for (Person p : state.getPersons()) {
            if (p.isActive()) {
                comboModel.addElement(p);
                participantsModel.addElement(p);
            }
        }
        payerCombo.setModel(comboModel);
    }
}
