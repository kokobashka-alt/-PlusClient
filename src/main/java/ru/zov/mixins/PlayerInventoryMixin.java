package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PlayerInventory.class})
public class PlayerInventoryMixin {
   @ModifyExpressionValue(
      method = {"<init>"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/collection/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/collection/DefaultedList;",
   ordinal = 0
)}
   )
   private static DefaultedList<ItemStack> initHook(DefaultedList<ItemStack> original, @Local(ordinal = 0,argsOnly = true) PlayerEntity player) {
      return player == null ? DefaultedList.ofSize(99, ItemStack.EMPTY) : original;
   }
}
