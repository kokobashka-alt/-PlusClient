package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;

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
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.events.packet.PacketEvent;

public class PouchCooldowns extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final List<PouchCoolDown> list = new ArrayList<>();

    public PouchCooldowns() {
        // Координаты x=10, y=120 (под Potions)
        super("PouchCooldowns", 10, 120, 120, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(coolDown -> coolDown.anim().isFinished(Direction.BACKWARDS));

        list.stream()
                .filter(coolDown -> !ItemUtil.hasCooldown(coolDown.item()))
                .forEach(coolDown -> coolDown.anim().setDirection(Direction.BACKWARDS));
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) {
            return;
        }

        Packet<?> packet = e.getPacket();

        if (packet instanceof CooldownUpdateS2CPacket cooldownUpdate) {
            Item item = Registries.ITEM.get(cooldownUpdate.cooldownGroup());

            list.stream()
                    .filter(coolDown -> coolDown.item().equals(item))
                    .forEach(coolDown -> coolDown.anim().setDirection(Direction.BACKWARDS));

            int cooldown = MathHelper.clamp(cooldownUpdate.cooldown(), -1, 100000);

            if (cooldown != 0) {
                list.add(new PouchCoolDown(
                        item,
                        new StopWatch().setMs(-cooldown * 50),
                        new DecelerateAnimation().setMs(150).setValue(1.0F)
                ));
            }

        } else if (packet instanceof PlayerRespawnS2CPacket) {
            list.clear();
        }
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
        String header = "Cooldowns";
        font.drawString(matrix, header, (int) (centerX - font.getStringWidth(header) / 2.0F), this.getY() + 7, ColorUtil.getText());

        // Полоска акцента
        rectangle.render(ShapeProperties.create(matrix, this.getX() + 5, this.getY() + 16.5F, this.getWidth() - 10, 1.0F)
                .color(ColorUtil.getClientColor()).build());

        int offset = 23;
        int maxWidth = 120;

        for (PouchCoolDown coolDown : list) {
            float animation = coolDown.anim().getOutput().floatValue();
            int time = -coolDown.time().elapsedInt() / 100;
            
            if (animation <= 0.05f) continue;

            String name = coolDown.item().getDefaultStack().getName().getString();
            String duration = StringUtil.getDurationMS(time, true);

            // Иконка предмета
            context.drawItem(coolDown.item().getDefaultStack(), (int) this.getX() + 5, (int) this.getY() + offset - 3);

            // Название (слева)
            fontItem.drawString(matrix, name, this.getX() + 23, this.getY() + offset, ColorUtil.getText());

            // Время (справа)
            int durationWidth = fontItem.getStringWidth(duration);
            fontItem.drawString(matrix, duration, this.getX() + this.getWidth() - durationWidth - 5, this.getY() + offset, ColorUtil.getClientColor());

            int width = (int) fontItem.getStringWidth(name) + durationWidth + 35;
            maxWidth = Math.max(maxWidth, width);
            offset += Math.max(1, (int) (12.0F * animation));
        }

        this.setWidth(maxWidth);
        this.setHeight(offset + 5);
    }

    public record PouchCoolDown(Item item, StopWatch time, Animation anim) {
    }
}
