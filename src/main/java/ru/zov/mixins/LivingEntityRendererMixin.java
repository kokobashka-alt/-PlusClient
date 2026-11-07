package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.render.EntityColorEvent;
import ru.zov.common.util.rotation.RotationController;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState> implements QuickImports {
   @Shadow
   protected abstract @Nullable RenderLayer getRenderLayer(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4);

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
      )
   )
   private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
      if (!translucent && state.width == 0.6F) {
         EntityColorEvent event = new EntityColorEvent(-1);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            translucent = true;
         }
      }

      return this.getRenderLayer(state, showBody, translucent, showOutline);
   }

   @ModifyExpressionValue(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/ColorHelper;mix(II)I"
      )
   )
   private int renderModelHook(int originalColor, @Local(argsOnly = true) LivingEntityRenderState renderState) {
      EntityColorEvent event = new EntityColorEvent(originalColor);
      if (renderState.invisibleToPlayer) {
         EventManager.callEvent(event);
      }
      return event.getColor();
   }

   @ModifyExpressionValue(
      method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"
      )}
   )
   private float lerpAngleDegreesHook(float original, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) float delta) {
      if (entity.equals(mc.player)) {
         RotationController controller = RotationController.INSTANCE;
         return MathHelper.lerpAngleDegrees(delta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
      method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"
      )}
   )
   private float getLerpedPitchHook(float original, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) float delta) {
      if (entity.equals(mc.player)) {
         RotationController controller = RotationController.INSTANCE;
         return MathHelper.lerp(delta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
      } else {
         return original;
      }
   }
}
