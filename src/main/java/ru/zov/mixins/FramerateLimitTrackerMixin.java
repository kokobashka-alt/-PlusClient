package ru.zov.mixins;

import net.minecraft.client.option.InactivityFpsLimiter;
import net.minecraft.client.option.InactivityFpsLimiter.LimitReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
public abstract class FramerateLimitTrackerMixin {
   @Inject(method = "getLimitReason", at = @At("RETURN"), cancellable = true)
   private void zov$removeAfkThrottle(CallbackInfoReturnable<LimitReason> cir) {
      LimitReason reason = cir.getReturnValue();
      if (reason == LimitReason.SHORT_AFK || reason == LimitReason.LONG_AFK) {
         cir.setReturnValue(LimitReason.NONE);
      }
   }
}
