package com.vacances.ravtricount;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
import java.util.List;
import java.util.Locale;

public class PersonPanel extends JPanel {
    private final AppState state;
    private final Runnable onDataChanged;
    private final PersonTableModel tableModel;
    private final JTable table;

    private final JTextField nameField = new JTextField(20);
    private final JTextField livingRestField = new JTextField(10);
    private final JCheckBox advancedLivingRestBox = new JCheckBox("Calcul avancé du reste à vivre");
    private final JTextField netIncomeField = new JTextField(10);
    private final JTextField rentField = new JTextField(10);
    private final JTextField creditsField = new JTextField(10);
    private final JTextField fixedChargesField = new JTextField(10);
    private final JTextField transportField = new JTextField(10);
    private final JTextField insuranceField = new JTextField(10);
    private final JTextField otherMandatoryExpensesField = new JTextField(10);
    private final JButton calculateLivingRestButton = new JButton("Calculer le RAV");
    private final JCheckBox averageWeightBox = new JCheckBox("Poids moyen dans les dépenses");
    private final JCheckBox vegetarianBox = new JCheckBox("Végétarien");
    private final JCheckBox noAlcoholBox = new JCheckBox("Sans alcool");
    private final JTextField presenceStartField = new JTextField(10);
    private final JTextField presenceEndField = new JTextField(10);
    private final JCheckBox activeBox = new JCheckBox("Actif", true);

    public PersonPanel(AppState state, Runnable onDataChanged) {
        this.state = state;
        this.onDataChanged = onDataChanged;
        this.tableModel = new PersonTableModel(state.getPersons());
        this.table = new JTable(tableModel);
        buildLayout();
        configureSelection();
        clearForm();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Personne"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, c, row++, "Nom", nameField);
        addRow(form, c, row++, "Reste à vivre", livingRestField);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(advancedLivingRestBox, c);
        row++;

        addRow(form, c, row++, "Salaire net après impôt", netIncomeField);
        addRow(form, c, row++, "Loyer", rentField);
        addRow(form, c, row++, "Crédits", creditsField);
        addRow(form, c, row++, "Charges fixes", fixedChargesField);
        addRow(form, c, row++, "Transport", transportField);
        addRow(form, c, row++, "Assurances", insuranceField);
        addRow(form, c, row++, "Autres charges contraintes", otherMandatoryExpensesField);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(calculateLivingRestButton, c);
        row++;

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(averageWeightBox, c);
        row++;

        addRow(form, c, row++, "Début présence", presenceStartField);
        addRow(form, c, row++, "Fin présence", presenceEndField);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(vegetarianBox, c);
        row++;
        c.gridy = row;
        form.add(noAlcoholBox, c);
        row++;
        c.gridy = row;
        form.add(activeBox, c);
        row++;

        JPanel buttons = new JPanel();
        JButton addButton = new JButton("Ajouter");
        JButton updateButton = new JButton("Modifier");
        JButton deactivateButton = new JButton("Désactiver / supprimer");
        JButton clearButton = new JButton("Vider");

        addButton.addActionListener(e -> addPerson());
        updateButton.addActionListener(e -> updateSelectedPerson());
        deactivateButton.addActionListener(e -> deactivateSelectedPerson());
        clearButton.addActionListener(e -> clearForm());
        advancedLivingRestBox.addActionListener(e -> updateLivingRestModeState());
        calculateLivingRestButton.addActionListener(e -> calculateAndDisplayLivingRest());

        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deactivateButton);
        buttons.add(clearButton);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(buttons, c);

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

    private void configureSelection() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    fillForm(tableModel.getPersonAt(row));
                }
            }
        });
    }

    private void addPerson() {
        try {
            Person person = readPersonFromForm(null);
            state.getPersons().add(person);
            afterMutation();
            clearForm();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void updateSelectedPerson() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtils.showInfo(this, "Sélectionne une personne à modifier.");
            return;
        }
        try {
            Person selected = tableModel.getPersonAt(row);
            readPersonFromForm(selected);
            afterMutation();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void deactivateSelectedPerson() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtils.showInfo(this, "Sélectionne une personne à désactiver.");
            return;
        }
        Person selected = tableModel.getPersonAt(row);
        selected.setActive(false);
        afterMutation();
        fillForm(selected);
    }

    private Person readPersonFromForm(Person existing) {
        String name = normalizeDisplayName(nameField.getText());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        ensureUniqueName(name, existing);

        boolean advancedLivingRest = advancedLivingRestBox.isSelected();
        boolean averageWeight = averageWeightBox.isSelected();
        AdvancedLivingRestInput advancedInput = readAdvancedLivingRestInput(advancedLivingRest);
        BigDecimal livingRest = advancedLivingRest
                ? advancedInput.calculateLivingRest()
                : UiUtils.parseMoney(livingRestField.getText(), "Reste à vivre");

        if (advancedLivingRest) {
            livingRestField.setText(livingRest.toPlainString());
        }
        if (livingRest.signum() < 0) {
            throw new IllegalArgumentException("Le reste à vivre calculé ne peut pas être négatif.");
        }
        if (!averageWeight && livingRest.signum() <= 0) {
            throw new IllegalArgumentException("Le reste à vivre doit être strictement positif, sauf si la personne utilise le poids moyen.");
        }

        LocalDate start = UiUtils.parseDate(presenceStartField.getText(), "Début présence");
        LocalDate end = UiUtils.parseDate(presenceEndField.getText(), "Fin présence");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La date de fin de présence ne peut pas être avant la date de début.");
        }

        Person person = existing == null
                ? new Person(name, livingRest, vegetarianBox.isSelected(), noAlcoholBox.isSelected(), start, end)
                : existing;

        person.setName(name);
        person.setLivingRest(livingRest);
        person.setAdvancedLivingRest(advancedLivingRest);
        person.setAverageWeight(averageWeight);
        person.setNetIncomeAfterTax(advancedInput.netIncomeAfterTax());
        person.setRent(advancedInput.rent());
        person.setCredits(advancedInput.credits());
        person.setFixedCharges(advancedInput.fixedCharges());
        person.setTransport(advancedInput.transport());
        person.setInsurance(advancedInput.insurance());
        person.setOtherMandatoryExpenses(advancedInput.otherMandatoryExpenses());
        person.setVegetarian(vegetarianBox.isSelected());
        person.setNoAlcohol(noAlcoholBox.isSelected());
        person.setPresenceStart(start);
        person.setPresenceEnd(end);
        person.setActive(activeBox.isSelected());
        return person;
    }

    private AdvancedLivingRestInput readAdvancedLivingRestInput(boolean advancedLivingRest) {
        if (!advancedLivingRest) {
            return new AdvancedLivingRestInput(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }
        return new AdvancedLivingRestInput(
                UiUtils.parseMoney(netIncomeField.getText(), "Salaire net après impôt"),
                UiUtils.parseMoney(rentField.getText(), "Loyer"),
                UiUtils.parseMoney(creditsField.getText(), "Crédits"),
                UiUtils.parseMoney(fixedChargesField.getText(), "Charges fixes"),
                UiUtils.parseMoney(transportField.getText(), "Transport"),
                UiUtils.parseMoney(insuranceField.getText(), "Assurances"),
                UiUtils.parseMoney(otherMandatoryExpensesField.getText(), "Autres charges contraintes")
        );
    }

    private void calculateAndDisplayLivingRest() {
        try {
            AdvancedLivingRestInput input = readAdvancedLivingRestInput(true);
            BigDecimal livingRest = input.calculateLivingRest();
            livingRestField.setText(livingRest.toPlainString());
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void ensureUniqueName(String name, Person currentPerson) {
        String normalizedCandidate = normalizeNameForComparison(name);
        for (Person person : state.getPersons()) {
            if (person == currentPerson) {
                continue;
            }
            if (normalizeNameForComparison(person.getName()).equals(normalizedCandidate)) {
                throw new IllegalArgumentException(
                        "Ce nom existe déjà. Deux personnes ne peuvent pas avoir le même nom."
                );
            }
        }
    }

    private String normalizeDisplayName(String rawName) {
        return rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNameForComparison(String name) {
        return normalizeDisplayName(name).toLowerCase(Locale.ROOT);
    }

    private void fillForm(Person p) {
        nameField.setText(p.getName());
        livingRestField.setText(p.getLivingRest().toPlainString());
        advancedLivingRestBox.setSelected(p.isAdvancedLivingRest());
        averageWeightBox.setSelected(p.isAverageWeight());
        netIncomeField.setText(p.getNetIncomeAfterTax().toPlainString());
        rentField.setText(p.getRent().toPlainString());
        creditsField.setText(p.getCredits().toPlainString());
        fixedChargesField.setText(p.getFixedCharges().toPlainString());
        transportField.setText(p.getTransport().toPlainString());
        insuranceField.setText(p.getInsurance().toPlainString());
        otherMandatoryExpensesField.setText(p.getOtherMandatoryExpenses().toPlainString());
        vegetarianBox.setSelected(p.isVegetarian());
        noAlcoholBox.setSelected(p.isNoAlcohol());
        presenceStartField.setText(String.valueOf(p.getPresenceStart()));
        presenceEndField.setText(String.valueOf(p.getPresenceEnd()));
        activeBox.setSelected(p.isActive());
        updateLivingRestModeState();
    }

    private void clearForm() {
        nameField.setText("");
        livingRestField.setText("");
        advancedLivingRestBox.setSelected(false);
        averageWeightBox.setSelected(false);
        for (JTextField field : advancedFields()) {
            field.setText("0");
        }
        vegetarianBox.setSelected(false);
        noAlcoholBox.setSelected(false);
        LocalDate today = LocalDate.now();
        presenceStartField.setText(today.toString());
        presenceEndField.setText(today.plusDays(7).toString());
        activeBox.setSelected(true);
        table.clearSelection();
        updateLivingRestModeState();
    }

    private void updateLivingRestModeState() {
        boolean advanced = advancedLivingRestBox.isSelected();
        livingRestField.setEditable(!advanced);
        calculateLivingRestButton.setEnabled(advanced);
        for (JTextField field : advancedFields()) {
            field.setEnabled(advanced);
        }
    }

    private List<JTextField> advancedFields() {
        return List.of(
                netIncomeField,
                rentField,
                creditsField,
                fixedChargesField,
                transportField,
                insuranceField,
                otherMandatoryExpensesField
        );
    }

    private void afterMutation() {
        tableModel.refresh();
        onDataChanged.run();
    }

    public void refresh() {
        tableModel.refresh();
    }

    private record AdvancedLivingRestInput(
            BigDecimal netIncomeAfterTax,
            BigDecimal rent,
            BigDecimal credits,
            BigDecimal fixedCharges,
            BigDecimal transport,
            BigDecimal insurance,
            BigDecimal otherMandatoryExpenses
    ) {
        private BigDecimal calculateLivingRest() {
            return netIncomeAfterTax
                    .subtract(rent)
                    .subtract(credits)
                    .subtract(fixedCharges)
                    .subtract(transport)
                    .subtract(insurance)
                    .subtract(otherMandatoryExpenses)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}
