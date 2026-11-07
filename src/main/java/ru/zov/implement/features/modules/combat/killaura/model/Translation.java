package ru.zov.implement.features.modules.combat.killaura.model;

/**
 * Lightweight translation value used by the transplanted combat settings.
 * The donor's combat code only needs a stable display string here.
 */
public interface Translation {
    String effective();

    default String original() {
        return effective();
    }

    default String firstLetterUppercase() {
        String value = effective();
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    static Translation clearText(String value) {
        return () -> value;
    }

    static Translation unformatted(String value) {
        return () -> value;
    }
}
