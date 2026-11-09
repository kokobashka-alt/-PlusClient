package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.TriggerBot;

public class PouchArmor extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private LivingEntity target;

    private final List<EquipmentSlot> armorSlots = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    public PouchArmor() {
        super("PouchArmor", 300, 260, 120, 30, true);
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        LivingEntity triggerTarget = TriggerBot.getInstance().getTarget();

        if (auraTarget != null && auraTarget.isAlive()) {
            target = auraTarget;
        } else if (triggerTarget != null && triggerTarget.isAlive()) {
            target = triggerTarget;
        } else {
            target = null;
        }
    }

    @Override
    public boolean visible() {
        if (target == null) return false;

        for (EquipmentSlot slot : armorSlots) {
            if (!target.getEquippedStack(slot).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (target == null) return;

        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontItem = Fonts.getSize(13, Fonts.Type.DEFAULT);

        List<ItemStack> items = new ArrayList<>();

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = target.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        // Заголовок (17.5 высота как в других твоих худах)
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), 17.5F)
                .round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        // Основной фон (список)
        blur.render(ShapeProperties.create(matrix, this.getX(), (float) this.getY() + 17.0F, this.getWidth(), this.getHeight() - 17)
                .round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        // Заголовок текста (по центру)
        float centerX = this.getX() + this.getWidth() / 2.0F;
        font.drawString(matrix, "Target Armor", (int) (centerX - font.getStringWidth("Target Armor") / 2.0F), this.getY() + 7, ColorUtil.getText());

        // Полоска акцента под заголовком
        rectangle.render(ShapeProperties.create(matrix, this.getX() + 5, this.getY() + 16.5F, this.getWidth() - 10, 1.0F)
                .color(ColorUtil.getClientColor()).build());

        int offset = 22;
        int maxWidth = 120;

        for (ItemStack stack : items) {
            // Иконка предмета
            context.drawItem(stack, (int) this.getX() + 5, (int) this.getY() + offset);

            String name = stack.getName().getString();
            String durability;

            if (stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                int percent = remaining * 100 / stack.getMaxDamage();
                durability = percent + "%";
            } else {
                durability = "100%";
            }

            // Название предмета (по левому краю)
            fontItem.drawString(matrix, name, this.getX() + 25, this.getY() + offset + 2, ColorUtil.getText());

            // Прочность (по правому краю)
            int durabilityWidth = (int) fontItem.getStringWidth(durability);
            fontItem.drawString(matrix, durability, this.getX() + this.getWidth() - durabilityWidth - 5, this.getY() + offset + 2, ColorUtil.getClientColor());

            int width = (int) fontItem.getStringWidth(name + durability) + 40;
            maxWidth = Math.max(maxWidth, width);
            offset += 22;
        }

        this.setWidth(maxWidth);
        this.setHeight(offset);
    }
}
