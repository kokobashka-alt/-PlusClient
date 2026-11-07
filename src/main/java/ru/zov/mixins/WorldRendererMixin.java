package ru.zov.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.render.Render3DUtil;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
   @Shadow
   private @Nullable ClientWorld world;

   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(method = "renderMain", at = @At("HEAD"))
   private void onRenderMain(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Matrix4f matrix, GpuBufferSlice fogBuffer, boolean renderBlockOutline, WorldRenderState state, RenderTickCounter renderTickCounter, Profiler profiler, CallbackInfo ci) {
      ProjectionUtil.frustum = frustum;
      this.blockHighLight();
   }

   @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
   private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, double cameraX, double cameraY, double cameraZ, OutlineRenderState outlineRenderState, int color, float lineWidth, CallbackInfo ci) {
      ci.cancel();
   }

   @Unique
   private void blockHighLight() {
      if (this.world == null) {
         return;
      }

      HitResult target = this.client.crosshairTarget;
      if (!(target instanceof BlockHitResult result) || result.getType() != HitResult.Type.BLOCK) {
         return;
      }

      BlockPos pos = result.getBlockPos();
      VoxelShape shape = this.world.getBlockState(pos).getOutlineShape(this.world, pos);
      if (result.isAgainstWorldBorder()) {
         Render3DUtil.drawShape(pos, shape, ColorUtil.getFriendColor(), 2.0F, true, false);
      } else {
         Render3DUtil.drawShapeAlternative(pos, shape, ColorUtil.getClientColor(), 2.0F, true, true);
      }
   }
}
