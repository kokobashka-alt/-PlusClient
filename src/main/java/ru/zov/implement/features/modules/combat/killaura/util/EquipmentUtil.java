package ru.zov.implement.features.modules.combat.killaura.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public final class EquipmentUtil {
    private EquipmentUtil() {
    }

    public static List<ItemStack> armor(LivingEntity entity) {
        return new ArrayList<>(List.of(
            entity.getEquippedStack(EquipmentSlot.FEET),
            entity.getEquippedStack(EquipmentSlot.LEGS),
            entity.getEquippedStack(EquipmentSlot.CHEST),
            entity.getEquippedStack(EquipmentSlot.HEAD)
        ));
    }

    public static ItemStack armorStack(LivingEntity entity, int index) {
        return armor(entity).get(index);
    }
}
