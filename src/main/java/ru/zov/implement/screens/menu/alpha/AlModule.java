package ru.zov.implement.screens.menu.alpha;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.core.Main;

/** One module row of a category panel: toggle, expandable settings, middle-click binding. */
final class AlModule {
    static final float ROW_H = 15.0F;
    private static final int NAME_PX = 8;

    final Module module;
    private final List<AlSetting> settings = new ArrayList<>();
    private final AlUi.Sm open = new AlUi.Sm(0.0F);
    private final AlUi.Sm hover = new AlUi.Sm(0.0F);
    private final AlUi.Sm enabled;
    private boolean opened;
    private boolean binding;

    float x;
    float y;
    float w;
    float h = ROW_H;

    AlModule(Module module) {
        this.module = module;
        this.enabled = new AlUi.Sm(module.isState() ? 1.0F : 0.0F);
        for (Setting setting : module.settings()) {
            AlSetting component = AlSetting.create(setting);
            if (component != null) {
                settings.add(component);
            }
        }
    }

    /** Advances the expand animation and returns the current row height. */
    float updateHeight() {
        open.to(opened ? 1.0F : 0.0F, 14.0F);
        float extra = 0.0F;
        for (AlSetting setting : settings) {
            setting.vis.to(setting.shown() ? 1.0F : 0.0F, 14.0F);
            extra += setting.height();
        }
        h = ROW_H + extra * open.v;
        return h;
    }

    void render(Matrix3x2fStack m, int mx, int my, float alpha, boolean mouseInView, float viewTop, float viewBottom) {
        boolean over = mouseInView && AlUi.hover(mx, my, x, y, w, ROW_H);
        hover.to(over ? 1.0F : 0.0F, 16.0F);
        enabled.to(module.isState() ? 1.0F : 0.0F, 12.0F);
        if (over) {
            AlUi.wantHand = true;
        }

        int textColor = AlUi.mix(AlUi.rgba(178, 183, 204, (int) (255 * alpha)), AlUi.rgba(255, 255, 255, (int) (255 * alpha)), enabled.v);
        AlUi.box(m, x, y, w, h - 0.5F, 3.5F, AlUi.rgba(255, 255, 255, (int) (12 * alpha * hover.v)));
        AlUi.box(m, x, y, w, h - 0.5F, 3.5F, AlUi.rgba(46, 52, 100, (int) (175 * alpha * enabled.v)));

        if (binding) {
            String text = "Press a key...";
            AlUi.text(m, text, x + w / 2.0F - AlUi.width(text, NAME_PX) / 2.0F, y + ROW_H / 2.0F, NAME_PX, AlUi.rgba(255, 255, 255, (int) (255 * alpha)));
        } else {
            AlUi.text(m, module.getVisibleName(), x + 4.5F, y + ROW_H / 2.0F, NAME_PX, textColor);
            if (!settings.isEmpty()) {
                AlUi.text(m, "...", x + w - 4.5F - AlUi.width("...", NAME_PX), y + ROW_H / 2.0F - 1.0F, NAME_PX, textColor);
            }
        }

        if (open.v > 0.01F && !settings.isEmpty()) {
            float areaTop = Math.max(y + ROW_H, viewTop);
            float areaBottom = Math.min(y + h, viewBottom);
            float areaH = Math.max(0.0F, areaBottom - areaTop);
            AlUi.box(m, x + 1.0F, y + ROW_H, w - 2.0F, Math.max(0.0F, h - ROW_H), 0.0F, AlUi.rgba(8, 10, 24, (int) (110 * alpha * open.v)));
            if (areaH > 0.5F) {
                Main.getInstance().getScissorManager().push(m, x, areaTop, w, areaH);
                float cy = y + ROW_H;
                for (AlSetting setting : settings) {
                    if (setting.vis.v <= 0.01F) {
                        continue;
                    }
                    setting.x = x + 2.0F;
                    setting.y = cy;
                    setting.w = w - 4.0F;
                    setting.render(m, mx, my, alpha * Math.min(1.0F, open.v * 1.5F) * setting.vis.v);
                    cy += setting.height() * open.v;
                }
                Main.getInstance().getScissorManager().pop();
            }
        }
    }

    void mouseClicked(double mx, double my, int button) {
        if (AlUi.hover(mx, my, x, y, w, ROW_H)) {
            if (button == 0) {
                module.switchState();
            } else if (button == 1 && !settings.isEmpty()) {
                opened = !opened;
            } else if (button == 2) {
                binding = !binding;
            }
        }
        if (opened) {
            for (AlSetting setting : settings) {
                if (setting.vis.v > 0.5F) {
                    setting.mouseClicked(mx, my, button);
                }
            }
        }
    }

    void mouseReleased(double mx, double my, int button) {
        for (AlSetting setting : settings) {
            setting.mouseReleased(mx, my, button);
        }
    }

    /** Returns true when the key was consumed. */
    boolean keyPressed(int key) {
        if (binding) {
            binding = false;
            module.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE ? -1 : key);
            return true;
        }
        if (opened) {
            for (AlSetting setting : settings) {
                if (setting.keyPressed(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean charTyped(char c) {
        if (opened) {
            for (AlSetting setting : settings) {
                if (setting.charTyped(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean capturing() {
        if (binding) {
            return true;
        }
        for (AlSetting setting : settings) {
            if (setting.capturing()) {
                return true;
            }
        }
        return false;
    }
}
