package ru.zov.implement.screens.menu.alpha;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.api.system.sound.SoundManager;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.MenuScreen;

/**
 * Click GUI ported from the Alpha client: one translucent panel per module category, module rows that expand into
 * their settings, a search field under the panels. It extends MenuScreen so that every place that checks for the menu
 * screen (open/close animation, sound, input handling in the mixins) keeps working, and replaces all of its drawing
 * and input handling. Not ported: the theme manager window (it depends on Alpha's own theme system).
 */
public class AlphaClickGui extends MenuScreen {
    public static final AlphaClickGui INSTANCE = new AlphaClickGui();

    private static final float PANEL_W = 105.0F;
    private static final float PANEL_GAP = 4.0F;
    private static final float PANEL_H = 240.0F;
    private static final float SEARCH_W = 105.0F;
    private static final float SEARCH_H = 17.0F;
    private static final int SEARCH_PX = 9;
    private static final int SEARCH_MAX = 15;

    private final List<AlPanel> panels = new ArrayList<>();
    private final AlConfigWindow config = new AlConfigWindow();
    private String search = "";
    private boolean searchFocused;
    private float searchX;
    private float searchY;
    private float pivotX;
    private float pivotY;
    private float totalScale = 1.0F;

    @Override
    public void initialize() {
        // MenuScreen builds its own components here; this screen draws everything itself.
    }

    private void ensurePanels() {
        if (!panels.isEmpty()) {
            return;
        }
        for (ModuleCategory category : new ModuleCategory[]{ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC}) {
            panels.add(new AlPanel(category, this));
        }
    }

    boolean hiddenBySearch(String name) {
        if (search.isEmpty()) {
            return false;
        }
        return !name.replace(" ", "").toLowerCase().contains(search.replace(" ", "").toLowerCase());
    }

    private double mapX(double screenX) {
        return pivotX + (screenX - pivotX) / totalScale;
    }

    private double mapY(double screenY) {
        return pivotY + (screenY - pivotY) / totalScale;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AlUi.beginFrame();
        ensurePanels();

        float width = window.getScaledWidth();
        float height = window.getScaledHeight();
        float anim = this.getScaleAnimation();
        float contentW = panels.size() * (PANEL_W + PANEL_GAP) - PANEL_GAP;
        float fit = Math.min(1.0F, Math.min((width - 16.0F) / contentW, (height - 24.0F) / (PANEL_H + 60.0F)));
        fit = Math.max(fit, 0.3F);
        pivotX = width / 2.0F;
        pivotY = height / 2.0F;
        float animScale = anim == 1.0F ? 1.0F : 0.5F + anim / 2.0F;
        totalScale = Math.max(0.05F, fit * animScale);
        int mx = (int) mapX(mouseX);
        int my = (int) mapY(mouseY);

        Matrix3x2fStack m = context.getMatrices();
        AlUi.box(m, 0.0F, 0.0F, width, height, 0.0F, AlUi.rgba(0, 0, 0, (int) (110 * AlUi.clamp01(anim))));

        final float fitScale = fit;
        m.pushMatrix();
        m.translate(pivotX, pivotY);
        m.scale(fitScale, fitScale);
        m.translate(-pivotX, -pivotY);
        MathUtil.scale(m, pivotX, pivotY, anim, () -> drawUi(m, mx, my, width, height, AlUi.clamp01(anim)));
        m.popMatrix();

        AlUi.applyCursor();
    }

    private void drawUi(Matrix3x2fStack m, int mx, int my, float width, float height, float alpha) {
        float contentW = panels.size() * (PANEL_W + PANEL_GAP) - PANEL_GAP;
        float startX = (width - contentW) / 2.0F;
        float panelY = (height - PANEL_H) / 2.0F - 20.0F;

        for (int i = 0; i < panels.size(); i++) {
            AlPanel panel = panels.get(i);
            panel.x = startX + i * (PANEL_W + PANEL_GAP);
            panel.y = panelY;
            panel.w = PANEL_W;
            panel.h = PANEL_H;
            panel.render(m, mx, my, alpha);
        }

        searchX = width / 2.0F - SEARCH_W / 2.0F;
        searchY = panelY + PANEL_H + 15.0F;
        drawSearch(m, mx, my, alpha);
        config.render(m, mx, my, width, height, alpha);
    }

    private void drawSearch(Matrix3x2fStack m, int mx, int my, float alpha) {
        if (AlUi.hover(mx, my, searchX, searchY, SEARCH_W, SEARCH_H)) {
            AlUi.wantIBeam = true;
        }
        AlUi.blurBox(m, searchX, searchY, SEARCH_W, SEARCH_H, 4.0F, AlUi.rgba(14, 17, 32, (int) (215 * alpha)));
        AlUi.box(m, searchX - 0.5F, searchY - 0.5F, SEARCH_W + 1.0F, SEARCH_H + 1.0F, 4.5F, AlUi.rgba(70, 76, 120, (int) (90 * alpha)));
        AlUi.box(m, searchX, searchY, SEARCH_W, SEARCH_H, 4.0F, AlUi.rgba(18, 21, 38, (int) (215 * alpha)));

        boolean placeholder = search.isEmpty() && !searchFocused;
        String caret = searchFocused && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
        String text = placeholder ? "Поиск" : search + caret;
        int color = placeholder ? AlUi.rgba(150, 150, 150, (int) (255 * alpha)) : AlUi.rgba(255, 255, 255, (int) (255 * alpha));
        AlUi.text(m, text, searchX + 6.0F, searchY + SEARCH_H / 2.0F, SEARCH_PX, color);
    }

    @Override
    public void removed() {
        AlUi.resetCursor();
        searchFocused = false;
        config.reset();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mapX(mouseX);
        double my = mapY(mouseY);
        boolean inConfig = config.mouseClicked(mx, my, button);
        searchFocused = !inConfig && AlUi.hover(mx, my, searchX, searchY, SEARCH_W, SEARCH_H);
        if (inConfig) {
            return false;
        }
        for (AlPanel panel : panels) {
            if (search.isEmpty() ? panel.inside(mx, my) : true) {
                panel.mouseClicked(mx, my, button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mx = mapX(mouseX);
        double my = mapY(mouseY);
        for (AlPanel panel : panels) {
            panel.mouseReleased(mx, my, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double mx = mapX(mouseX);
        double my = mapY(mouseY);
        if (config.mouseScrolled(mx, my, vertical)) {
            return true;
        }
        for (AlPanel panel : panels) {
            panel.mouseScrolled(mx, my, vertical);
        }
        return true;
    }

    private boolean capturing() {
        if (searchFocused || config.capturing()) {
            return true;
        }
        for (AlPanel panel : panels) {
            if (panel.capturing()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (config.capturing()) {
            config.keyPressed(keyCode);
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
            }
            return true;
        }
        if (capturing()) {
            for (AlPanel panel : panels) {
                if (panel.keyPressed(keyCode)) {
                    break;
                }
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            this.animation.setDirection(Direction.BACKWARDS);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (config.capturing()) {
            config.charTyped(chr);
            return true;
        }
        if (searchFocused) {
            if (chr >= 32 && search.length() < SEARCH_MAX) {
                search += chr;
            }
            return true;
        }
        for (AlPanel panel : panels) {
            if (panel.charTyped(chr)) {
                return true;
            }
        }
        return false;
    }
}
