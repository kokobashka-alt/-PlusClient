package ru.zov.implement.screens.menu.moonward;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.core.Main;

/** A category column of the Moonward-style click GUI: title, scrollable module list, scrollbar. */
final class MwPanel {
    static final float HEADER_H = 20.0F;
    private static final float BOTTOM_PAD = 4.0F;
    private static final float ROW_GAP = 0.75F;
    private static final int TITLE_PX = 9;

    private final ModuleCategory category;
    private final MoonwardClickGui gui;
    private final List<MwModule> modules = new ArrayList<>();
    private final MwUi.Sm scrollAnim = new MwUi.Sm(0.0F);
    private final MwUi.Sm barAnim = new MwUi.Sm(0.0F);
    private float scroll;
    private float maxScroll;

    float x;
    float y;
    float w;
    float h;

    MwPanel(ModuleCategory category, MoonwardClickGui gui) {
        this.category = category;
        this.gui = gui;
        Main.getInstance().getModuleRepository().modules().stream()
                .filter(module -> module.getCategory() == category)
                .sorted(Comparator.comparing((Module module) -> module.getVisibleName().toLowerCase()))
                .forEach(module -> modules.add(new MwModule(module)));
    }

    boolean inside(double mx, double my) {
        return MwUi.hover(mx, my, x, y, w, h);
    }

    private boolean inView(double mx, double my) {
        return MwUi.hover(mx, my, x, y + HEADER_H, w, h - HEADER_H - BOTTOM_PAD);
    }

    private void clampScroll() {
        scroll = maxScroll > 0.0F ? MwUi.clamp(scroll, -maxScroll, 0.0F) : 0.0F;
    }

    void render(Matrix3x2fStack m, int mx, int my, float alpha) {
        MwUi.blurBox(m, x, y, w, h, 8.0F, MwUi.rgba(18, 18, 22, (int) (175 * alpha)));

        String title = category.getReadableName();
        MwUi.text(m, title, x + w / 2.0F - MwUi.width(title, TITLE_PX) / 2.0F, y + HEADER_H / 2.0F, TITLE_PX, MwUi.rgba(255, 255, 255, (int) (255 * alpha)));
        MwUi.boxGradient(m, x + 8.0F, y + HEADER_H - 1.0F, w - 16.0F, 1.0F, 0.0F,
                MwUi.alphaMul(MwUi.themeSecondary(), alpha * 0.55F), MwUi.alphaMul(MwUi.theme(), alpha * 0.55F));

        clampScroll();
        scrollAnim.to(scroll, 18.0F);

        float viewTop = y + HEADER_H;
        float viewH = h - HEADER_H - BOTTOM_PAD;
        boolean mouseInView = inView(mx, my);
        Main.getInstance().getScissorManager().push(m, x, viewTop, w, viewH);

        float offset = 0.0F;
        for (MwModule module : modules) {
            if (gui.hiddenBySearch(module.module.getVisibleName())) {
                continue;
            }
            module.x = x + 5.2F;
            module.y = viewTop + offset + scrollAnim.v;
            module.w = w - 10.4F;
            float rowH = module.updateHeight();
            if (module.y + rowH >= viewTop - 2.0F && module.y <= viewTop + viewH + 2.0F) {
                module.render(m, mx, my, alpha, mouseInView, viewTop, viewTop + viewH);
            }
            offset += rowH + ROW_GAP;
        }
        maxScroll = Math.max(0.0F, offset - (viewH - 4.0F));
        barAnim.to(maxScroll > 0.0F ? 1.0F : 0.0F, 14.0F);

        if (barAnim.v > 0.01F) {
            float track = viewH - 4.0F;
            float barH = MwUi.clamp(track / Math.max(offset, track) * track, 10.0F, track);
            float barY = viewTop;
            if (maxScroll > 0.0F) {
                barY += (-scrollAnim.v / maxScroll) * (track - barH);
            }
            float barW = 2.0F * barAnim.v;
            MwUi.box(m, x + w - 3.0F - barW / 2.0F, barY, barW, barH, 1.0F, MwUi.rgba(255, 255, 255, (int) (80 * alpha * barAnim.v)));
        }
        Main.getInstance().getScissorManager().pop();
    }

    void mouseClicked(double mx, double my, int button) {
        if (!MwUi.hover(mx, my, x, y + HEADER_H, w, h - HEADER_H)) {
            return;
        }
        for (MwModule module : modules) {
            if (!gui.hiddenBySearch(module.module.getVisibleName())) {
                module.mouseClicked(mx, my, button);
            }
        }
    }

    void mouseReleased(double mx, double my, int button) {
        for (MwModule module : modules) {
            module.mouseReleased(mx, my, button);
        }
    }

    void mouseScrolled(double mx, double my, double vertical) {
        if (inside(mx, my)) {
            scroll += (float) (vertical * 30.0);
            clampScroll();
        }
    }

    boolean keyPressed(int key) {
        for (MwModule module : modules) {
            if (module.keyPressed(key)) {
                return true;
            }
        }
        return false;
    }

    boolean charTyped(char c) {
        for (MwModule module : modules) {
            if (module.charTyped(c)) {
                return true;
            }
        }
        return false;
    }

    boolean capturing() {
        for (MwModule module : modules) {
            if (module.capturing()) {
                return true;
            }
        }
        return false;
    }
}
