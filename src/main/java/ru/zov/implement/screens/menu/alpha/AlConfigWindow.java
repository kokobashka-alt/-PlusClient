package ru.zov.implement.screens.menu.alpha;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.core.Main;

/**
 * "Config Manager" window in the lower right corner: a name field with a create button and the list of saved
 * configs. Left click on a config loads it, right click overwrites it with the current settings.
 */
final class AlConfigWindow {
    private static final float W = 104.0F;
    private static final float H = 98.0F;
    private static final float MARGIN = 10.0F;
    private static final float ROW = 12.0F;
    private static final int VISIBLE_ROWS = 4;
    private static final int NAME_MAX = 16;
    private static final int PX = 8;
    private static final long REFRESH_MS = 2000L;
    private static final long STATUS_MS = 2500L;

    private final List<String> configs = new ArrayList<>();
    private String input = "";
    private boolean focused;
    private int first;
    private long lastRefresh;
    private String status = "";
    private long statusAt;

    private float x;
    private float y;

    private float fieldX() {
        return x + 5.0F;
    }

    private float fieldY() {
        return y + 19.0F;
    }

    private float fieldW() {
        return W - 10.0F - 38.0F;
    }

    private float buttonX() {
        return x + W - 5.0F - 34.0F;
    }

    private float listY() {
        return y + 35.0F;
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < REFRESH_MS) {
            return;
        }
        lastRefresh = now;
        configs.clear();
        configs.addAll(AlConfigs.list());
        first = Math.max(0, Math.min(first, Math.max(0, configs.size() - VISIBLE_ROWS)));
    }

    private void say(String text) {
        status = text;
        statusAt = System.currentTimeMillis();
    }

    void reset() {
        focused = false;
    }

    boolean capturing() {
        return focused;
    }

    void render(Matrix3x2fStack m, int mx, int my, float width, float height, float alpha) {
        x = width - W - MARGIN;
        y = height - H - MARGIN;
        refresh();

        AlUi.blurBox(m, x, y, W, H, 7.0F, AlUi.rgba(20, 23, 41, (int) (228 * alpha)));
        String title = "Config Manager";
        AlUi.textBold(m, title, x + W / 2.0F - AlUi.widthBold(title, 9) / 2.0F, y + 10.0F, 9, AlUi.rgba(255, 255, 255, (int) (255 * alpha)));

        // name field
        if (AlUi.hover(mx, my, fieldX(), fieldY(), fieldW(), 11.0F)) {
            AlUi.wantIBeam = true;
        }
        AlUi.box(m, fieldX() - 0.5F, fieldY() - 0.5F, fieldW() + 1.0F, 12.0F, 3.0F, AlUi.rgba(70, 76, 120, (int) (110 * alpha)));
        AlUi.box(m, fieldX(), fieldY(), fieldW(), 11.0F, 3.0F, AlUi.rgba(14, 17, 32, (int) (240 * alpha)));
        boolean placeholder = input.isEmpty() && !focused;
        String caret = focused && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
        String shown = placeholder ? "Название" : input + caret;
        int shownColor = placeholder ? AlUi.rgba(150, 155, 175, (int) (255 * alpha)) : AlUi.rgba(255, 255, 255, (int) (255 * alpha));
        AlUi.text(m, shown, fieldX() + 4.0F, fieldY() + 5.5F, PX, shownColor);

        // create button
        boolean overButton = AlUi.hover(mx, my, buttonX(), fieldY(), 34.0F, 11.0F);
        if (overButton) {
            AlUi.wantHand = true;
        }
        AlUi.box(m, buttonX(), fieldY(), 34.0F, 11.0F, 3.0F, AlUi.alphaMul(AlUi.theme(), alpha * (overButton ? 1.0F : 0.8F)));
        String create = "Создать";
        AlUi.text(m, create, buttonX() + 17.0F - AlUi.width(create, 7) / 2.0F, fieldY() + 5.5F, 7, AlUi.rgba(255, 255, 255, (int) (255 * alpha)));

        // config list
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = first + i;
            if (index >= configs.size()) {
                break;
            }
            float ry = listY() + i * ROW;
            boolean over = AlUi.hover(mx, my, x + 5.0F, ry, W - 10.0F, ROW - 1.0F);
            if (over) {
                AlUi.wantHand = true;
                AlUi.box(m, x + 5.0F, ry, W - 10.0F, ROW - 1.0F, 3.0F, AlUi.rgba(46, 52, 100, (int) (170 * alpha)));
            }
            AlUi.text(m, AlUi.fit(configs.get(index), PX, W - 20.0F), x + 9.0F, ry + (ROW - 1.0F) / 2.0F, PX, AlUi.rgba(220, 224, 240, (int) (255 * alpha)));
        }
        if (configs.isEmpty()) {
            AlUi.text(m, "Нет конфигов", x + 9.0F, listY() + 6.0F, 7, AlUi.rgba(130, 136, 160, (int) (255 * alpha)));
        }

        boolean fresh = System.currentTimeMillis() - statusAt < STATUS_MS && !status.isEmpty();
        String bottom = fresh ? status : "ЛКМ: загрузить  ПКМ: сохранить";
        int bottomColor = fresh ? AlUi.rgba(190, 196, 255, (int) (255 * alpha)) : AlUi.rgba(120, 126, 150, (int) (255 * alpha));
        AlUi.text(m, AlUi.fit(bottom, 6, W - 10.0F), x + 6.0F, y + H - 7.0F, 6, bottomColor);
    }

    private boolean inside(double mx, double my) {
        return AlUi.hover(mx, my, x, y, W, H);
    }

    private void create() {
        String name = input.trim();
        if (name.isEmpty()) {
            say("Введите название");
            return;
        }
        if (AlConfigs.save(name)) {
            say("Сохранено: " + name);
            input = "";
            lastRefresh = 0L;
            refresh();
        } else {
            say("Ошибка сохранения");
        }
    }

    /** Returns true when the click landed inside the window. */
    boolean mouseClicked(double mx, double my, int button) {
        if (!inside(mx, my)) {
            focused = false;
            return false;
        }
        focused = AlUi.hover(mx, my, fieldX(), fieldY(), fieldW(), 11.0F);
        if (button == 0 && AlUi.hover(mx, my, buttonX(), fieldY(), 34.0F, 11.0F)) {
            create();
            return true;
        }
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = first + i;
            if (index >= configs.size()) {
                break;
            }
            if (AlUi.hover(mx, my, x + 5.0F, listY() + i * ROW, W - 10.0F, ROW - 1.0F)) {
                String name = configs.get(index);
                if (button == 0) {
                    say(AlConfigs.load(name) ? "Загружено: " + name : "Ошибка загрузки");
                } else if (button == 1) {
                    say(AlConfigs.save(name) ? "Перезаписано: " + name : "Ошибка сохранения");
                }
                return true;
            }
        }
        return true;
    }

    boolean mouseScrolled(double mx, double my, double vertical) {
        if (!inside(mx, my)) {
            return false;
        }
        first = (int) Math.max(0, Math.min(Math.max(0, configs.size() - VISIBLE_ROWS), first - Math.signum(vertical)));
        return true;
    }

    void keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()) {
            input = input.substring(0, input.length() - 1);
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            create();
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
        }
    }

    void charTyped(char c) {
        if (focused && input.length() < NAME_MAX && (Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
            input += c;
        }
    }
}
