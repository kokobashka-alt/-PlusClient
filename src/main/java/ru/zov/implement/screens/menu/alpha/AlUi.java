package ru.zov.implement.screens.menu.alpha;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;

/** Drawing, color, text and smoothing helpers shared by the Alpha-style click GUI. */
final class AlUi implements QuickImports {
    static float dt = 0.016F;
    static boolean wantHand;
    static boolean wantIBeam;

    private static long lastNs;
    private static final Map<Integer, Float> TEXT_BOX_HEIGHT = new HashMap<>();
    private static long handCursor;
    private static long ibeamCursor;
    private static int appliedCursor;

    private AlUi() {
    }

    /** Frame-time exponential smoothing toward a target. */
    static final class Sm {
        float v;

        Sm(float start) {
            this.v = start;
        }

        float to(float target, float speed) {
            v += (target - v) * (1.0F - (float) Math.exp(-speed * dt));
            if (Math.abs(target - v) < 0.002F) {
                v = target;
            }
            return v;
        }
    }

    static void beginFrame() {
        long now = System.nanoTime();
        dt = lastNs == 0L ? 0.016F : Math.min(0.05F, (now - lastNs) / 1.0E9F);
        lastNs = now;
        wantHand = false;
        wantIBeam = false;
    }

    /** Applies the cursor shape requested during this frame. */
    static void applyCursor() {
        long window = mc.getWindow().getHandle();
        int wanted = wantHand ? 1 : (wantIBeam ? 2 : 0);
        if (wanted == appliedCursor) {
            return;
        }
        appliedCursor = wanted;
        if (wanted == 1) {
            if (handCursor == 0L) {
                handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
            }
            GLFW.glfwSetCursor(window, handCursor);
        } else if (wanted == 2) {
            if (ibeamCursor == 0L) {
                ibeamCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
            }
            GLFW.glfwSetCursor(window, ibeamCursor);
        } else {
            GLFW.glfwSetCursor(window, 0L);
        }
    }

    static void resetCursor() {
        wantHand = false;
        wantIBeam = false;
        applyCursor();
    }

    static boolean hover(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    static float clamp01(float v) {
        return clamp(v, 0.0F, 1.0F);
    }

    private static int c255(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    static int rgba(int r, int g, int b, int a) {
        return c255(a) << 24 | c255(r) << 16 | c255(g) << 8 | c255(b);
    }

    static int alphaMul(int color, float mul) {
        int a = Math.round(((color >>> 24) & 255) * clamp01(mul));
        return a << 24 | (color & 0xFFFFFF);
    }

    static int withAlpha(int color, int alpha) {
        return c255(alpha) << 24 | (color & 0xFFFFFF);
    }

    static int mix(int from, int to, float t) {
        t = clamp01(t);
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return rgba(r, g, b, a);
    }

    /** Accent of the Alpha look (blue-violet), fixed instead of following the client theme. */
    static int theme() {
        return rgba(118, 104, 255, 255);
    }

    static int themeSecondary() {
        return rgba(80, 94, 232, 255);
    }

    static void box(Matrix3x2fStack m, float x, float y, float w, float h, float round, int color) {
        if ((color >>> 24) == 0 || w <= 0.0F || h <= 0.0F) {
            return;
        }
        rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).color(color).build());
    }

    /** Horizontal two-color gradient (corner order: top-left, bottom-left, bottom-right, top-right). */
    static void boxGradient(Matrix3x2fStack m, float x, float y, float w, float h, float round, int left, int right) {
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).color(left, left, right, right).build());
    }

    static void blurBox(Matrix3x2fStack m, float x, float y, float w, float h, float round, int color) {
        if ((color >>> 24) == 0 || w <= 0.0F || h <= 0.0F) {
            return;
        }
        blur.render(ShapeProperties.create(m, x, y, w, h).round(round).softness(1.0F).color(color).build());
    }

    static FontRenderer font(int px) {
        return Fonts.getSize(px, Fonts.Type.DEFAULT);
    }

    /**
     * Height of the glyph box in drawn pixels, measured on a fixed reference string so that every string of the same
     * size is placed on the same line. FontRenderer works in a 2x atlas (width and height are halved when drawn) and
     * shifts the glyph box up by 3 px inside drawString.
     */
    private static float textBoxHeight(int px) {
        return TEXT_BOX_HEIGHT.computeIfAbsent(px, k -> font(k).getStringHeight("Ay") / 2.0F);
    }

    static float width(String text, int px) {
        return font(px).getStringWidth(text);
    }

    private static final Map<Integer, Float> BOLD_BOX_HEIGHT = new HashMap<>();

    private static float boldBoxHeight(int px) {
        return BOLD_BOX_HEIGHT.computeIfAbsent(px, k -> Fonts.getSize(k, Fonts.Type.BOLD).getStringHeight("Ay") / 2.0F);
    }

    static float widthBold(String text, int px) {
        return Fonts.getSize(px, Fonts.Type.BOLD).getStringWidth(text);
    }

    /** Same placement rule as {@link #text}, with the bold face. */
    static float textBold(Matrix3x2fStack m, String text, float x, float centerY, int px, int color) {
        FontRenderer font = Fonts.getSize(px, Fonts.Type.BOLD);
        font.drawString(m, text, x, centerY - boldBoxHeight(px) / 2.0F + 3.0F, color);
        return font.getStringWidth(text);
    }

    /** Draws text with its glyph box vertically centered on {@code centerY}; returns the text width. */
    static float text(Matrix3x2fStack m, String text, float x, float centerY, int px, int color) {
        FontRenderer font = font(px);
        font.drawString(m, text, x, centerY - textBoxHeight(px) / 2.0F + 3.0F, color);
        return font.getStringWidth(text);
    }

    static String fit(String text, int px, float maxWidth) {
        String out = text;
        while (out.length() > 1 && width(out, px) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
