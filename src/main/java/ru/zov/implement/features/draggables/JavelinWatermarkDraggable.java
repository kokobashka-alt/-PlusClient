package ru.zov.implement.features.draggables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;

public class JavelinWatermarkDraggable extends AbstractDraggable {

    public JavelinWatermarkDraggable() {
        super("javelinWatermark", 10, 10, 130, 20);
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();

        String text = "⚡ plusclient |  " + mc.getCurrentFps() + " fps";
        FontRenderer font = Fonts.bold; // Используем твой шрифт

        // Фон (если есть утилита для скругленных прямоугольников, можно заменить)
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColorUtil.rgba(16, 16, 16, 180));

        if (font != null) {
            font.drawString(context.getMatrices(), text, getX() + 8, getY() + 6, -1);
        }
    }
}
