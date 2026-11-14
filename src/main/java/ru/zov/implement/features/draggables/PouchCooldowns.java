package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import ru.zov.implement.events.packet.PacketEvent;

public class PouchCooldowns extends AbstractDraggable {

    private static final float MIN_WIDTH = 90.0F;
    private static final float NAME_TIME_GAP = 12.0F;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final List<PouchCoolDown> list = new ArrayList<>();

    public PouchCooldowns() {
        super("PouchCooldowns", 10, 120, 90, 28, true);
    }

    @Override
    public boolean visible() {
        // Если открыт чат, показываем панель всегда
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return !list.isEmpty();
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

    private void drawItem(DrawContext context, ItemStack stack, float x, float y, float size) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        float scale = size / 16.0F;
        matrix.pushMatrix();
        matrix.translate(x, y);
        matrix.scale(scale, scale);
        context.drawItem(stack, 0, 0);
        matrix.popMatrix();
    }

    private record Row(ItemStack stack, String name, String time, float anim) {
    }

    /**
     * Cooldowns in the AspectDLC style: one flat tinted panel, a bold "Cooldowns" title on the left, then one row
     * per item on cooldown: small item icon, name, remaining time as m:ss on the right. Rows fade with their animation.
     */
    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.BOLD);
        FontRenderer nameFont = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer timeFont = Fonts.getSize(10, Fonts.Type.DEFAULT);

        boolean placeholder = list.isEmpty() && PlayerIntersectionUtil.isChat(mc.currentScreen);
        List<Row> rows = new ArrayList<>();
        if (placeholder) {
            ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
            rows.add(new Row(pearl, pearl.getName().getString(), "0:15", 1.0F));
        } else {
            for (PouchCoolDown coolDown : list) {
                float animation = coolDown.anim().getOutput().floatValue();
                if (animation <= 0.05F) {
                    continue;
                }

                long remainingMs = Math.max(0L, -(long) coolDown.time().elapsedInt());
                ItemStack stack = coolDown.item().getDefaultStack();
                rows.add(new Row(stack, stack.getName().getString(), PouchStyle.clock((remainingMs + 999L) / 1000L), animation));
            }
        }

        float panelWidth = MIN_WIDTH;
        float rowsHeight = 0.0F;
        for (Row row : rows) {
            float w = PouchStyle.SIDE_PADDING + PouchStyle.ICON_SIZE + 4.0F + nameFont.getStringWidth(row.name())
                    + NAME_TIME_GAP + timeFont.getStringWidth(row.time()) + PouchStyle.SIDE_PADDING;
            panelWidth = Math.max(panelWidth, w);
            rowsHeight += PouchStyle.ROW_HEIGHT * row.anim();
        }

        float panelHeight = PouchStyle.HEADER_HEIGHT + rowsHeight + (rows.isEmpty() ? 0.0F : 2.0F);
        float x = this.getX();
        float y = this.getY();

        rectangle.render(ShapeProperties.create(matrix, x, y, panelWidth, panelHeight)
                .round(PouchStyle.PANEL_ROUND).color(PouchStyle.panelColor(ColorUtil.getClientColor())).build());

        titleFont.drawString(matrix, "Cooldowns", x + PouchStyle.SIDE_PADDING, PouchStyle.textY(y + PouchStyle.HEADER_HEIGHT / 2.0F, 14), ColorUtil.WHITE);

        float rowTop = y + PouchStyle.HEADER_HEIGHT;
        for (Row row : rows) {
            float centerY = rowTop + PouchStyle.ROW_HEIGHT / 2.0F;
            if (row.anim() > 0.5F) {
                drawItem(context, row.stack(), x + PouchStyle.SIDE_PADDING, centerY - PouchStyle.ICON_SIZE / 2.0F, PouchStyle.ICON_SIZE);
            }
            nameFont.drawString(matrix, row.name(), x + PouchStyle.SIDE_PADDING + PouchStyle.ICON_SIZE + 4.0F, PouchStyle.textY(centerY, 12), ColorUtil.multAlpha(placeholder ? ColorUtil.getText(0.5F) : ColorUtil.WHITE, row.anim()));
            timeFont.drawString(matrix, row.time(), x + panelWidth - PouchStyle.SIDE_PADDING - timeFont.getStringWidth(row.time()), PouchStyle.textY(centerY, 10), ColorUtil.multAlpha(ColorUtil.getText(0.7F), row.anim()));
            rowTop += PouchStyle.ROW_HEIGHT * row.anim();
        }

        this.setWidth((int) Math.ceil(panelWidth));
        this.setHeight((int) Math.ceil(panelHeight));
    }

    public record PouchCoolDown(Item item, StopWatch time, Animation anim) {
    }
}
