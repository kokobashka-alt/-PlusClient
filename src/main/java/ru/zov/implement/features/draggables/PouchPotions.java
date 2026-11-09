
package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import ru.zov.api.feature.draggable.AbstractDraggable;

import java.util.ArrayList;
import java.util.List;

public class PouchPotions extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<StatusEffectInstance> effects = new ArrayList<>();

    private static final int MIN_WIDTH = 150;
    private static final int HEADER_HEIGHT = 22;
    private static final int ROW_HEIGHT = 17;

    private static final int BACKGROUND = 0xAA101010;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public PouchPotions() {
        super("PouchPotions", 300, 100, 120, 25, true);
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
        return !effects.isEmpty();
    }

    private String getEffectDuration(StatusEffectInstance effect) {
        int ticks = effect.getDuration();

        if (ticks == Integer.MAX_VALUE) {
            return "∞";
        }

        int seconds = ticks / 20;
        int minutes = seconds / 60;

        return String.format("%d:%02d", minutes, seconds % 60);
    }

    private String getEffectName(StatusEffectInstance effect) {
        return effect.getEffectType().value().getName().getString();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        int x = (int) getX();
        int y = (int) getY();

        int width = MIN_WIDTH;
        int height = HEADER_HEIGHT + effects.size() * ROW_HEIGHT;

        for (StatusEffectInstance effect : effects) {
            String name = getEffectName(effect);
            String level = " " + (effect.getAmplifier() + 1);
            String time = getEffectDuration(effect);

            int textWidth = mc.textRenderer.getWidth(name + level);
            int timeWidth = mc.textRenderer.getWidth(time);

            width = Math.max(width, textWidth + timeWidth + 20);
        }

        context.fill(x, y, x + width, y + height, BACKGROUND);
        context.fill(x, y, x + 2, y + height, ACCENT);

        context.drawText(
                mc.textRenderer,
                "Potions",
                x + 7,
                y + 6,
                ACCENT,
                false
        );

        int offset = HEADER_HEIGHT;

        for (StatusEffectInstance effect : effects) {
            String name = getEffectName(effect);
            String level = " " + (effect.getAmplifier() + 1);
            String time = getEffectDuration(effect);

            context.drawText(
                    mc.textRenderer,
                    name + level,
                    x + 7,
                    y + offset,
                    TEXT_COLOR,
                    false
            );

            int timeWidth = mc.textRenderer.getWidth(time);

            context.drawText(
                    mc.textRenderer,
                    time,
                    x + width - timeWidth - 7,
                    y + offset,
                    ACCENT,
                    false
            );

            offset += ROW_HEIGHT;
        }

        setWidth(width);
        setHeight(height);
    }
}
