package ru.zov.implement.features.draggables;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.module.Module;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.other.StringUtil;

/**
 * Keybinds panel in the AspectDLC style (Aspect KeyBindsHud, "DefaultStyle"): one flat rounded panel
 * tinted by the client color, a "Keybinds" title, then one 12 px row per bound module with the module name
 * on the left in white and the key on the right in the client color. Rows fade in and out with the module's
 * own animation. The header icon of the original uses Aspect's icon font and is not reproduced.
 */
public class PouchHotKeys extends AbstractDraggable {
    private static final float MIN_WIDTH = 70.0F;
    private static final float ROW_HEIGHT = 12.0F;
    private static final float HEADER_HEIGHT = 16.0F;
    private static final float FIRST_ROW_OFFSET = 18.0F;
    private static final float SIDE_PADDING = 5.5F;
    private static final float PANEL_ROUND = 3.0F;
    private static final float VISIBLE_EPSILON = 0.01F;

    private List<Module> keysList = List.of();

    public PouchHotKeys() {
        super("PouchHotKeys", 140, 60, 120, 23, true);
    }

    @Override
    public void tick() {
        keysList = main.getModuleProvider()
                .getModules()
                .stream()
                .filter(module -> module.getAnimation().getOutput().floatValue() != 0.0F
                        && module.getKey() != -1)
                .toList();
    }

    @Override
    public boolean visible() {
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return !keysList.isEmpty();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer nameFont = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer keyFont = Fonts.getSize(10, Fonts.Type.DEFAULT);

        int accent = ColorUtil.getClientColor();
        List<Module> drawList = keysList;
        boolean placeholder = drawList.isEmpty() && PlayerIntersectionUtil.isChat(mc.currentScreen);

        float contentWidth = MIN_WIDTH - 6.0F;
        float rowsHeight = 0.0F;
        int visibleRows = 0;
        if (placeholder) {
            contentWidth = Math.max(contentWidth, nameFont.getStringWidth("Пример бинда") + keyFont.getStringWidth("Z") + 20.0F);
            rowsHeight = ROW_HEIGHT;
            visibleRows = 1;
        } else {
            for (Module module : drawList) {
                float anim = module.getAnimation().getOutput().floatValue();
                if (anim <= VISIBLE_EPSILON) {
                    continue;
                }
                String key = StringUtil.getBindName(module.getKey());
                contentWidth = Math.max(contentWidth, nameFont.getStringWidth(module.getName()) + keyFont.getStringWidth(key) + 20.0F);
                rowsHeight += ROW_HEIGHT * anim;
                visibleRows++;
            }
        }

        float panelWidth = contentWidth + 6.0F;
        float panelHeight = HEADER_HEIGHT + rowsHeight + (visibleRows > 0 ? 2.0F : 0.0F);

        int r = (accent >> 16) & 255;
        int g = (accent >> 8) & 255;
        int b = accent & 255;
        int panelColor = ColorUtil.getColor(Math.min(12 + (int) (r * 0.08F), 255), Math.min(12 + (int) (g * 0.08F), 255), Math.min(14 + (int) (b * 0.08F), 255), 210.0F / 255.0F);
        rectangle.render(ShapeProperties.create(matrix, this.getX(), this.getY(), panelWidth, panelHeight)
                .round(PANEL_ROUND).color(panelColor).build());

        titleFont.drawString(matrix, "Keybinds", this.getX() + 5.0F, this.getY() + 6.0F, ColorUtil.WHITE);

        float rowY = FIRST_ROW_OFFSET;
        if (placeholder) {
            nameFont.drawString(matrix, "Пример бинда", this.getX() + SIDE_PADDING, this.getY() + 2.0F + rowY, ColorUtil.getText(0.5F));
            keyFont.drawString(matrix, "Z", this.getX() + panelWidth - keyFont.getStringWidth("Z") - 7.0F, this.getY() + rowY + 2.8F, accent);
        } else {
            for (Module module : drawList) {
                float anim = module.getAnimation().getOutput().floatValue();
                if (anim <= VISIBLE_EPSILON) {
                    continue;
                }
                String key = StringUtil.getBindName(module.getKey());
                nameFont.drawString(matrix, module.getName(), this.getX() + SIDE_PADDING, this.getY() + 2.0F + rowY, ColorUtil.multAlpha(ColorUtil.WHITE, anim));
                keyFont.drawString(matrix, key, this.getX() + panelWidth - keyFont.getStringWidth(key) - 7.0F, this.getY() + rowY + 2.8F, ColorUtil.multAlpha(accent, anim));
                rowY += ROW_HEIGHT * anim;
            }
        }

        this.setWidth((int) Math.ceil(panelWidth));
        this.setHeight((int) Math.ceil(panelHeight));
    }
}
