package ru.zov.implement.features.draggables.oldcelestial;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix3x2fStack;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.misc.HPFixer;

public class OldCelestialTarget extends OldCelestialDraggable {
   private static final float LINGER_SECONDS = 1.5F;
   private static final float HEAD = 32.0F;
   private static final float ITEM_SCALE = 0.6F;
   private static final float ITEM_CELL = 10.0F;
   private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   private LivingEntity target;
   private float lastSeen = -100.0F;
   private float hpShown = 1.0F;

   public OldCelestialTarget() {
      super("OldCelestialTarget", 150, 46, 0.41F, 0.55F, false, false);
   }

   private boolean lingering() {
      return mc.player != null && this.target != null && this.target.isAlive() && seconds() - this.lastSeen < LINGER_SECONDS;
   }

   @Override
   public void tick() {
      if (mc.player == null) {
         return;
      }

      LivingEntity detectedTarget = null;

      // 1. Проверяем цель из киллауры
      Aura aura = Aura.getInstance();
      if (aura != null && aura.isState() && aura.getTarget() != null && aura.getTarget().isAlive()) {
         detectedTarget = aura.getTarget();
      } 
      // 2. Если киллаура не активна или нет цели, проверяем игрока под прицелом (Crosshair)
      else if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
         EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
         if (entityHit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            detectedTarget = living;
         }
      }

      if (detectedTarget != null) {
         this.target = detectedTarget;
         this.lastSeen = seconds();
      }
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return mc.player != null && (this.lingering() || PlayerIntersectionUtil.isChat(mc.currentScreen));
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null) {
         return;
      }

      this.clock();
      boolean live = this.lingering();
      LivingEntity shown = live ? this.target : mc.player;
      String name = shown.getName().getString();
      float max = Math.max(1.0F, HPFixer.getMaxHealth(shown));
      float frac = clamp01(HPFixer.getHealth(shown) / max);
      this.hpShown = this.approach(this.hpShown, frac, 10.0F);

      float w = Math.max(150.0F, 44.0F + textWidth(name, 9.5F) + 8.0F + ITEM_CELL * 2.0F + 8.0F);
      float h = 46.0F;
      this.layout(w, h);

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      this.fill(m, x, y, w, h, 5.0F, OLD_BODY);

      float hx = x + 6.0F;
      float hy = y + (h - HEAD) / 2.0F;
      if (shown instanceof PlayerEntity) {
         Render2DUtil.drawTexture(m, Render2DUtil.getHeadId(shown), hx, hy, HEAD, 5.0F, 1, 1, 8, OLD_RED_DARK,
            ColorUtil.multRed(-1, 1.0F + (float) shown.hurtTime / 4.0F));
      } else {
         this.fill(m, hx, hy, HEAD, HEAD, 5.0F, OLD_RED_DARK);
      }

      float tx = x + 44.0F;
      this.text(m, name, tx, y + 11.0F, 9.5F, WHITE);

      float gridX = x + w - 8.0F - ITEM_CELL * 2.0F;
      for (int i = 0; i < ARMOR.length; i++) {
         ItemStack stack = shown.getEquippedStack(ARMOR[i]);
         if (!stack.isEmpty()) {
            this.drawItem(context, stack, gridX + (i % 2) * ITEM_CELL, y + 6.0F + (i / 2) * ITEM_CELL);
         }
      }

      float barY = y + 25.0F;
      float barW = w - 44.0F - 8.0F;
      float barH = 12.0F;
      this.fill(m, tx, barY, barW, barH, 3.0F, OLD_RED_DARK);
      this.fill(m, tx, barY, Math.max(barH, barW * this.hpShown), barH, 3.0F, OLD_RED);
      String percent = String.format(Locale.ROOT, "%.1f%%", frac * 100.0F);
      this.text(m, percent, tx + (barW - textWidth(percent, 7.5F)) / 2.0F, barY + barH / 2.0F, 7.5F, WHITE);
   }

   private void drawItem(DrawContext context, ItemStack stack, float x, float y) {
      Matrix3x2fStack matrix = context.getMatrices();
      matrix.pushMatrix();
      matrix.translate(x, y);
      matrix.scale(ITEM_SCALE, ITEM_SCALE);
      context.drawItem(stack, 0, 0);
      matrix.popMatrix();
   }
}
