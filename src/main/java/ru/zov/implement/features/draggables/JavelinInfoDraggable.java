package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class JavelinInfoDraggable extends AbstractDraggable {

    public JavelinInfoDraggable() {
        super("javelinInfo", 10, 35, 170, 20, true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null) return;

        Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.BOLD);

        String text = String.format("cords x: %d y: %d z: %d  |  ⏱ 20.0 tps",
                MathHelper.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()),
                MathHelper.floor(mc.player.getZ()));

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(3.0F).color(ColorUtil.getColor(16, 16, 16, 0.7F)).build());
        font.drawString(matrix, text, getX() + 8, getY() + 6, -1);
    }
}
