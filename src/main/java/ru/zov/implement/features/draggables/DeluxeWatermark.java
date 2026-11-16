package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScaledResolution;
import ru.zov.common.util.render.Render2DUtil;
import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DeluxeWatermark extends DeluxeDraggable {

    public DeluxeWatermark() {
        super("DeluxeWatermark", 300, 6, 220, 18);
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY) {
        if (!enabled) return;
        updateDrag(mouseX, mouseY);

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String text = String.format("DELUXE | FPS | %s", time);

        // Используем textRenderer клиента для получения ширины текста
        this.width = mc.textRenderer.getWidth(text) + 16;

        if (!dragging && x == 300) {
            int screenWidth = mc.getWindow().getScaledWidth();
            this.x = (screenWidth - width) / 2;
        }

        // Отрисовка через ваш новый Render2DUtil
        Render2DUtil.drawQuad(x, y, width, height, new Color(16, 16, 20, 200).getRGB());
        Render2DUtil.drawQuad(x, y, width, 2, new Color(145, 70, 255).getRGB()); // Неоновый верх

        // Рендер текста
        context.drawTextWithShadow(mc.textRenderer, text, x + 8, y + 5, Color.WHITE.getRGB());
    }
}
