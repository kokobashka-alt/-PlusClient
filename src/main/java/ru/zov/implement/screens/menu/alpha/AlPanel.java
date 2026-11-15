package ru.zov.implement.screens.menu.alpha;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.core.Main;

/** A category column of the Alpha-style click GUI: title, scrollable module list, scrollbar. */
final class AlPanel {
    static final float HEADER_H = 20.0F;
    private static final float BOTTOM_PAD = 4.0F;
    private static final float ROW_GAP = 0.75F;
    private static final int TITLE_PX = 10;

    private final ModuleCategory category;
    private final AlphaClickGui gui;
    private final List<AlModule> modules = new ArrayList<>();
    private final AlUi.Sm scrollAnim = new AlUi.Sm(0.0F);
    private final AlUi.Sm barAnim = new AlUi.Sm(0.0F);
    private float scroll;
    private float maxScroll;

    float x;
    float y;
    float w;
    float h;

    AlPanel(ModuleCategory category, AlphaClickGui gui) {
        this.category = category;
        this.gui = gui;
        Main.getInstance().getModuleRepository().modules().stream()
                .filter(module -> module.getCategory() == category)
                .sorted(Comparator.comparing((Module module) -> module.getVisibleName().toLowerCase()))
                .forEach(module -> modules.add(new AlModule(module)));
    }

    private static String titleOf(ModuleCategory category) {
        return switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Visuals";
            case PLAYER -> "Player";
            case MISC -> "Miscellaneous";
        };
    }

    boolean inside(double mx, double my) {
        return AlUi.hover(mx, my, x, y, w, h);
    }

    private boolean inView(double mx, double my) {
        return AlUi.hover(mx, my, x, y + HEADER_H, w, h - HEADER_H - BOTTOM_PAD);
    }

    private void clampScroll() {
        scroll = maxScroll > 0.0F ? AlUi.clamp(scroll, -maxScroll, 0.0F) : 0.0F;
    }

    void render(Matrix3x2fStack m, int mx, int my, float alpha) {
        AlUi.blurBox(m, x, y, w, h, 7.0F, AlUi.rgba(20, 23, 41, (int) (228 * alpha)));

        String title = titleOf(category);
        AlUi.textBold(m, title, x + w / 2.0F - AlUi.widthBold(title, TITLE_PX) / 2.0F, y + HEADER_H / 2.0F, TITLE_PX, AlUi.rgba(255, 255, 255, (int) (255 * alpha)));

        clampScroll();
        scrollAnim.to(scroll, 18.0F);

        float viewTop = y + HEADER_H;
        float viewH = h - HEADER_H - BOTTOM_PAD;
        boolean mouseInView = inView(mx, my);
        Main.getInstance().getScissorManager().push(m, x, viewTop, w, viewH);

        float offset = 0.0F;
        for (AlModule module : modules) {
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
            float barH = AlUi.clamp(track / Math.max(offset, track) * track, 10.0F, track);
            float barY = viewTop;
            if (maxScroll > 0.0F) {
                barY += (-scrollAnim.v / maxScroll) * (track - barH);
            }
            float barW = 2.0F * barAnim.v;
            AlUi.box(m, x + w - 3.0F - barW / 2.0F, barY, barW, barH, 1.0F, AlUi.rgba(255, 255, 255, (int) (80 * alpha * barAnim.v)));
        }
        Main.getInstance().getScissorManager().pop();
    }

    void mouseClicked(double mx, double my, int button) {
        if (!AlUi.hover(mx, my, x, y + HEADER_H, w, h - HEADER_H)) {
            return;
        }
        for (AlModule module : modules) {
            if (!gui.hiddenBySearch(module.module.getVisibleName())) {
                module.mouseClicked(mx, my, button);
            }
        }
    }

    void mouseReleased(double mx, double my, int button) {
        for (AlModule module : modules) {
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
        for (AlModule module : modules) {
            if (module.keyPressed(key)) {
                return true;
            }
        }
        return false;
    }

    boolean charTyped(char c) {
        for (AlModule module : modules) {
            if (module.charTyped(c)) {
                return true;
            }
        }
        return false;
    }

    boolean capturing() {
        for (AlModule module : modules) {
            if (module.capturing()) {
                return true;
            }
        }
        return false;
    }
}
