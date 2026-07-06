package com.vacances.ravtricount;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class UiUtils {
    private UiUtils() {
    }

    public static BigDecimal parseMoney(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Le champ \"" + fieldName + "\" doit être un montant valide.");
        }
    }

    public static LocalDate parseDate(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Le champ \"" + fieldName + "\" est obligatoire. Format attendu : yyyy-MM-dd.");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Le champ \"" + fieldName + "\" doit respecter le format yyyy-MM-dd.");
        }
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return "0,00 €";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " €";
    }

    public static void showError(Component parent, Exception e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
