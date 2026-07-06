package com.vacances.ravtricount;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Person> persons = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();

    public List<Person> getPersons() {
        return persons;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }
}
