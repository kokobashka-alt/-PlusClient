package ru.zov.implement.features.draggables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;

public class JavelinTargetHudDraggable extends AbstractDraggable {
    
    // Можешь передавать сюда текущую цель из киллауры
    public static LivingEntity currentTargetPath = null;

    public JavelinTargetHudDraggable() {
        super("javelinTargetHud", 200, 200, 130, 36);
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        
        // Для теста в редакторе можно выводить себя или оставить пустым, если цель null
        LivingEntity target = currentTargetPath != null ? currentTargetPath : Minecraft.getInstance().player;
        if (target == null) return;

        FontRenderer font = Fonts.bold;
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColorUtil.rgba(16, 16, 16, 180));

        if (font != null) {
            String name = target.getName().getString();
            font.drawString(context.getMatrices(), name, getX() + 34, getY() + 6, -1);

            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            float hpPercentage = Math.min(1.0f, health / maxHealth);

            // Полоска здоровья
            context.fill(getX() + 34, getY() + 20, getX() + 120, getY() + 26, ColorUtil.rgba(68, 68, 68, 100));
            context.fill(getX() + 34, getY() + 20, getX() + 34 + (int)(86 * hpPercentage), getY() + 26, ColorUtil.rgba(255, 85, 85, 255));

            font.drawString(context.getMatrices(), "❤ " + (int)health, getX() + 34, getY() + 10, 0xFFFF5555);
        }
    }
}
