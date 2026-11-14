package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

/**
 * Potions list in the AspectDLC style: one flat tinted panel, a bold "Potions list" title on the left, then one
 * row per effect: icon, lower-case name with the level in brackets in roman numerals, remaining time on the right.
 */
public class PouchPotions extends AbstractDraggable {
    private static final float MIN_WIDTH = 90.0F;
    private static final float NAME_TIME_GAP = 12.0F;

    private record Row(RegistryEntry<StatusEffect> type, String label, String time) {
    }

    private final List<StatusEffectInstance> effects = new ArrayList<>();

    public PouchPotions() {
        super("PouchPotions", 10, 60, 90, 28, true);
    }

    @Override
    public void tick() {
        effects.clear();
        if (mc.player != null) {
            effects.addAll(mc.player.getStatusEffects());
        }
    }

    @Override
    public boolean visible() {
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return !effects.isEmpty();
    }

    private static Row rowOf(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString().toLowerCase(Locale.ROOT);
        String label = name + " [" + PouchStyle.roman(effect.getAmplifier() + 1) + "]";
        boolean endless = effect.isInfinite() || effect.getDuration() == Integer.MAX_VALUE;
        String time = endless ? "∞" : PouchStyle.clock(effect.getDuration() / 20);
        return new Row(effect.getEffectType(), label, time);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.BOLD);
        FontRenderer nameFont = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer timeFont = Fonts.getSize(10, Fonts.Type.DEFAULT);

        boolean placeholder = effects.isEmpty() && PlayerIntersectionUtil.isChat(mc.currentScreen);
        List<Row> rows = new ArrayList<>();
        if (placeholder) {
            rows.add(new Row(StatusEffects.SPEED, "скорость [II]", "1:00"));
        } else {
            for (StatusEffectInstance effect : effects) {
                rows.add(rowOf(effect));
            }
        }

        float panelWidth = MIN_WIDTH;
        for (Row row : rows) {
            float w = PouchStyle.SIDE_PADDING + PouchStyle.ICON_SIZE + 4.0F + nameFont.getStringWidth(row.label())
                    + NAME_TIME_GAP + timeFont.getStringWidth(row.time()) + PouchStyle.SIDE_PADDING;
            panelWidth = Math.max(panelWidth, w);
        }

        float panelHeight = PouchStyle.HEADER_HEIGHT + rows.size() * PouchStyle.ROW_HEIGHT + (rows.isEmpty() ? 0.0F : 2.0F);
        float x = this.getX();
        float y = this.getY();

        rectangle.render(ShapeProperties.create(matrix, x, y, panelWidth, panelHeight)
                .round(PouchStyle.PANEL_ROUND).color(PouchStyle.panelColor(ColorUtil.getClientColor())).build());

        titleFont.drawString(matrix, "Potions list", x + PouchStyle.SIDE_PADDING, PouchStyle.textY(y + PouchStyle.HEADER_HEIGHT / 2.0F, 14), ColorUtil.WHITE);

        int nameColor = placeholder ? ColorUtil.getText(0.5F) : ColorUtil.WHITE;
        int timeColor = ColorUtil.getText(placeholder ? 0.4F : 0.7F);
        float rowTop = y + PouchStyle.HEADER_HEIGHT;
        for (Row row : rows) {
            float centerY = rowTop + PouchStyle.ROW_HEIGHT / 2.0F;
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    InGameHud.getEffectTexture(row.type()),
                    (int) (x + PouchStyle.SIDE_PADDING), (int) (centerY - PouchStyle.ICON_SIZE / 2.0F),
                    (int) PouchStyle.ICON_SIZE, (int) PouchStyle.ICON_SIZE
            );
            nameFont.drawString(matrix, row.label(), x + PouchStyle.SIDE_PADDING + PouchStyle.ICON_SIZE + 4.0F, PouchStyle.textY(centerY, 12), nameColor);
            timeFont.drawString(matrix, row.time(), x + panelWidth - PouchStyle.SIDE_PADDING - timeFont.getStringWidth(row.time()), PouchStyle.textY(centerY, 10), timeColor);
            rowTop += PouchStyle.ROW_HEIGHT;
        }

        this.setWidth((int) Math.ceil(panelWidth));
        this.setHeight((int) Math.ceil(panelHeight));
    }
}
