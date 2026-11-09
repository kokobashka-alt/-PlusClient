
package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.events.packet.PacketEvent;

public class PouchCooldowns extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final List<PouchCoolDown> list = new ArrayList<>();

    public PouchCooldowns() {
        super("PouchCooldowns", 120, 300, 100, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty()
                || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(coolDown ->
                coolDown.anim().isFinished(Direction.BACKWARDS)
        );

        list.stream()
                .filter(coolDown ->
                        !ItemUtil.hasCooldown(coolDown.item())
                )
                .forEach(coolDown ->
                        coolDown.anim().setDirection(Direction.BACKWARDS)
                );
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) {
            return;
        }

        Packet<?> packet = e.getPacket();

        if (packet instanceof CooldownUpdateS2CPacket cooldownUpdate) {

            Item item = Registries.ITEM.get(
                    cooldownUpdate.cooldownGroup()
            );

            list.stream()
                    .filter(coolDown -> coolDown.item().equals(item))
                    .forEach(coolDown ->
                            coolDown.anim().setDirection(Direction.BACKWARDS)
                    );

            int cooldown = MathHelper.clamp(
                    cooldownUpdate.cooldown(),
                    -1,
                    100000
            );

            if (cooldown != 0) {
                list.add(new PouchCoolDown(
                        item,
                        new StopWatch().setMs(-cooldown * 50),
                        new DecelerateAnimation()
                                .setMs(150)
                                .setValue(1.0F)
                ));
            }

        } else if (packet instanceof PlayerRespawnS2CPacket) {
            list.clear();
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        int x = (int) getX();
        int y = (int) getY();

        int width = 100;
        int offset = 23;

        // Background
        context.fill(
                x, y,
                x + width, y + 20,
                0xAA101010
        );

        context.fill(
                x, y + 20,
                x + width, y + getHeight(),
                0xAA181818
        );

        // Accent
        context.fill(
                x, y,
                x + 2, y + getHeight(),
                0xFF8B5CF6
        );

        // Header
        String title = "Cooldowns";

        int titleWidth = mc.textRenderer.getWidth(title);

        context.drawText(
                mc.textRenderer,
                title,
                x + (width - titleWidth) / 2,
                y + 6,
                0xFF8B5CF6,
                false
        );

        int maxWidth = 100;

        for (PouchCoolDown coolDown : list) {

            float animation = coolDown.anim().getOutput().floatValue();

            int time = -coolDown.time().elapsedInt() / 100;

            String name = coolDown.item()
                    .getDefaultStack()
                    .getName()
                    .getString();

            String duration = StringUtil.getDurationMS(time, true);

            int rowY = y + offset;

            // Item icon
            context.drawItem(
                    coolDown.item().getDefaultStack(),
                    x + 5,
                    rowY - 3
            );

            // Item name
            context.drawText(
                    mc.textRenderer,
                    name,
                    x + 24,
                    rowY,
                    0xFFFFFFFF,
                    false
            );

            // Remaining time
            int durationWidth = mc.textRenderer.getWidth(duration);

            context.drawText(
                    mc.textRenderer,
                    duration,
                    x + width - durationWidth - 6,
                    rowY,
                    0xFF8B5CF6,
                    false
            );

            int rowWidth = mc.textRenderer.getWidth(name)
                    + durationWidth + 35;

            maxWidth = Math.max(maxWidth, rowWidth);

            offset += Math.max(1, (int) (11.0F * animation));
        }

        setWidth(maxWidth);
        setHeight(offset);
    }

    public record PouchCoolDown(
            Item item,
            StopWatch time,
            Animation anim
    ) {
    }
}
