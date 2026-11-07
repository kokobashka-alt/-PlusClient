package ru.zov.mixins;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.common.util.world.ServerUtil;

@Mixin({ItemStack.class})
public abstract class ItemStackMixin {
   @Shadow
   public abstract boolean isOf(Item var1);

   @Inject(
      method = {"getMaxCount"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getMaxCountHook(CallbackInfoReturnable<Integer> cir) {
      if (this.isOf(Items.POTION) && (ServerUtil.isHolyWorld() || ServerUtil.isCopyTime())) {
         cir.setReturnValue(64);
      }

   }
}
