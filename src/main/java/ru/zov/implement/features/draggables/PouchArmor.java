
package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import ru.zov.api.feature.draggable.AbstractDraggable;
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

        int x = (int) getX();
        int y = (int) getY();

        List<ItemStack> items = new ArrayList<>();

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = target.getEquippedStack(slot);

            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        int width = 145;
        int height = 22 + items.size() * 22;

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

        // Header
        context.drawText(
                mc.textRenderer,
                "Target Armor",
                x + 7,
                y + 6,
                0xFF8B5CF6,
                false
        );

        int offset = 20;

        for (ItemStack stack : items) {
            context.drawItem(stack, x + 7, y + offset - 3);

            String name = stack.getName().getString();

            String durability;

            if (stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                int percent = remaining * 100 / stack.getMaxDamage();

                durability = percent + "%";
            } else {
                durability = "100%";
            }

            context.drawText(
                    mc.textRenderer,
                    name,
                    x + 27,
                    y + offset,
                    0xFFFFFFFF,
                    false
            );

            int durabilityWidth = mc.textRenderer.getWidth(durability);

            context.drawText(
                    mc.textRenderer,
                    durability,
                    x + width - durabilityWidth - 7,
                    y + offset,
                    0xFF8B5CF6,
                    false
            );

            offset += 22;
        }

        setWidth(width);
        setHeight(height);
    }
}
