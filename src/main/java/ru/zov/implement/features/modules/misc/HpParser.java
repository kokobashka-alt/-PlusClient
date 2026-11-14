package ru.zov.implement.features.modules.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HpParser {
    private static final Pattern NUMBER = Pattern.compile("(?<!\\d)(\\d+(?:[.,]\\d+)?)(?!\\d)");

    private HpParser() {
    }

    static Float parse(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String text = strip(raw).replace("❤", " ").replace("♥", " ")
                .replace("HP", " ").replace("hp", " ").replace("хп", " ").replace("Хп", " ").trim();
        Matcher m = NUMBER.matcher(text);
        Float last = null;
        while (m.find()) {
            try {
                last = Float.parseFloat(m.group(1).replace(",", "."));
            } catch (NumberFormatException ignored) {
            }
        }
        return last;
    }

    private static String strip(String s) {
        return s.replaceAll("§[0-9a-fk-orx]", "").replaceAll("(?i)§x(§[0-9a-f]){6}", "").trim();
    }
}
