package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.ScaledResolution;
import ru.zov.implement.utils.RenderUtils;
import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DeluxeWatermark extends DeluxeDraggable {

    public DeluxeWatermark() {
        super("DeluxeWatermark", 300, 6, 220, 18);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (!enabled) return;
        updateDrag(mouseX, mouseY);

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String playerName = mc.thePlayer != null ? mc.thePlayer.getName() : "Player";
        String text = String.format("PLUS §7| §f%s §7| §f%d FPS §7| §f%s", playerName, mc.DebugFPS, time);

        this.width = mc.fontRendererObj.getStringWidth(text) + 16;

        if (!dragging && x == 300) {
            ScaledResolution sr = new ScaledResolution(mc);
            this.x = (sr.getScaledWidth() - width) / 2;
        }

        RenderUtils.drawRect(x, y, x + width, y + height, new Color(16, 16, 20, 200).getRGB());
        RenderUtils.drawRect(x, y, x + width, y + 2, new Color(145, 70, 255).getRGB());

        mc.fontRendererObj.drawStringWithShadow(text, x + 8, y + 5, Color.WHITE.getRGB());
    }
}
