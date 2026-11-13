package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class JavelinPotionsDraggable extends AbstractDraggable {

    public JavelinPotionsDraggable() {
        super("javelinPotions", 140, 60, 130, 40, true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null || mc.player.getStatusEffects().isEmpty()) return;

        Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.BOLD);

        int height = 16 + mc.player.getStatusEffects().size() * 16;
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), height)
                .round(3.0F).color(ColorUtil.getColor(16, 16, 16, 0.7F)).build());

        font.drawString(matrix, "javelinPotions", getX() + 6, getY() + 5, 0xFFFF5555);

        int offset = 16;
        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            String name = effect.getEffectType().value().getName().getString() + " " + (effect.getAmplifier() + 1);
            font.drawString(matrix, name, getX() + 6, getY() + 5 + offset, -1);
            offset += 16;
        }
    }
}
