package ru.zov.implement.features.draggables.oldcelestial;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Matrix3x2fStack;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

public class OldCelestialPotions extends OldCelestialDraggable {
   private static final float ROW_H = 22.0F;
   private static final float ICON = 18.0F;
   private static final float NAME_SIZE = 9.0F;
   private static final float TIME_SIZE = 8.0F;
   private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

   private record Row(RegistryEntry<StatusEffect> type, String label, String time) {
   }

   public OldCelestialPotions() {
      super("OldCelestialPotions", 120, 22, 0.997F, 0.01F, true, false);
   }

   private static String roman(int level) {
      return level >= 1 && level < ROMAN.length ? ROMAN[level] : String.valueOf(level);
   }

   private List<Row> collect() {
      List<Row> rows = new ArrayList<>();
      for (StatusEffectInstance effect : new ArrayList<>(mc.player.getStatusEffects())) {
         String name = effect.getEffectType().value().getName().getString() + " " + roman(effect.getAmplifier() + 1);
         String time;
         if (effect.isInfinite()) {
            time = "**:**";
         } else {
            int seconds = effect.getDuration() / 20;
            time = (seconds / 60) + ":" + String.format("%02d", seconds % 60);
         }
         rows.add(new Row(effect.getEffectType(), name, time));
      }
      return rows;
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return mc.player != null && (PlayerIntersectionUtil.isChat(mc.currentScreen) || !mc.player.getStatusEffects().isEmpty());
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null) {
         return;
      }

      this.clock();
      List<Row> rows = this.collect();
      if (rows.isEmpty()) {
         rows.add(new Row(StatusEffects.SPEED, "Preview Potion I", "0:00"));
      }

      float textW = 0.0F;
      for (Row row : rows) {
         textW = Math.max(textW, Math.max(textWidth(row.label(), NAME_SIZE), textWidth(row.time(), TIME_SIZE)));
      }

      float w = textW + ICON + 8.0F;
      this.layout(w, rows.size() * ROW_H);

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      float rowTop = y;
      for (Row row : rows) {
         float cy = rowTop + ROW_H / 2.0F;
         context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            InGameHud.getEffectTexture(row.type()),
            (int) (x + w - ICON), (int) (cy - ICON / 2.0F),
            (int) ICON, (int) ICON
         );
         float textRight = x + w - ICON - 6.0F;
         this.text(m, row.label(), textRight - textWidth(row.label(), NAME_SIZE), cy - 4.5F, NAME_SIZE, WHITE);
         this.text(m, row.time(), textRight - textWidth(row.time(), TIME_SIZE), cy + 5.5F, TIME_SIZE, OLD_DIM);
         rowTop += ROW_H;
      }
   }
}
