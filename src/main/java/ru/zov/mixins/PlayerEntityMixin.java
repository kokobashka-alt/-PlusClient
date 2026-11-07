package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.block.PushEvent;
import ru.zov.implement.events.player.SwimmingEvent;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.common.util.rotation.RotationPlan;
import ru.zov.implement.features.modules.movement.AutoSprint;

@Mixin({PlayerEntity.class})
public abstract class PlayerEntityMixin implements QuickImports {
   @Inject(
      method = {"isPushedByFluids"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
      PushEvent event = new PushEvent(PushEvent.Type.WATER);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }

   }

   @WrapOperation(
      method = "knockbackTarget",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
      )
   )
   private void keepSprintVelocity(PlayerEntity instance, Vec3d velocity, Operation<Void> original) {
      AutoSprint sprint = AutoSprint.getInstance();
      if (!sprint.isState() || !sprint.keepSprint.isValue()) {
         original.call(instance, velocity);
      }
   }

   @WrapOperation(
      method = "knockbackTarget",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"
      )
   )
   private void keepSprintState(PlayerEntity instance, boolean sprinting, Operation<Void> original) {
      AutoSprint sprint = AutoSprint.getInstance();
      if (!sprint.isState() || !sprint.keepSprint.isValue()) {
         original.call(instance, sprinting);
      }
   }

   @ModifyExpressionValue(
      method = {"attack"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"
)},
      require = 0
   )
   private float hookFixRotation(float original) {
      return RotationController.INSTANCE.getMoveRotation().getYaw();
   }

   @ModifyExpressionValue(
      method = {"travel"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
)}
   )
   public Vec3d travelHook(Vec3d vec3d) {
      RotationController controller = RotationController.INSTANCE;
      RotationPlan plan = controller.getCurrentRotationPlan();
      if (plan != null && plan.moveCorrection()) {
         vec3d = controller.getMoveRotation().toVector();
      }

      SwimmingEvent event = new SwimmingEvent(vec3d);
      EventManager.callEvent(event);
      return event.getVector();
   }
}
