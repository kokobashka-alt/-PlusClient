package ru.zov.implement.features.draggables;

import ru.zov.common.util.color.ColorUtil;

/** Shared look of the Pouch panels: the tinted flat panel of the AspectDLC HUD and small text helpers. */
final class PouchStyle {
    static final float HEADER_HEIGHT = 16.0F;
    static final float ROW_HEIGHT = 12.0F;
    static final float PANEL_ROUND = 3.0F;
    static final float SIDE_PADDING = 5.0F;
    static final float ICON_SIZE = 8.0F;

    // FontRenderer.drawGlyphs lifts every string by 3 GUI units; the cap-height centre of a glyph cell sits
    // about 0.35 * size - 0.35 units below the cell top (measured for sizes 11 to 26).
    private static final float GLYPH_LIFT = 3.0F;

    private PouchStyle() {
    }

    static int panelColor(int accent) {
        int r = Math.min(12 + (int) (((accent >> 16) & 255) * 0.08F), 255);
        int g = Math.min(12 + (int) (((accent >> 8) & 255) * 0.08F), 255);
        int b = Math.min(14 + (int) ((accent & 255) * 0.08F), 255);
        return ColorUtil.getColor(r, g, b, 210.0F / 255.0F);
    }

    /** Y to pass to FontRenderer.drawString so that the middle of capital letters lands on centerY. */
    static float textY(float centerY, float fontSize) {
        return centerY + GLYPH_LIFT - (0.35F * fontSize - 0.35F);
    }

    static String roman(int value) {
        if (value <= 0 || value >= 40) {
            return Integer.toString(value);
        }

        String[] tens = {"", "X", "XX", "XXX"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return tens[value / 10] + ones[value % 10];
    }

    /** Minutes and seconds, "m:ss"; minutes are not wrapped. */
    static String clock(long totalSeconds) {
        long s = Math.max(0L, totalSeconds);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }
}
