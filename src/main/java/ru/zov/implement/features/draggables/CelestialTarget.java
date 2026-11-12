package ru.zov.implement.features.draggables.celestial;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix3x2fStack;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.features.modules.render.CelestialHud;

public class CelestialTarget extends CelestialDraggable {
   private static final float LINGER_SECONDS = 1.5F;

   private LivingEntity target;
   private float lastSeen = -100.0F;
   private float hpShown = 1.0F;
   private float hpTrail = 1.0F;

   private boolean placed;

   public CelestialTarget() {
      // QuickImports must not be touched in the constructor (runs before the render system exists); see ensurePlaced().
      super("CelestialTarget", 0, 0, 132, 46);
   }

   @Override
   public void setX(int x) {
      super.setX(x);
      this.placed = true;
   }

   @Override
   public void setY(int y) {
      super.setY(y);
      this.placed = true;
   }

   private void ensurePlaced() {
      if (this.placed) {
         return;
      }

      this.placed = true;
      super.setX((int) (window.getScaledWidth() * 0.348F));
      super.setY((int) (window.getScaledHeight() * 0.22F));
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return this.isLingering() || this.previewWanted();
   }

   @Override
   public void tick() {
      if (mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
         this.target = living;
         this.lastSeen = seconds();
      }
   }

   private boolean isLingering() {
      return mc.player != null && this.target != null && seconds() - this.lastSeen < LINGER_SECONDS;
   }

   private boolean previewWanted() {
      CelestialHud hud = CelestialHud.getInstance();
      return mc.player != null && (PlayerIntersectionUtil.isChat(mc.currentScreen) || hud != null && hud.targetTest.isValue());
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null) {
         return;
      }

      this.clock();
      boolean live = this.isLingering();
      LivingEntity shown = live ? this.target : mc.player;
      String name = live ? shown.getName().getString() : "Player";
      float health = live ? shown.getHealth() + shown.getAbsorptionAmount() : 20.0F;
      float frac = live ? clamp01(shown.getHealth() / Math.max(1.0F, shown.getMaxHealth())) : 1.0F;
      String hpText = String.format(Locale.ROOT, "HP: %.1f", health);
      boolean player = shown instanceof PlayerEntity;

      float w = Math.max(132.0F, 43.0F + textWidth(name, 11.0F) + 10.0F);
      float h = 46.0F;
      this.hpShown = this.approach(this.hpShown, frac, 10.0F);
      this.hpTrail = this.approach(this.hpTrail, frac, 3.0F);
      this.setWidth((int) Math.ceil(w));
      this.setHeight((int) h);

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      this.panel(m, x, y, w, h, R);

      float headSize = 32.0F;
      float hx = x + 7.0F;
      float hy = y + (h - headSize) / 2.0F;
      if (player) {
         Render2DUtil.drawTexture(m, Render2DUtil.getHeadId(shown), hx, hy, headSize, 7.0F, 1, 1, 8, BODY, ColorUtil.multRed(-1, 1.0F + (float) shown.hurtTime / 4.0F));
      } else {
         String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
         this.fill(m, hx, hy, headSize, headSize, 7.0F, 0xFF18181E);
         this.text(m, letter, hx + (headSize - textWidth(letter, 16.0F)) / 2.0F, hy + headSize / 2.0F, 16.0F, WHITE);
      }

      float tx = x + 43.0F;
      this.text(m, name, tx, y + 12.0F, 11.0F, WHITE);
      this.text(m, hpText, tx, y + 24.0F, 8.0F, 0xFFE8E0FA);

      float barY = y + 31.0F;
      float barW = w - 43.0F - 8.0F;
      float barH = 9.0F;
      this.fill(m, tx, barY, barW, barH, 4.5F, TRACK);
      if (this.hpTrail > this.hpShown) {
         this.fill(m, tx, barY, Math.max(barH, barW * this.hpTrail), barH, 4.5F, 0x66FFFFFF);
      }

      this.gradient(m, tx, barY, Math.max(barH, barW * this.hpShown), barH, 4.5F);
   }
}
