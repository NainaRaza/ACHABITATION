package com.vacances.ravtricount;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ExpenseTableModel extends AbstractTableModel {
    private final List<Expense> expenses;
    private final List<Person> persons;
    private final BalanceService balanceService;
    private final String[] columns = {"Titre", "Date", "Type", "Payeur", "Total", "Viande", "Alcool", "Avancé"};

    public ExpenseTableModel(List<Expense> expenses, List<Person> persons, BalanceService balanceService) {
        this.expenses = expenses;
        this.persons = persons;
        this.balanceService = balanceService;
    }

    @Override
    public int getRowCount() {
        return expenses.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Expense e = expenses.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> e.getTitle();
            case 1 -> e.getDate();
            case 2 -> e.getType();
            case 3 -> balanceService.findPersonById(persons, e.getPayerId()).map(Person::getName).orElse("?");
            case 4 -> UiUtils.money(e.getTotalAmount());
            case 5 -> UiUtils.money(e.getMeatAmount());
            case 6 -> UiUtils.money(e.getAlcoholAmount());
            case 7 -> e.isAdvancedMode() ? "Oui" : "Non";
            default -> "";
        };
    }

    public Expense getExpenseAt(int row) {
        return expenses.get(row);
    }

    public void refresh() {
        fireTableDataChanged();
    }
}
