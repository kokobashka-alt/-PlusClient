package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class PouchWatermark extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public PouchWatermark() {
        super("PouchWatermark", 10, 10, 150, 18, true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null) return;

        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontSmall = Fonts.getSize(13, Fonts.Type.DEFAULT);

        String clientName = "Pouch";
        String username = mc.player.getName().getString();
        String fps = mc.getCurrentFps() + "fps";
        String role = "User"; // Можно заменить на получение роли

        // Получаем пинг
        String ping = "0ms";
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                ping = entry.getLatency() + "ms";
            }
        }

        // Собираем полную строку для расчета ширины
        String fullText = clientName + " | " + username + " [" + role + "] | " + ping + " | " + fps;
        int width = (int) font.getStringWidth(fullText) + 20;
        int height = 18;

        // Фон с блюром и скруглениями
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), width, height)
                .round(4.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        // Название клиента (градиент)
        float currentX = this.getX() + 5;
        font.drawGradientString(matrix, clientName, currentX, this.getY() + 5, ColorUtil.fade(0), ColorUtil.fade(100));
        currentX += font.getStringWidth(clientName);

        // Разделитель
        font.drawString(matrix, " | ", currentX, this.getY() + 5, ColorUtil.getText(0.5F));
        currentX += font.getStringWidth(" | ");

        // Ник
        font.drawString(matrix, username, currentX, this.getY() + 5, ColorUtil.getText());
        currentX += font.getStringWidth(username);

        // Роль (в скобках, серым)
        String roleText = " [" + role + "]";
        fontSmall.drawString(matrix, roleText, currentX, this.getY() + 6, ColorUtil.getText(0.6F));
        currentX += fontSmall.getStringWidth(roleText);

        // Разделитель
        font.drawString(matrix, " | ", currentX, this.getY() + 5, ColorUtil.getText(0.5F));
        currentX += font.getStringWidth(" | ");

        // Пинг и FPS (фиолетовым)
        fontSmall.drawString(matrix, ping, currentX, this.getY() + 6, ColorUtil.getClientColor());
        currentX += fontSmall.getStringWidth(ping);
        
        fontSmall.drawString(matrix, " | ", currentX, this.getY() + 6, ColorUtil.getText(0.5F));
        currentX += fontSmall.getStringWidth(" | ");

        fontSmall.drawString(matrix, fps, currentX, this.getY() + 6, ColorUtil.getClientColor());

        setWidth(width);
        setHeight(height);
    }
}
