package ru.zov.mixins;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.render.CameraEvent;
import ru.zov.implement.events.render.CameraPositionEvent;
import ru.zov.common.util.rotation.Angle;
import ru.zov.implement.features.modules.combat.Aura;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   @Final
   private BlockPos.Mutable blockPos;
   @Shadow
   private Vec3d pos;
   @Shadow
   private float yaw;
   @Shadow
   private float pitch;

   @Shadow
   protected abstract void moveBy(float var1, float var2, float var3);

   @Shadow
   public abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract float clipToSpace(float var1);

   @Inject(
      method = {"update"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V",
   shift = Shift.AFTER
)},
      cancellable = true
   )
   private void updateHook(net.minecraft.world.World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      CameraEvent event = new CameraEvent(false, 4.0F, new Angle(this.yaw, this.pitch));
      EventManager.callEvent(event);
      Angle angle = event.getAngle();
      if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity player) {
         if (!player.isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (float)(inverseView ? 180 : 0);
            float distance = event.getDistance();
            this.setRotation(yaw, pitch);
            this.moveBy(event.isCameraClip() ? -distance : -this.clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"setPos(Lnet/minecraft/util/math/Vec3d;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void posHook(Vec3d pos, CallbackInfo ci) {
      CameraPositionEvent event = new CameraPositionEvent(pos);
      EventManager.callEvent(event);
      this.pos = pos = event.getPos();
      this.blockPos.set(pos.x, pos.y, pos.z);
      ci.cancel();
   }

   @Redirect(
      method = {"updateEyeHeight"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getStandingEyeHeight()F")
   )
   private float keepSprintVisualEyeHeight(Entity entity) {
      if (entity instanceof ClientPlayerEntity player) {
         Aura aura = Aura.getInstance();
         if (aura != null && aura.shouldHoldSprintVisual(player)) {
            return aura.visualSprintEyeHeight();
         }
      }
      return entity.getStandingEyeHeight();
   }
}
