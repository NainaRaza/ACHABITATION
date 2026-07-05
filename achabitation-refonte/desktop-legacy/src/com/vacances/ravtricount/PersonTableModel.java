package com.vacances.ravtricount;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class PersonTableModel extends AbstractTableModel {
    private final List<Person> persons;
    private final String[] columns = {
            "Nom",
            "Reste à vivre",
            "Mode RAV",
            "Poids",
            "Végétarien",
            "Sans alcool",
            "Présence début",
            "Présence fin",
            "Actif"
    };

    public PersonTableModel(List<Person> persons) {
        this.persons = persons;
    }

    @Override
    public int getRowCount() {
        return persons.size();
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
        Person p = persons.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> p.getName();
            case 1 -> UiUtils.money(p.getLivingRest());
            case 2 -> p.isAdvancedLivingRest() ? "Avancé" : "Simple";
            case 3 -> p.isAverageWeight() ? "Moyenne" : "RAV";
            case 4 -> p.isVegetarian() ? "Oui" : "Non";
            case 5 -> p.isNoAlcohol() ? "Oui" : "Non";
            case 6 -> p.getPresenceStart();
            case 7 -> p.getPresenceEnd();
            case 8 -> p.isActive() ? "Oui" : "Non";
            default -> "";
        };
    }

    public Person getPersonAt(int row) {
        return persons.get(row);
    }

    public void refresh() {
        fireTableDataChanged();
    }
}
