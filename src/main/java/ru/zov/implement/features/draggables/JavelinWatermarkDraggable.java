package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class JavelinWatermarkDraggable extends AbstractDraggable {

    public JavelinWatermarkDraggable() {
        super("javelinWatermark", 10, 10, 130, 20, true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.BOLD);

        String text = "⚡ plusclient |  " + mc.getCurrentFps() + " fps";

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(3.0F).color(ColorUtil.getColor(16, 16, 16, 0.7F)).build());
        font.drawString(matrix, text, getX() + 8, getY() + 6, -1);
    }
}
