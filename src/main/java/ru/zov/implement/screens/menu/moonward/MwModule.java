package ru.zov.implement.screens.menu.moonward;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.core.Main;

/** One module row of a category panel: toggle, expandable settings, middle-click binding. */
final class MwModule {
    static final float ROW_H = 15.0F;
    private static final int NAME_PX = 8;

    final Module module;
    private final List<MwSetting> settings = new ArrayList<>();
    private final MwUi.Sm open = new MwUi.Sm(0.0F);
    private final MwUi.Sm hover = new MwUi.Sm(0.0F);
    private final MwUi.Sm enabled;
    private boolean opened;
    private boolean binding;

    float x;
    float y;
    float w;
    float h = ROW_H;

    MwModule(Module module) {
        this.module = module;
        this.enabled = new MwUi.Sm(module.isState() ? 1.0F : 0.0F);
        for (Setting setting : module.settings()) {
            MwSetting component = MwSetting.create(setting);
            if (component != null) {
                settings.add(component);
            }
        }
    }

    /** Advances the expand animation and returns the current row height. */
    float updateHeight() {
        open.to(opened ? 1.0F : 0.0F, 14.0F);
        float extra = 0.0F;
        for (MwSetting setting : settings) {
            setting.vis.to(setting.shown() ? 1.0F : 0.0F, 14.0F);
            extra += setting.height();
        }
        h = ROW_H + extra * open.v;
        return h;
    }

    void render(Matrix3x2fStack m, int mx, int my, float alpha, boolean mouseInView, float viewTop, float viewBottom) {
        boolean over = mouseInView && MwUi.hover(mx, my, x, y, w, ROW_H);
        hover.to(over ? 1.0F : 0.0F, 16.0F);
        enabled.to(module.isState() ? 1.0F : 0.0F, 12.0F);
        if (over) {
            MwUi.wantHand = true;
        }

        int textColor = MwUi.mix(MwUi.rgba(170, 170, 170, (int) (255 * alpha)), MwUi.rgba(255, 255, 255, (int) (255 * alpha)), enabled.v);
        float highlight = Math.max(hover.v, enabled.v);
        int outline = MwUi.rgba(255, 255, 255, (int) ((25 + 40 * highlight) * alpha));
        MwUi.box(m, x - 0.5F, y - 0.5F, w + 1.0F, h + 0.5F, 3.5F, outline);
        MwUi.blurBox(m, x, y, w, h - 0.5F, 3.0F, MwUi.rgba(44, 44, 44, (int) (140 * alpha)));

        if (binding) {
            String text = "Press a key...";
            MwUi.text(m, text, x + w / 2.0F - MwUi.width(text, NAME_PX) / 2.0F, y + ROW_H / 2.0F, NAME_PX, MwUi.rgba(255, 255, 255, (int) (255 * alpha)));
        } else {
            MwUi.text(m, module.getVisibleName(), x + 4.5F, y + ROW_H / 2.0F, NAME_PX, textColor);
            if (!settings.isEmpty()) {
                MwUi.text(m, "...", x + w - 4.5F - MwUi.width("...", NAME_PX), y + ROW_H / 2.0F - 1.0F, NAME_PX, textColor);
            }
        }

        if (open.v > 0.01F && !settings.isEmpty()) {
            float areaTop = Math.max(y + ROW_H, viewTop);
            float areaBottom = Math.min(y + h, viewBottom);
            float areaH = Math.max(0.0F, areaBottom - areaTop);
            MwUi.box(m, x + 1.0F, y + ROW_H, w - 2.0F, Math.max(0.0F, h - ROW_H), 0.0F, MwUi.rgba(0, 0, 0, (int) (30 * alpha * open.v)));
            if (areaH > 0.5F) {
                Main.getInstance().getScissorManager().push(m, x, areaTop, w, areaH);
                float cy = y + ROW_H;
                for (MwSetting setting : settings) {
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
        if (MwUi.hover(mx, my, x, y, w, ROW_H)) {
            if (button == 0) {
                module.switchState();
            } else if (button == 1 && !settings.isEmpty()) {
                opened = !opened;
            } else if (button == 2) {
                binding = !binding;
            }
        }
        if (opened) {
            for (MwSetting setting : settings) {
                if (setting.vis.v > 0.5F) {
                    setting.mouseClicked(mx, my, button);
                }
            }
        }
    }

    void mouseReleased(double mx, double my, int button) {
        for (MwSetting setting : settings) {
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
            for (MwSetting setting : settings) {
                if (setting.keyPressed(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean charTyped(char c) {
        if (opened) {
            for (MwSetting setting : settings) {
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
        for (MwSetting setting : settings) {
            if (setting.capturing()) {
                return true;
            }
        }
        return false;
    }
}
