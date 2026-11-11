package ru.zov.implement.features.modules.render;

import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.FogParticleField;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.render.WorldRenderEvent;

public class Ambience extends Module {
   private static final long NANOS_WRAP = 100000000000L;
   private static final double NANOS_PER_SECOND = 1.0E9;
   private static final float FADE_SECONDS = 0.35F;
   private static final float MIN_FOG_TIME = 0.002F;
   private static final float MIN_SPAN = 0.004F;
   private static final int MAX_PARTICLES = FogParticleField.MAX_COUNT;
   private static final double STREAK_HALF_WIDTH = 0.012;
   private static final int STREAK_RGB = 0xD6C4B2;

   // UNKNOWN: the block list that starts the fade-in is not in the supplied Ambience.java
   // (Client.ambienceTimeBlockList / trackedBlockPresent). While empty, fogTime never rises.
   private static final Set<Block> TRIGGER_BLOCKS = Set.of();

   public final ValueSetting densitySetting = new ValueSetting("Fog Density", "Opacity of the fog streaks")
      .setValue(50.0F)
      .range(0.0F, 100.0F);
   public final ValueSetting particlesSetting = new ValueSetting("Particles", "Amount of fog streaks around the camera")
      .setValue(25.0F)
      .range(0, 100);

   private long lastFrameNanos;
   private float fogTime;

   public static Ambience getInstance() {
      return Instance.get(Ambience.class);
   }

   public Ambience() {
      super("Ambience", "Ambience", ModuleCategory.RENDER);
      this.setup(this.densitySetting, this.particlesSetting);
   }

   @Override
   public void deactivate() {
      this.fogTime = 0.0F;
      this.lastFrameNanos = 0L;
   }

   public float getFogTime() {
      return this.fogTime;
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.updateFogTime();
      this.spawnFogParticles();
   }

   private void updateFogTime() {
      long now = System.nanoTime();
      float step = 1.0F;
      if (this.lastFrameNanos != 0L) {
         step = (float)((double)(now - this.lastFrameNanos) / NANOS_PER_SECOND) / FADE_SECONDS;
      }

      this.lastFrameNanos = now;
      if (mc.player == null || mc.world == null) {
         this.fogTime = 0.0F;
         return;
      }

      BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      BlockState state = mc.world.getBlockState(feet);
      float signedStep = TRIGGER_BLOCKS.contains(state.getBlock()) ? step : -step;
      this.fogTime = MathHelper.clamp(this.fogTime + signedStep, 0.0F, 1.0F);
   }

   private void spawnFogParticles() {
      if (!(MIN_FOG_TIME < this.fogTime)) {
         return;
      }

      float span = (float)((double)this.densitySetting.getValue() / 100.0) * this.fogTime;
      if (!(MIN_SPAN < span)) {
         return;
      }

      int count = (int)(this.particlesSetting.getValue() * (float)MAX_PARTICLES / 100.0F + 0.5F);
      int tint = Math.max(0, Math.min(255, Math.round(span * 255.0F)));
      int color = tint << 24 | STREAK_RGB;
      float timeSeed = (float)((double)Math.floorMod(System.nanoTime(), NANOS_WRAP) * 1.0E-9);
      Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();

      FogParticleField.generate(camera.x, camera.y, camera.z, count, timeSeed, (x, yTop, yBottom, z) -> {
         Render3DUtil.drawQuad(
            new Vec3d(x, yTop, z + STREAK_HALF_WIDTH),
            new Vec3d(x, yBottom, z + STREAK_HALF_WIDTH),
            new Vec3d(x, yBottom, z - STREAK_HALF_WIDTH),
            new Vec3d(x, yTop, z - STREAK_HALF_WIDTH),
            color,
            true
         );
         Render3DUtil.drawQuad(
            new Vec3d(x - STREAK_HALF_WIDTH, yTop, z),
            new Vec3d(x - STREAK_HALF_WIDTH, yBottom, z),
            new Vec3d(x + STREAK_HALF_WIDTH, yBottom, z),
            new Vec3d(x + STREAK_HALF_WIDTH, yTop, z),
            color,
            true
         );
      });
   }
}
