package ru.zov.implement.screens.menu.moonward;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ButtonSetting;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.TextSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.StringUtil;

/**
 * One row of a module's settings in the Moonward-style click GUI. Geometry (x, y, w) is assigned by the owning
 * module row every frame; {@code alpha} passed to render is the combined fade of panel, module and row.
 */
abstract class MwSetting {
    protected static final int WHITE = 0xFFFFFFFF;
    protected static final int LABEL_PX = 8;
    protected static final float ITEM_H = 11.0F;

    final Setting setting;
    float x;
    float y;
    float w;
    final MwUi.Sm vis = new MwUi.Sm(0.0F);

    protected MwSetting(Setting setting) {
        this.setting = setting;
    }

    abstract float fullHeight();

    float height() {
        return fullHeight() * vis.v;
    }

    boolean shown() {
        return setting.isVisible();
    }

    abstract void render(Matrix3x2fStack m, int mx, int my, float alpha);

    void mouseClicked(double mx, double my, int button) {
    }

    void mouseReleased(double mx, double my, int button) {
    }

    boolean keyPressed(int key) {
        return false;
    }

    boolean charTyped(char c) {
        return false;
    }

    /** True while the row waits for keyboard input (binding or typing), so Escape must not close the GUI. */
    boolean capturing() {
        return false;
    }

    protected int white(float alpha) {
        return MwUi.alphaMul(WHITE, alpha);
    }

    protected void label(Matrix3x2fStack m, String text, float cy, float alpha) {
        MwUi.text(m, text, x + 4.5F, cy, LABEL_PX, white(alpha));
    }

    static MwSetting create(Setting setting) {
        if (setting instanceof BooleanSetting s) {
            return new Bool(s);
        } else if (setting instanceof ValueSetting s) {
            return new Slider(s);
        } else if (setting instanceof SelectSetting s) {
            return new Mode(s);
        } else if (setting instanceof MultiSelectSetting s) {
            return new Multi(s);
        } else if (setting instanceof BindSetting s) {
            return new Bind(s);
        } else if (setting instanceof ColorSetting s) {
            return new Picker(s);
        } else if (setting instanceof TextSetting s) {
            return new Text(s);
        } else if (setting instanceof ButtonSetting s) {
            return new Button(s);
        } else if (setting instanceof GroupSetting s) {
            return new Group(s);
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ boolean

    static final class Bool extends MwSetting {
        private final BooleanSetting s;
        private final MwUi.Sm on;
        private boolean binding;

        Bool(BooleanSetting s) {
            super(s);
            this.s = s;
            this.on = new MwUi.Sm(s.isValue() ? 1.0F : 0.0F);
        }

        @Override
        float fullHeight() {
            return 13.0F;
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            on.to(s.isValue() ? 1.0F : 0.0F, 16.0F);
            if (MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                MwUi.wantHand = true;
            }
            label(m, binding ? "Binding..." : s.getName(), y + 6.5F, alpha);
            float bx = x + w - 10.5F;
            float by = y + 2.5F;
            MwUi.box(m, bx - 0.5F, by - 0.5F, 9.0F, 9.0F, 2.0F, MwUi.rgba(60, 60, 65, (int) (150 * alpha)));
            MwUi.box(m, bx, by, 8.0F, 8.0F, 2.0F, MwUi.rgba(20, 20, 25, (int) (255 * alpha)));
            if (on.v > 0.02F) {
                MwUi.box(m, bx + 1.5F, by + 1.5F, 5.0F, 5.0F, 1.0F, MwUi.alphaMul(MwUi.theme(), alpha * on.v));
            }
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (!MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                return;
            }
            if (button == 0) {
                s.setValue(!s.isValue());
            } else if (button == 2) {
                binding = !binding;
            }
        }

        @Override
        boolean keyPressed(int key) {
            if (!binding) {
                return false;
            }
            binding = false;
            if (key == GLFW.GLFW_KEY_DELETE) {
                s.setKey(-1);
            } else if (key != GLFW.GLFW_KEY_ESCAPE) {
                s.setKey(key);
            }
            return true;
        }

        @Override
        boolean capturing() {
            return binding;
        }
    }

    // ------------------------------------------------------------------------------------------------ slider

    static final class Slider extends MwSetting {
        private final ValueSetting s;
        private final MwUi.Sm shown;
        private boolean drag;

        Slider(ValueSetting s) {
            super(s);
            this.s = s;
            this.shown = new MwUi.Sm(fraction());
        }

        private float fraction() {
            float range = s.getMax() - s.getMin();
            return range <= 0.0F ? 0.0F : MwUi.clamp01((s.getValue() - s.getMin()) / range);
        }

        private float step() {
            if (s.isInteger()) {
                return 1.0F;
            }
            float range = s.getMax() - s.getMin();
            return range <= 3.0F ? 0.01F : (range <= 30.0F ? 0.1F : 0.5F);
        }

        private String format(float value) {
            if (s.isInteger()) {
                return Integer.toString(Math.round(value));
            }
            String text = String.format(Locale.ROOT, "%.2f", value);
            if (text.indexOf('.') >= 0) {
                text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return text;
        }

        @Override
        float fullHeight() {
            return 15.0F;
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            float trackW = w - 9.0F;
            if (drag) {
                MwUi.wantIBeam = true;
                float raw = (float) ((mx - (x + 3.5F)) / trackW) * (s.getMax() - s.getMin()) + s.getMin();
                float step = step();
                float snapped = Math.round(raw / step) * step;
                s.setValue(MwUi.clamp(snapped, s.getMin(), s.getMax()));
            } else if (MwUi.hover(mx, my, x + 3.0F, y + 6.0F, w - 6.0F, 10.0F)) {
                MwUi.wantHand = true;
            }
            shown.to(fraction(), 20.0F);

            label(m, s.getName(), y + 5.0F, alpha);
            String value = format(s.getValue());
            MwUi.text(m, value, x + w - 4.5F - MwUi.width(value, LABEL_PX), y + 5.0F, LABEL_PX, MwUi.rgba(200, 200, 200, (int) (255 * alpha)));

            MwUi.box(m, x + 3.0F, y + 9.5F, trackW + 1.0F, 4.0F, 1.0F, MwUi.rgba(60, 60, 65, (int) (100 * alpha)));
            MwUi.box(m, x + 3.5F, y + 10.0F, trackW, 3.0F, 1.0F, MwUi.rgba(20, 20, 25, (int) (255 * alpha)));
            float fill = MwUi.clamp(trackW * shown.v, 0.0F, trackW);
            MwUi.boxGradient(m, x + 3.5F, y + 10.0F, fill, 3.0F, 1.0F, MwUi.alphaMul(MwUi.themeSecondary(), alpha), MwUi.alphaMul(MwUi.theme(), alpha));
            MwUi.box(m, x + 3.5F + fill - 2.5F, y + 9.0F, 5.0F, 5.0F, 2.0F, white(alpha));
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (button == 0 && MwUi.hover(mx, my, x + 3.0F, y + 6.0F, w - 6.0F, 10.0F)) {
                drag = true;
            }
        }

        @Override
        void mouseReleased(double mx, double my, int button) {
            drag = false;
        }
    }

    // ------------------------------------------------------------------------------------------------ dropdowns

    private abstract static class Dropdown extends MwSetting {
        protected final MwUi.Sm open = new MwUi.Sm(0.0F);
        protected boolean opened;

        protected Dropdown(Setting setting) {
            super(setting);
        }

        protected abstract List<String> items();

        protected abstract String headerText();

        protected abstract boolean isChosen(String item);

        /** Returns true when the dropdown should close after the click. */
        protected abstract boolean choose(String item);

        @Override
        float fullHeight() {
            return 22.0F + open.v * items().size() * ITEM_H;
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            open.to(opened ? 1.0F : 0.0F, 14.0F);
            List<String> items = items();
            float btnY = y + 9.5F;
            float drop = open.v * items.size() * ITEM_H;

            label(m, setting.getName(), y + 5.0F, alpha);
            if (MwUi.hover(mx, my, x + 4.0F, btnY, w - 8.0F, ITEM_H)) {
                MwUi.wantHand = true;
            }
            MwUi.box(m, x + 3.5F, btnY - 0.5F, w - 7.0F, ITEM_H + drop + 1.0F, 2.5F, MwUi.rgba(60, 60, 65, (int) (150 * alpha)));
            MwUi.box(m, x + 4.0F, btnY, w - 8.0F, ITEM_H + drop, 2.5F, MwUi.rgba(20, 20, 25, (int) (255 * alpha)));

            String header = MwUi.fit(headerText(), LABEL_PX, w - 18.0F);
            MwUi.text(m, open.v > 0.5F ? "..." : header, x + 7.0F, btnY + ITEM_H / 2.0F, LABEL_PX, MwUi.rgba(200, 200, 200, (int) (255 * alpha)));

            if (open.v > 0.01F) {
                float cy = btnY + ITEM_H;
                for (String item : items) {
                    if (MwUi.hover(mx, my, x + 4.0F, cy, w - 8.0F, ITEM_H)) {
                        MwUi.wantHand = true;
                        MwUi.box(m, x + 4.5F, cy, w - 9.0F, ITEM_H, 0.0F, MwUi.rgba(255, 255, 255, (int) (15 * alpha)));
                    }
                    int base = isChosen(item) ? MwUi.theme() : MwUi.rgba(180, 180, 180, 255);
                    MwUi.text(m, item, x + 7.0F, cy + ITEM_H / 2.0F, LABEL_PX, MwUi.alphaMul(base, alpha * open.v));
                    cy += ITEM_H;
                }
            }
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (button != 0) {
                return;
            }
            float btnY = y + 9.5F;
            if (MwUi.hover(mx, my, x + 4.0F, btnY, w - 8.0F, ITEM_H)) {
                opened = !opened;
                return;
            }
            if (!opened) {
                return;
            }
            float cy = btnY + ITEM_H;
            for (String item : items()) {
                if (MwUi.hover(mx, my, x + 4.0F, cy, w - 8.0F, ITEM_H)) {
                    if (choose(item)) {
                        opened = false;
                    }
                    return;
                }
                cy += ITEM_H;
            }
        }
    }

    static final class Mode extends Dropdown {
        private final SelectSetting s;

        Mode(SelectSetting s) {
            super(s);
            this.s = s;
        }

        @Override
        protected List<String> items() {
            return s.getList();
        }

        @Override
        protected String headerText() {
            return s.getSelected();
        }

        @Override
        protected boolean isChosen(String item) {
            return item.equals(s.getSelected());
        }

        @Override
        protected boolean choose(String item) {
            s.setSelected(item);
            return true;
        }
    }

    static final class Multi extends Dropdown {
        private final MultiSelectSetting s;

        Multi(MultiSelectSetting s) {
            super(s);
            this.s = s;
        }

        @Override
        protected List<String> items() {
            return s.getList();
        }

        @Override
        protected String headerText() {
            List<String> selected = s.getSelected();
            return selected.isEmpty() ? "None" : String.join(", ", selected);
        }

        @Override
        protected boolean isChosen(String item) {
            return s.getSelected().contains(item);
        }

        @Override
        protected boolean choose(String item) {
            List<String> next = new ArrayList<>(s.getSelected());
            if (!next.remove(item)) {
                next.add(item);
            }
            s.setSelected(next);
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------ bind

    static final class Bind extends MwSetting {
        private final BindSetting s;
        private boolean binding;

        Bind(BindSetting s) {
            super(s);
            this.s = s;
        }

        @Override
        float fullHeight() {
            return 13.0F;
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            if (MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                MwUi.wantHand = true;
            }
            label(m, s.getName(), y + 6.5F, alpha);
            String key = binding ? "..." : StringUtil.getBindName(s.getKey());
            float kw = MwUi.width(key, LABEL_PX) + 8.0F;
            float kx = x + w - 4.5F - kw;
            MwUi.box(m, kx - 0.5F, y + 1.5F, kw + 1.0F, 11.0F, 2.5F, MwUi.rgba(60, 60, 65, (int) (150 * alpha)));
            MwUi.box(m, kx, y + 2.0F, kw, 10.0F, 2.5F, MwUi.rgba(20, 20, 25, (int) (255 * alpha)));
            MwUi.text(m, key, kx + 4.0F, y + 7.0F, LABEL_PX, binding ? MwUi.alphaMul(MwUi.theme(), alpha) : MwUi.rgba(200, 200, 200, (int) (255 * alpha)));
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (button == 0 && MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                binding = !binding;
            }
        }

        @Override
        boolean keyPressed(int key) {
            if (!binding) {
                return false;
            }
            binding = false;
            if (key == GLFW.GLFW_KEY_DELETE) {
                s.setKey(-1);
            } else if (key != GLFW.GLFW_KEY_ESCAPE) {
                s.setKey(key);
            }
            return true;
        }

        @Override
        boolean capturing() {
            return binding;
        }
    }

    // ------------------------------------------------------------------------------------------------ color

    static final class Picker extends MwSetting {
        private static final String[] NAMES = {"Hue", "Sat", "Bright", "Alpha"};
        private static final float ROW_H = 11.0F;

        private final ColorSetting s;
        private final MwUi.Sm open = new MwUi.Sm(0.0F);
        private boolean opened;
        private int dragRow = -1;

        Picker(ColorSetting s) {
            super(s);
            this.s = s;
        }

        @Override
        float fullHeight() {
            return 13.0F + open.v * NAMES.length * ROW_H;
        }

        private float trackX() {
            return x + 36.0F;
        }

        private float trackW() {
            return w - 36.0F - 8.0F;
        }

        private float get(int row) {
            return switch (row) {
                case 0 -> s.getHue();
                case 1 -> s.getSaturation();
                case 2 -> s.getBrightness();
                default -> s.getAlpha();
            };
        }

        private void set(int row, float value) {
            switch (row) {
                case 0 -> s.setHue(value);
                case 1 -> s.setSaturation(value);
                case 2 -> s.setBrightness(value);
                default -> s.setAlpha(value);
            }
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            open.to(opened ? 1.0F : 0.0F, 14.0F);
            if (dragRow >= 0) {
                MwUi.wantIBeam = true;
                set(dragRow, MwUi.clamp01((float) ((mx - trackX()) / trackW())));
            } else if (MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                MwUi.wantHand = true;
            }

            label(m, s.getName(), y + 6.5F, alpha);
            int color = MwUi.withAlpha(s.getColor(), 255);
            MwUi.box(m, x + w - 20.5F, y + 3.0F, 16.0F, 7.0F, 2.0F, MwUi.rgba(60, 60, 65, (int) (150 * alpha)));
            MwUi.box(m, x + w - 20.0F, y + 3.5F, 15.0F, 6.0F, 2.0F, MwUi.alphaMul(color, alpha));

            if (open.v > 0.02F) {
                float a = alpha * open.v;
                int pure = Color.HSBtoRGB(s.getHue(), 1.0F, 1.0F) | 0xFF000000;
                for (int i = 0; i < NAMES.length; i++) {
                    float ry = y + 13.0F + i * ROW_H * open.v;
                    MwUi.text(m, NAMES[i], x + 6.0F, ry + ROW_H / 2.0F, 7, MwUi.rgba(200, 200, 200, (int) (255 * a)));
                    float tx = trackX();
                    float tw = trackW();
                    MwUi.box(m, tx, ry + 4.0F, tw, 3.0F, 1.0F, MwUi.rgba(20, 20, 25, (int) (255 * a)));
                    float value = MwUi.clamp01(get(i));
                    int fill = i == 0 ? pure : color;
                    MwUi.box(m, tx, ry + 4.0F, Math.max(1.0F, tw * value), 3.0F, 1.0F, MwUi.alphaMul(fill, a));
                    MwUi.box(m, tx + tw * value - 2.0F, ry + 3.0F, 4.0F, 5.0F, 1.5F, white(a));
                }
            }
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                if (button == 0 || button == 1) {
                    opened = !opened;
                }
                return;
            }
            if (!opened || button != 0) {
                return;
            }
            for (int i = 0; i < NAMES.length; i++) {
                float ry = y + 13.0F + i * ROW_H;
                if (MwUi.hover(mx, my, trackX() - 3.0F, ry, trackW() + 6.0F, ROW_H)) {
                    dragRow = i;
                    return;
                }
            }
        }

        @Override
        void mouseReleased(double mx, double my, int button) {
            dragRow = -1;
        }
    }

    // ------------------------------------------------------------------------------------------------ text

    static final class Text extends MwSetting {
        private final TextSetting s;
        private boolean focused;

        Text(TextSetting s) {
            super(s);
            this.s = s;
        }

        @Override
        float fullHeight() {
            return 22.0F;
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            float by = y + 9.5F;
            if (MwUi.hover(mx, my, x + 4.0F, by, w - 8.0F, ITEM_H)) {
                MwUi.wantIBeam = true;
            }
            label(m, s.getName(), y + 5.0F, alpha);
            MwUi.box(m, x + 3.5F, by - 0.5F, w - 7.0F, ITEM_H + 1.0F, 2.5F, MwUi.rgba(60, 60, 65, (int) (150 * alpha)));
            MwUi.box(m, x + 4.0F, by, w - 8.0F, ITEM_H, 2.5F, MwUi.rgba(20, 20, 25, (int) (255 * alpha)));
            String value = s.getText() == null ? "" : s.getText();
            String caret = focused && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
            String shownText = MwUi.fit(value + caret, LABEL_PX, w - 18.0F);
            MwUi.text(m, shownText, x + 7.0F, by + ITEM_H / 2.0F, LABEL_PX, MwUi.rgba(200, 200, 200, (int) (255 * alpha)));
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            focused = button == 0 && MwUi.hover(mx, my, x + 4.0F, y + 9.5F, w - 8.0F, ITEM_H);
        }

        @Override
        boolean keyPressed(int key) {
            if (!focused) {
                return false;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String value = s.getText() == null ? "" : s.getText();
                if (!value.isEmpty()) {
                    s.setText(value.substring(0, value.length() - 1));
                }
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                focused = false;
            }
            return true;
        }

        @Override
        boolean charTyped(char c) {
            if (!focused || c < 32) {
                return false;
            }
            String value = s.getText() == null ? "" : s.getText();
            int max = s.getMax() > 0 ? s.getMax() : 64;
            if (value.length() < max) {
                s.setText(value + c);
            }
            return true;
        }

        @Override
        boolean capturing() {
            return focused;
        }
    }

    // ------------------------------------------------------------------------------------------------ button

    static final class Button extends MwSetting {
        private final ButtonSetting s;
        private final MwUi.Sm hover = new MwUi.Sm(0.0F);

        Button(ButtonSetting s) {
            super(s);
            this.s = s;
        }

        @Override
        float fullHeight() {
            return 16.0F;
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            boolean over = MwUi.hover(mx, my, x + 2.0F, y + 1.0F, w - 4.0F, 14.0F);
            hover.to(over ? 1.0F : 0.0F, 16.0F);
            if (over) {
                MwUi.wantHand = true;
            }
            MwUi.box(m, x + 1.5F, y + 0.5F, w - 3.0F, 15.0F, 3.5F, MwUi.rgba(255, 255, 255, (int) ((25 + 40 * hover.v) * alpha)));
            MwUi.box(m, x + 2.0F, y + 1.0F, w - 4.0F, 14.0F, 3.0F, MwUi.rgba(44, 44, 44, (int) (200 * alpha)));
            String name = s.getButtonName() == null || s.getButtonName().isEmpty() ? s.getName() : s.getButtonName();
            float tw = MwUi.width(name, LABEL_PX);
            MwUi.text(m, name, x + w / 2.0F - tw / 2.0F, y + 8.0F, LABEL_PX, white(alpha));
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (button == 0 && MwUi.hover(mx, my, x + 2.0F, y + 1.0F, w - 4.0F, 14.0F) && s.getRunnable() != null) {
                s.getRunnable().run();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ group

    static final class Group extends MwSetting {
        private final GroupSetting s;
        private final List<MwSetting> children = new ArrayList<>();
        private final MwUi.Sm open = new MwUi.Sm(0.0F);
        private final MwUi.Sm on;
        private boolean opened;

        Group(GroupSetting s) {
            super(s);
            this.s = s;
            this.on = new MwUi.Sm(s.isValue() ? 1.0F : 0.0F);
            for (Setting sub : s.getSubSettings()) {
                MwSetting child = MwSetting.create(sub);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        private float childrenHeight() {
            float total = 0.0F;
            for (MwSetting child : children) {
                total += child.height();
            }
            return total;
        }

        @Override
        float fullHeight() {
            return 13.0F + open.v * childrenHeight();
        }

        @Override
        void render(Matrix3x2fStack m, int mx, int my, float alpha) {
            open.to(opened ? 1.0F : 0.0F, 14.0F);
            on.to(s.isValue() ? 1.0F : 0.0F, 16.0F);
            if (MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                MwUi.wantHand = true;
            }
            label(m, s.getName(), y + 6.5F, alpha);
            float bx = x + w - 10.5F;
            float by = y + 2.5F;
            MwUi.box(m, bx - 0.5F, by - 0.5F, 9.0F, 9.0F, 2.0F, MwUi.rgba(60, 60, 65, (int) (150 * alpha)));
            MwUi.box(m, bx, by, 8.0F, 8.0F, 2.0F, MwUi.rgba(20, 20, 25, (int) (255 * alpha)));
            if (on.v > 0.02F) {
                MwUi.box(m, bx + 1.5F, by + 1.5F, 5.0F, 5.0F, 1.0F, MwUi.alphaMul(MwUi.theme(), alpha * on.v));
            }
            MwUi.text(m, "...", bx - 4.0F - MwUi.width("...", LABEL_PX), y + 5.0F, LABEL_PX, MwUi.rgba(170, 170, 170, (int) (255 * alpha)));

            float cy = y + 13.0F;
            for (MwSetting child : children) {
                child.vis.to(child.shown() ? 1.0F : 0.0F, 14.0F);
                if (child.vis.v <= 0.01F) {
                    continue;
                }
                child.x = x + 4.0F;
                child.y = cy;
                child.w = w - 8.0F;
                if (open.v > 0.05F) {
                    child.render(m, mx, my, alpha * open.v * child.vis.v);
                }
                cy += child.height() * open.v;
            }
        }

        @Override
        void mouseClicked(double mx, double my, int button) {
            if (MwUi.hover(mx, my, x + 3.0F, y, w - 6.0F, 13.0F)) {
                if (button == 0) {
                    s.setValue(!s.isValue());
                } else if (button == 1) {
                    opened = !opened;
                }
                return;
            }
            if (opened) {
                for (MwSetting child : children) {
                    if (child.vis.v > 0.5F) {
                        child.mouseClicked(mx, my, button);
                    }
                }
            }
        }

        @Override
        void mouseReleased(double mx, double my, int button) {
            for (MwSetting child : children) {
                child.mouseReleased(mx, my, button);
            }
        }

        @Override
        boolean keyPressed(int key) {
            for (MwSetting child : children) {
                if (child.keyPressed(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        boolean charTyped(char c) {
            for (MwSetting child : children) {
                if (child.charTyped(c)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        boolean capturing() {
            for (MwSetting child : children) {
                if (child.capturing()) {
                    return true;
                }
            }
            return false;
        }
    }
}
