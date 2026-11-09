
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

    @Override
    public void drawDraggable(DrawContext context) {
        int x = (int) getX();
        int y = (int) getY();

        int width = 150;
        int height = 22 + effects.size() * 17;

        // Background
        context.fill(
                x, y,
                x + width, y + height,
                0xAA101010
        );

        // Accent
        context.fill(
                x, y,
                x + 2, y + height,
                0xFF8B5CF6
        );

        // Header
        context.drawText(
                mc.textRenderer,
                "Potions",
                x + 7,
                y + 6,
                0xFF8B5CF6,
                false
        );

        int offset = 22;
        int maxWidth = width;

        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType()
                    .value()
                    .getName()
                    .getString();

            int amplifier = effect.getAmplifier() + 1;
            String level = " " + amplifier;

            String time = StatusEffectInstance
                    .getDurationString(effect)
                    .getString();

            context.drawText(
                    mc.textRenderer,
                    name + level,
                    x + 7,
                    y + offset,
                    0xFFFFFFFF,
                    false
            );

            int timeWidth = mc.textRenderer.getWidth(time);

            context.drawText(
                    mc.textRenderer,
                    time,
                    x + width - timeWidth - 7,
                    y + offset,
                    0xFF8B5CF6,
                    false
            );

            maxWidth = Math.max(
                    maxWidth,
                    mc.textRenderer.getWidth(name + level)
                            + timeWidth + 20
            );

            offset += 17;
        }

        setWidth(maxWidth);
        setHeight(height);
    }
}

