package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import ru.zov.api.feature.draggable.AbstractDraggable;

import java.util.ArrayList;
import java.util.List;

public class PouchStaffList extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<String> staff = new ArrayList<>();

    public PouchStaffList() {
        super("PouchStaffList", 300, 150, 120, 25, true);
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
        return !staff.isEmpty();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        int x = (int) getX();
        int y = (int) getY();

        int width = 130;
        int height = 23 + staff.size() * 15;

        for (String entry : staff) {
            String[] parts = entry.split("\\|", 2);
            width = Math.max(
                    width,
                    mc.textRenderer.getWidth(parts[0])
                            + mc.textRenderer.getWidth(parts[1]) + 20
            );
        }

        context.fill(x, y, x + width, y + height, 0xAA101010);
        context.fill(x, y, x + 2, y + height, 0xFF8B5CF6);

        context.drawText(
                mc.textRenderer,
                "Staff List",
                x + 7, y + 6,
                0xFF8B5CF6,
                false
        );

        int offset = 23;

        for (String entry : staff) {
            String[] parts = entry.split("\\|", 2);
            String name = parts[0];
            String status = parts[1];

            context.drawText(
                    mc.textRenderer,
                    name,
                    x + 7, y + offset,
                    0xFFFFFFFF,
                    false
            );

            int color = status.equals("GM3")
                    ? 0xFFFFD45C
                    : status.equals("NEAR")
                    ? 0xFF70DC70
                    : 0xFFAAAAAA;

            int statusWidth = mc.textRenderer.getWidth(status);

            context.drawText(
                    mc.textRenderer,
                    status,
                    x + width - statusWidth - 7,
                    y + offset,
                    color,
                    false
            );

            offset += 15;
        }

        setWidth(width);
        setHeight(height);
    }
}
