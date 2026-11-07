package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.QuickImports;

@Mixin({CrossbowItem.class})
public abstract class CrossBowItemMixin implements QuickImports {
   @Shadow
   public abstract boolean onStoppedUsing(ItemStack var1, World var2, LivingEntity var3, int var4);

   @Inject(
      method = {"usageTick"},
      at = {@At("RETURN")}
   )
   public void usageTickHook(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
      if (this.onStoppedUsing(stack, world, user, remainingUseTicks)) {
         mc.options.useKey.setPressed(false);
      }

   }

   @ModifyExpressionValue(
      method = {"onStoppedUsing"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/item/CrossbowItem;getMaxUseTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)I"
)}
   )
   public int onStoppedUsingHook(int original) {
      return original - 1;
   }
}
