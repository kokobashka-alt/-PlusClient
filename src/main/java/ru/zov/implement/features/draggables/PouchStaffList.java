package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

import java.util.ArrayList;
import java.util.List;

public class PouchStaffList extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<String> staff = new ArrayList<>();

    public PouchStaffList() {
        super("PouchStaffList", 270, 60, 120, 25, true);
    }

    @Override
    public void tick() {
        staff.clear();

        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            Team team = entry.getScoreboardTeam();

            if (team == null) {
                continue;
            }

            String prefix = team.getPrefix().getString().toLowerCase();
            String name = entry.getProfile().name();

            if (isStaff(prefix)) {
                String status = "ONLINE";

                if (entry.getGameMode() == GameMode.SPECTATOR) {
                    status = "GM3";
                } else if (mc.world != null
                        && mc.world.getPlayers().stream()
                        .anyMatch(player -> player.getGameProfile()
                                .name().equals(name))) {
                    status = "NEAR";
                }

                staff.add(name + "|" + status);
            }
        }
    }

    private boolean isStaff(String prefix) {
        return prefix.contains("helper")
                || prefix.contains("moder")
                || prefix.contains("admin")
                || prefix.contains("owner")
                || prefix.contains("куратор")
                || prefix.contains("модер")
                || prefix.contains("админ")
                || prefix.contains("хелпер")
                || prefix.contains("стажер");
    }

    @Override
    public boolean visible() {
        // Если открыт чат, принудительно показываем элемент для перетаскивания
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return !staff.isEmpty();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontItem = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // Заголовок с блюром
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), 17.5F)
                .round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        // Основной фон
        blur.render(ShapeProperties.create(matrix, this.getX(), (float) this.getY() + 17.0F, this.getWidth(), this.getHeight() - 17)
                .round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        // Заголовок по центру
        float centerX = this.getX() + this.getWidth() / 2.0F;
        String header = "Staff List";
        font.drawString(matrix, header, (int) (centerX - font.getStringWidth(header) / 2.0F), this.getY() + 7, ColorUtil.getText());

        // Полоска акцента
        rectangle.render(ShapeProperties.create(matrix, this.getX() + 5, this.getY() + 16.5F, this.getWidth() - 10, 1.0F)
                .color(ColorUtil.getClientColor()).build());

        int offset = 23;
        int maxWidth = 120;

        // Если открыт чат и стафа нет, показываем заглушку
        if (staff.isEmpty() && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            fontItem.drawString(matrix, "Staff_Example", this.getX() + 5, this.getY() + offset, ColorUtil.getText(0.5F));
            int statusWidth = (int) fontItem.getStringWidth("NEAR");
            fontItem.drawString(matrix, "NEAR", this.getX() + this.getWidth() - statusWidth - 5, this.getY() + offset, ColorUtil.getColor(112, 220, 112));
            offset += 14;
        } else {
            for (String entry : staff) {
                String[] parts = entry.split("\\|", 2);
                String name = parts[0];
                String status = parts[1];

                // Ник (слева)
                fontItem.drawString(matrix, name, this.getX() + 5, this.getY() + offset, ColorUtil.getText());

                // Статус (справа)
                int color = status.equals("GM3") ? ColorUtil.getColor(255, 212, 92) : 
                            status.equals("NEAR") ? ColorUtil.getColor(112, 220, 112) : 
                            ColorUtil.getText(0.7F);

                int statusWidth = (int) fontItem.getStringWidth(status);
                fontItem.drawString(matrix, status, this.getX() + this.getWidth() - statusWidth - 5, this.getY() + offset, color);

                int width = (int) fontItem.getStringWidth(name) + statusWidth + 20;
                maxWidth = Math.max(maxWidth, width);
                offset += 14;
            }
        }

        this.setWidth(maxWidth);
        this.setHeight(offset + 5);
    }
}
