package ru.zov.implement.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.modules.render.betterrain.RainField;

/**
 * Better Rain: streaks of falling drops around the camera, wind drift, and splash rings where drops hit the ground.
 * The simulation lives in {@link RainField}; this class only feeds it the world and draws the result.
 *
 * Not included yet: hiding the vanilla rain columns (needs a mixin into the weather renderer, method name not yet
 * verified in this build) and the wet-ground layer from the original module.
 */
public class BetterRain extends Module {
   private static final double MAX_FRAME_SECONDS = 0.1;
   private static final double STREAK_TIME = 0.032;
   private static final float STREAK_WIDTH = 1.0F;
   private static final double SPLASH_MAX_DISTANCE = 16.0;
   private static final int RING_SEGMENTS = 16;
   private static final double SPLASH_START_RADIUS = 0.05;
   private static final double SPLASH_END_RADIUS = 0.26;
   private static final float SPLASH_ALPHA = 0.5F;
   private static final long WIND_PERIOD_MS = 600000L;

   private final ValueSetting densitySetting = new ValueSetting("Density", "Количество капель")
      .setValue(0.8F)
      .range(0.1F, 3.0F);
   private final ValueSetting speedSetting = new ValueSetting("Speed", "Скорость падения")
      .setValue(1.0F)
      .range(0.2F, 3.0F);
   private final ValueSetting sizeSetting = new ValueSetting("Drop size", "Длина штриха")
      .setValue(1.0F)
      .range(0.5F, 2.5F);
   private final ValueSetting radiusSetting = new ValueSetting("Radius", "Радиус вокруг камеры")
      .setValue(16.0F)
      .range(8.0F, 32.0F);
   private final ValueSetting windSetting = new ValueSetting("Wind", "Сила ветра")
      .setValue(0.2F)
      .range(0.0F, 1.0F);
   private final ValueSetting opacitySetting = new ValueSetting("Opacity", "Прозрачность капель")
      .setValue(0.5F)
      .range(0.1F, 1.0F);
   private final BooleanSetting splashSetting = new BooleanSetting("Splash", "Круги от падения капель")
      .setValue(true);
   private final ColorSetting tintSetting = new ColorSetting("Tint", "Цвет дождя")
      .value(0xFFCFE6FF);

   private final RainField field = new RainField(System.nanoTime());
   private final RainField.Frame frame = new RainField.Frame();
   private final RainField.Ground ground = (x, z) -> {
      if (mc.world == null) {
         return Double.NaN;
      }

      int top = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
      return top <= mc.world.getBottomY() ? Double.NaN : top;
   };
   private final RainField.Impact noImpact = (x, y, z) -> {
   };
   private long lastNanos;

   public BetterRain() {
      super("BetterRain", "Better Rain", ModuleCategory.RENDER);
      this.setup(
         this.densitySetting,
         this.speedSetting,
         this.sizeSetting,
         this.radiusSetting,
         this.windSetting,
         this.opacitySetting,
         this.splashSetting,
         this.tintSetting
      );
   }

   @Override
   public void deactivate() {
      this.field.clear();
      this.lastNanos = 0L;
      super.deactivate();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (!this.isState() || mc.player == null || mc.world == null || mc.gameRenderer == null) {
         return;
      }

      long nowNanos = System.nanoTime();
      double dt = this.lastNanos == 0L ? 0.016 : Math.min(MAX_FRAME_SECONDS, (nowNanos - this.lastNanos) / 1.0E9);
      this.lastNanos = nowNanos;
      long nowMs = System.currentTimeMillis();

      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
      double[] wind = RainField.wind((nowMs % WIND_PERIOD_MS) / 1000.0, this.windSetting.getValue());
      RainField.Frame f = this.frame;
      f.camX = cam.x;
      f.camY = cam.y;
      f.camZ = cam.z;
      f.radius = this.radiusSetting.getValue();
      f.rain = mc.world.getRainGradient(e.partialTicks());
      f.density = this.densitySetting.getValue();
      f.speedSetting = this.speedSetting.getValue();
      f.opacity = this.opacitySetting.getValue();
      f.windX = wind[0];
      f.windZ = wind[1];

      this.field.step(f, dt, this.ground, this.noImpact, nowMs);
      MatrixStack.Entry entry = e.stack().peek();
      this.drawDrops(entry, wind[0], wind[1]);
      if (this.splashSetting.isValue()) {
         this.drawSplashes(entry, cam, nowMs);
      }
   }

   private void drawDrops(MatrixStack.Entry entry, double windX, double windZ) {
      int tint = this.tintSetting.getColor() & 0xFFFFFF;
      double size = this.sizeSetting.getValue();
      int count = this.field.dropCount();
      for (int i = 0; i < count; i++) {
         double alpha = this.field.alpha(i);
         if (alpha < 0.01) {
            continue;
         }

         double speed = this.field.speed(i);
         Vec3d head = new Vec3d(this.field.x(i), this.field.y(i), this.field.z(i));
         Vec3d tail = new Vec3d(
            head.x - windX * STREAK_TIME * size,
            head.y + speed * STREAK_TIME * size,
            head.z - windZ * STREAK_TIME * size
         );
         int color = ColorUtil.multAlpha(0xFF000000 | tint, (float) alpha);
         Render3DUtil.drawLine(entry, head, tail, color, STREAK_WIDTH, true);
      }
   }

   private void drawSplashes(MatrixStack.Entry entry, Vec3d cam, long nowMs) {
      int tint = this.tintSetting.getColor() & 0xFFFFFF;
      int slots = this.field.splashSlots();
      for (int i = 0; i < slots; i++) {
         double age = this.field.splashAge(i, nowMs);
         if (age < 0.0) {
            continue;
         }

         double sx = this.field.splashX(i);
         double sy = this.field.splashY(i);
         double sz = this.field.splashZ(i);
         double dx = sx - cam.x;
         double dz = sz - cam.z;
         if (dx * dx + dz * dz > SPLASH_MAX_DISTANCE * SPLASH_MAX_DISTANCE) {
            continue;
         }

         double radius = SPLASH_START_RADIUS + (SPLASH_END_RADIUS - SPLASH_START_RADIUS) * age;
         float alpha = (float) (SPLASH_ALPHA * (1.0 - age));
         int color = ColorUtil.multAlpha(0xFF000000 | tint, alpha);
         double[][] ring = RainField.ring(sx, sy, sz, radius, RING_SEGMENTS);
         for (int k = 0; k < RING_SEGMENTS; k++) {
            Vec3d a = new Vec3d(ring[k][0], ring[k][1], ring[k][2]);
            Vec3d b = new Vec3d(ring[k + 1][0], ring[k + 1][1], ring[k + 1][2]);
            Render3DUtil.drawLine(entry, a, b, color, STREAK_WIDTH, true);
         }
      }
   }
}
