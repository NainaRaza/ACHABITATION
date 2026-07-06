package fr.achabitation.domain.util;

import java.text.Normalizer;
import java.util.Locale;

public final class ConstraintNameUtils {
    private ConstraintNameUtils() {}

    public static String canonicalDisplayName(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public static String key(String input) {
        String display = canonicalDisplayName(input);
        if (display.isBlank()) {
            return "";
        }
        return normalizedRawKey(display);
    }

    private static String normalizedRawKey(String input) {
        return Normalizer.normalize(input == null ? "" : input.trim().replaceAll("\\s+", " "), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
