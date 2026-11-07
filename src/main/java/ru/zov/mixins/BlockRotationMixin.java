package ru.zov.mixins;

import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.util.BlockRotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({BlockRotation.class})
public class BlockRotationMixin {
   @Unique
   private ArmorMaterial armorMaterial;
}
