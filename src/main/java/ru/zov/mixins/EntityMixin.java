package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.player.BoundingBoxControlEvent;
import ru.zov.implement.events.player.PlayerVelocityStrafeEvent;
import ru.zov.implement.events.player.PushAwayEvent;

@Mixin({Entity.class})
public abstract class EntityMixin implements QuickImports {
   @Shadow
   private Box boundingBox;

   @Shadow
   public abstract boolean equals(Object var1);

   @Redirect(
      method = {"updateVelocity"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"
)
   )
   public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
      if ((Object)this == mc.player) {
         PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
         EventManager.callEvent(event);
         return event.getVelocity();
      } else {
         return Entity.movementInputToVelocity(movementInput, speed, yaw);
      }
   }

   @Inject(
      method = {"getBoundingBox"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
      BoundingBoxControlEvent event = new BoundingBoxControlEvent(this.boundingBox, (Entity)(Object)this);
      EventManager.callEvent(event);
      cir.setReturnValue(event.getBox());
   }

   @ModifyExpressionValue(
      method = {"isLogicalSideForUpdatingMovement"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"
)}
   )
   public boolean isControlledByPlayerHook(boolean original) {
      return (Object)this == mc.player ? false : original;
   }

   @Redirect(
      method = {"pushAwayFrom"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V",
   ordinal = 0
)
   )
   public void pushAwayFromHook(Entity instance, double deltaX, double deltaY, double deltaZ) {
      PushAwayEvent event = new PushAwayEvent(deltaX, deltaZ);
      if (instance.equals(mc.player)) {
         EventManager.callEvent(event);
      }

      if (!event.isCancelled()) {
         instance.addVelocity(event.getX(), deltaY, event.getZ());
      }

   }

   @Redirect(
      method = {"pushAwayFrom"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V",
   ordinal = 1
)
   )
   public void pushAwayFromHook2(Entity instance, double deltaX, double deltaY, double deltaZ) {
      PushAwayEvent event = new PushAwayEvent(deltaX, deltaZ);
      if (instance.equals(mc.player)) {
         EventManager.callEvent(event);
      }

      if (!event.isCancelled()) {
         instance.addVelocity(event.getX(), deltaY, event.getZ());
      }

   }
}
