package ru.zov.implement.features.draggables.celestial;

import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;

public abstract class CelestialDraggable extends AbstractDraggable {
   protected static final int BODY = 0xFF0A0A0C;
   protected static final int BORDER = 0x90A582EC;
   protected static final int WHITE = 0xFFFFFFFF;
   protected static final int RED = 0xFFFF4A4A;
   protected static final int GREEN = 0xFF55FF7A;
   protected static final int YELLOW = 0xFFFFD84A;
   protected static final int TRACK = 0xFF1A1A20;
   protected static final float HEAD = 19.0F;
   protected static final float ROW = 14.0F;
   protected static final float PANEL_W = 130.0F;
   protected static final float R = 7.0F;

   private static final float TEXT_SCALE = 1.6F;
   private static final float[] GA = {0.88F, 0.78F, 1.00F};
   private static final float[] GB = {0.66F, 0.47F, 0.95F};
   private static final float[] GC = {0.46F, 0.27F, 0.84F};
   private static final long START_NS = System.nanoTime();

   protected float dt;
   protected float time;
   private long lastNs;

   protected CelestialDraggable(String name, int x, int y, int width, int height) {
      super(name, x, y, width, height, true);
   }

   protected static float seconds() {
      return (System.nanoTime() - START_NS) / 1.0E9F;
   }

   protected void clock() {
      long now = System.nanoTime();
      this.dt = this.lastNs == 0L ? 0.016F : Math.min(0.05F, (now - this.lastNs) / 1.0E9F);
      this.lastNs = now;
      this.time += this.dt;
   }

   protected static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
   }

   protected static float easeOutCubic(float t) {
      float u = 1.0F - clamp01(t);
      return 1.0F - u * u * u;
   }

   protected float lin(float cur, float target, float duration) {
      float step = this.dt / duration;
      return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
   }

   protected float approach(float cur, float target, float speed) {
      float v = cur + (target - cur) * (1.0F - (float) Math.exp(-speed * this.dt));
      return Math.abs(target - v) < 0.0005F ? target : v;
   }

   protected static int gcol(float u) {
      u = clamp01(u);
      float[] from = u < 0.5F ? GA : GB;
      float[] to = u < 0.5F ? GB : GC;
      float t = u < 0.5F ? u * 2.0F : (u - 0.5F) * 2.0F;
      int r = Math.round((from[0] + (to[0] - from[0]) * t) * 255.0F);
      int g = Math.round((from[1] + (to[1] - from[1]) * t) * 255.0F);
      int b = Math.round((from[2] + (to[2] - from[2]) * t) * 255.0F);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   protected static int withAlpha(int rgb, float alpha01) {
      return (Math.round(255.0F * clamp01(alpha01)) << 24) | (rgb & 0xFFFFFF);
   }

   protected static FontRenderer font(float size) {
      return Fonts.getSize(Math.max(1, Math.round(size * TEXT_SCALE)), Fonts.Type.BOLD);
   }

   protected static float textWidth(String text, float size) {
      return font(size).getStringWidth(text);
   }

   protected float text(Matrix3x2fStack m, String text, float x, float centerY, float size, int color) {
      FontRenderer font = font(size);
      font.drawString(m, text, x, centerY - font.getStringHeight(text) / 2.0F, color);
      return font.getStringWidth(text);
   }

   protected void fill(Matrix3x2fStack m, float x, float y, float w, float h, float round, int color) {
      rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).color(color).build());
   }

   protected void glow(Matrix3x2fStack m, float x, float y, float w, float h, float round, int argb) {
      rectangle.render(ShapeProperties.create(m, x - 5.0F, y - 5.0F, w + 10.0F, h + 10.0F).round(round + 5.0F).softness(10.0F).color(argb).build());
   }

   protected void panel(Matrix3x2fStack m, float x, float y, float w, float h, float round) {
      this.glow(m, x, y, w, h, round, withAlpha(gcol(0.5F), 0x78 / 255.0F));
      blur.render(ShapeProperties.create(m, x, y, w, h).round(round).softness(1.0F).thickness(1.0F).outlineColor(BORDER).color(BODY).build());
   }

   protected void gradient(Matrix3x2fStack m, float x, float y, float w, float h, float round) {
      float phase = 0.5F + 0.5F * (float) Math.sin(this.time * 0.8F);
      int left = gcol(phase * 0.5F);
      int right = gcol(0.5F + phase * 0.5F);
      rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).color(left, left, right, right).build());
   }
}
