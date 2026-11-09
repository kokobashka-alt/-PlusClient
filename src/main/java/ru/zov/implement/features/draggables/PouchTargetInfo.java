
package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.TriggerBot;

public class PouchTargetInfo extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private LivingEntity target;

    private float animatedHealth;

    public PouchTargetInfo() {
        super("PouchTargetInfo", 300, 200, 140, 40, true);
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        LivingEntity triggerTarget = TriggerBot.getInstance().getTarget();

        if (auraTarget != null && auraTarget.isAlive()) {
            target = auraTarget;
        } else if (triggerTarget != null && triggerTarget.isAlive()) {
            target = triggerTarget;
        } else {
            target = null;
        }

        if (target != null) {
            animatedHealth += (target.getHealth() - animatedHealth) * 0.2f;
        }
    }

    @Override
    public boolean visible() {
        return target != null;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (target == null) return;

        int x = (int) getX();
        int y = (int) getY();

        int width = 145;
        int height = 48;

        float maxHealth = Math.max(1.0f, target.getMaxHealth());

        float health = MathHelper.clamp(
                animatedHealth / maxHealth,
                0.0f,
                1.0f
        );

        context.fill(
                x, y,
                x + width, y + height,
                0xAA101010
        );

        context.fill(
                x, y,
                x + 2, y + height,
                0xFF8B5CF6
        );

        // Target name
        context.drawText(
                mc.textRenderer,
                target.getName(),
                x + 8,
                y + 7,
                0xFFFFFFFF,
                false
        );

        // Health text
        String healthText = String.format(
                java.util.Locale.ROOT,
                "%.1f / %.1f HP",
                target.getHealth(),
                maxHealth
        );

        context.drawText(
                mc.textRenderer,
                healthText,
                x + 8,
                y + 21,
                0xFFAAAAAA,
                false
        );

        // Health bar background
        context.fill(
                x + 8, y + 36,
                x + width - 8, y + 41,
                0xFF303030
        );

        // Health bar
        context.fill(
                x + 8, y + 36,
                x + 8 + (int) ((width - 16) * health),
                y + 41,
                0xFF8B5CF6
        );

        setWidth(width);
        setHeight(height);
    }
}

