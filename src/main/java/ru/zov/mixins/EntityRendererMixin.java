package ru.zov.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.implement.features.modules.render.EntityESP;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin<S extends EntityRenderState> {
   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderLabelIfPresent(S state, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue commandQueue,
            net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo ci) {
      if (EntityESP.getInstance().isState() && this.canRemove((int)(state.width * 100.0F), EntityESP.getInstance())) {
         ci.cancel();
      }

   }

   @Unique
   private boolean canRemove(int width, EntityESP esp) {
      boolean var10000;
      switch (width) {
         case 60 -> var10000 = esp.entityType.isSelected("Player");
         case 98 -> var10000 = esp.entityType.isSelected("TNT");
         default -> var10000 = false;
      }

      return var10000;
   }
}
