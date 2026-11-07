package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.player.CrosshairTargetEvent;
import ru.zov.implement.events.render.FovEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.common.util.rotation.RaytracingUtil;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.features.modules.render.CameraTweaks;
import ru.zov.implement.features.modules.combat.Aura;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Inject(method = {"updateFovMultiplier"}, at = {@At("HEAD")}, cancellable = true)
   private void keepSprintVisualFov(CallbackInfo ci) {
      Aura aura = Aura.getInstance();
      if (this.client.player != null && aura != null && aura.shouldHoldSprintVisual(this.client.player)) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"getBasicProjectionMatrix"},
      at = @At(
   value = "INVOKE",
   target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"
)
   )
   public Matrix4f getBasicProjectionMatrixHook(Matrix4f instance, float fovy, float aspect, float zNear, float zFar) {
      CameraTweaks tweaks = CameraTweaks.getInstance();
      if (tweaks.isState() && tweaks.ratioGroup.isValue()) {
         aspect = tweaks.ratio.getValue();
      }

      return instance.perspective(fovy, aspect, zNear, zFar);
   }

   @ModifyExpressionValue(
      method = {"getFov"},
      at = {@At(
   value = "INVOKE",
   target = "Ljava/lang/Integer;intValue()I",
   remap = false
)}
   )
   private int getFovHook(int original) {
      FovEvent event = new FovEvent(original);
      EventManager.callEvent(event);
      return event.getFov();
   }

   @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.client.MinecraftClient client;
   @org.spongepowered.asm.mixin.Shadow public abstract float getFov(net.minecraft.client.render.Camera camera, float tickDelta, boolean changingFov);

   @Inject(
      method = {"renderWorld"},
      at = {@At("TAIL")}
   )
   public void renderWorldHook(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (client.world == null || client.player == null) return;

        net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        float tickDelta = tickCounter.getTickProgress(false);

        MatrixStack viewStack = new MatrixStack();
        viewStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        viewStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        viewStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Render3DUtil.setLastProjMat(client.gameRenderer.getBasicProjectionMatrix(getFov(camera, tickDelta, true)));
        Render3DUtil.setLastModMat(RenderSystem.getModelViewMatrix());
        Render3DUtil.setLastWorldSpaceMatrix(viewStack.peek().copy());
        Render3DUtil.setLastViewMatrix(viewStack.peek().getPositionMatrix());

        MatrixStack matrixStack = new MatrixStack();
        WorldRenderEvent event = new WorldRenderEvent(matrixStack, tickDelta);
        org.joml.Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f projectionMatrix = client.gameRenderer.getBasicProjectionMatrix(getFov(camera, tickDelta, true));
        RenderSystem.backupProjectionMatrix();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        try {
            ru.zov.common.util.render.RenderCompat.modelViewOverride = new Matrix4f();
            RenderSystem.setProjectionMatrix(ru.zov.common.util.render.RenderCompat.allocateUniforms(RenderSystem.PROJECTION_MATRIX_UBO_SIZE, bytes -> {
                com.mojang.blaze3d.buffers.Std140Builder builder = com.mojang.blaze3d.buffers.Std140Builder.intoBuffer(bytes);
                builder.putMat4f(projectionMatrix);
            }), com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
            EventManager.callEvent(event);
            Render3DUtil.onWorldRender();
        } finally {
            ru.zov.common.util.render.RenderCompat.modelViewOverride = null;
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
   }
}
