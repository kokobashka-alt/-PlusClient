package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class JavelinTargetHudDraggable extends AbstractDraggable {

    // Сюда можно передавать текущую цель из киллауры
    public static LivingEntity currentTargetPath = null;

    public JavelinTargetHudDraggable() {
        super("javelinTargetHud", 200, 200, 130, 36, true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        // Для теста в редакторе без цели показываем себя
        LivingEntity target = currentTargetPath != null ? currentTargetPath : mc.player;
        if (target == null) return;

        Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.BOLD);

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(3.0F).color(ColorUtil.getColor(16, 16, 16, 0.7F)).build());

        font.drawString(matrix, target.getName().getString(), getX() + 34, getY() + 6, -1);

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float hpPercentage = maxHealth > 0.0F ? Math.min(1.0F, health / maxHealth) : 0.0F;

        font.drawString(matrix, "❤ " + (int) health, getX() + 34, getY() + 10, 0xFFFF5555);

        rectangle.render(ShapeProperties.create(matrix, getX() + 34, getY() + 20, 86, 6)
                .round(2.0F).color(ColorUtil.getColor(68, 68, 68, 0.4F)).build());
        rectangle.render(ShapeProperties.create(matrix, getX() + 34, getY() + 20, 86 * hpPercentage, 6)
                .round(2.0F).color(ColorUtil.getColor(255, 85, 85, 1.0F)).build());
    }
}
