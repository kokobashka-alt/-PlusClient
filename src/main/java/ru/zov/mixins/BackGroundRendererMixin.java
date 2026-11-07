package ru.zov.mixins;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import ru.zov.api.event.EventManager;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.implement.events.render.FogEvent;
import ru.zov.implement.features.modules.render.NoRender;

@Mixin({FogRenderer.class})
public class BackGroundRendererMixin {
   @org.spongepowered.asm.mixin.injection.Redirect(method = {"getFogColor", "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogModifier;shouldApply(Lnet/minecraft/block/enums/CameraSubmersionType;Lnet/minecraft/entity/Entity;)Z"), require = 0)
   private static boolean onShouldApply(net.minecraft.client.render.fog.FogModifier modifier, net.minecraft.block.enums.CameraSubmersionType submersionType, net.minecraft.entity.Entity entity) {
      NoRender noRender = NoRender.getInstance();
      if (noRender.isState()) {
         if (modifier instanceof net.minecraft.client.render.fog.BlindnessEffectFogModifier && noRender.modeSetting.isSelected("Bad Effects")) {
            return false;
         }
      }
      return modifier.shouldApply(submersionType, entity);
   }

   @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
   private static void getFogColorHook(Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
      FogEvent event = new FogEvent();
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         int color = event.getColor();
         cir.setReturnValue(new Vector4f(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color)));
      }
   }

   @org.spongepowered.asm.mixin.injection.ModifyVariable(
           method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
           at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuDevice;createCommandEncoder()Lcom/mojang/blaze3d/systems/CommandEncoder;"),
           ordinal = 0
   )
   private static net.minecraft.client.render.fog.FogData modifyFogData(net.minecraft.client.render.fog.FogData fogData) {
      FogEvent event = new FogEvent();
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         float end = Math.max(1.0F, event.getDistance());
         float start = Math.max(0.0F, Math.min(end - 0.5F, end * 0.05F));
         fogData.environmentalStart = start;
         fogData.environmentalEnd = end;
         fogData.renderDistanceStart = start;
         fogData.renderDistanceEnd = end;
         fogData.skyEnd = end;
         fogData.cloudEnd = end;
      }
      return fogData;
   }
}
