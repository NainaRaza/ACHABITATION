package com.vacances.ravtricount;

public enum ExpenseType {
    NORMAL("Dépense normale"),
    GLOBAL("Dépense globale");

    private final String label;

    ExpenseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
