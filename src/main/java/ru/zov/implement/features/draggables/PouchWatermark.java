
package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.draggable.AbstractDraggable;

public class PouchWatermark extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public PouchWatermark() {
        super("PouchWatermark", 10, 10, 150, 18, true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null) return;

        int x = (int) getX();
        int y = (int) getY();

        String clientName = "Pouch";
        String username = mc.player.getName().getString();
        String fps = mc.getCurrentFps() + " FPS";

        String server;
        if (mc.isInSingleplayer()) {
            server = "Singleplayer";
        } else if (mc.getNetworkHandler() != null) {
            server = mc.getNetworkHandler()
                    .getConnection()
                    .getAddress()
                    .toString();
        } else {
            server = "Menu";
        }

        String text = clientName + " | " + username
                + " | " + fps + " | " + server;

        int width = mc.textRenderer.getWidth(text) + 12;
        int height = 18;

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

        // Text
        context.drawText(
                mc.textRenderer,
                text,
                x + 7,
                y + 5,
                0xFFFFFFFF,
                false
        );

        setWidth(width);
        setHeight(height);
    }
}
