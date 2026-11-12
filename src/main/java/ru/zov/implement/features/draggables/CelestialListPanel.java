package ru.zov.implement.features.draggables.celestial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

/**
 * Shared base for the Celestial list panels (Keybinds, Potions, Cooldowns, Staff): a titled panel with one animated
 * row per entry, the name (and an optional small tag) on the left and a value on the right. Subclasses only decide
 * which rows exist. The constructor ratios are the initial position as a fraction of the scaled window size.
 */
public abstract class CelestialListPanel extends CelestialDraggable {
   private static final float TEXT_SIZE = 8.5F;
   private static final float TITLE_SIZE = 9.0F;
   private static final float PAD = 7.0F;
   private static final float FADE_SECONDS = 0.25F;

   protected static final class Row {
      final String id;
      final String name;
      final String tag;
      final int tagColor;
      final String value;
      final int valueColor;

      Row(String id, String name, String tag, int tagColor, String value, int valueColor) {
         this.id = id;
         this.name = name;
         this.tag = tag;
         this.tagColor = tagColor;
         this.value = value;
         this.valueColor = valueColor;
      }
   }

   private static final class Slot {
      Row row;
      float t;
      boolean present;
   }

   private final String title;
   private final Map<String, Slot> slots = new LinkedHashMap<>();

   protected CelestialListPanel(String name, String title, float xRatio, float yRatio) {
      super(name, (int) (window.getScaledWidth() * xRatio), (int) (window.getScaledHeight() * yRatio), (int) PANEL_W, (int) HEAD);
      this.title = title;
   }

   protected abstract Row preview();

   protected abstract List<Row> collect();

   @Override
   public boolean visible() {
      return mc.player != null && mc.world != null && (!this.slots.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen));
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) {
         this.slots.clear();
         return;
      }

      for (Slot slot : this.slots.values()) {
         slot.present = false;
      }

      for (Row row : this.collect()) {
         Slot slot = this.slots.computeIfAbsent(row.id, k -> new Slot());
         slot.row = row;
         slot.present = true;
      }
   }

   private static float rowWidth(Row row) {
      float w = PAD + textWidth(row.name, TEXT_SIZE) + PAD + textWidth(row.value, TEXT_SIZE) + 8.0F;
      if (row.tag != null) {
         w += textWidth(row.tag, TEXT_SIZE - 1.0F) + 5.0F;
      }

      return w;
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      this.clock();
      for (Iterator<Slot> it = this.slots.values().iterator(); it.hasNext(); ) {
         Slot slot = it.next();
         slot.t = this.lin(slot.t, slot.present ? 1.0F : 0.0F, FADE_SECONDS);
         if (!slot.present && slot.t <= 0.0F) {
            it.remove();
         }
      }

      List<Slot> shown = new ArrayList<>(this.slots.values());
      if (shown.isEmpty()) {
         if (!PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return;
         }

         Slot preview = new Slot();
         preview.row = this.preview();
         preview.t = 1.0F;
         preview.present = true;
         shown.add(preview);
      }

      float w = Math.max(PANEL_W, PAD * 2.0F + textWidth(this.title, TITLE_SIZE));
      float rowsHeight = 0.0F;
      for (Slot slot : shown) {
         w = Math.max(w, rowWidth(slot.row));
         rowsHeight += ROW * easeOutCubic(slot.t);
      }

      float h = HEAD + rowsHeight + 3.0F;
      this.setWidth((int) Math.ceil(w));
      this.setHeight((int) Math.ceil(h));

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      this.panel(m, x, y, w, h, R);
      this.text(m, this.title, x + PAD, y + HEAD / 2.0F, TITLE_SIZE, WHITE);
      this.gradient(m, x + 5.0F, y + HEAD - 1.0F, w - 10.0F, 1.0F, 0.0F);

      float ry = y + HEAD;
      for (Slot slot : shown) {
         float e = easeOutCubic(slot.t);
         float rowH = ROW * e;
         if (e >= 0.35F) {
            float a = clamp01((e - 0.35F) / 0.65F);
            Row row = slot.row;
            float cy = ry + rowH / 2.0F;
            float nameW = this.text(m, row.name, x + PAD, cy, TEXT_SIZE, withAlpha(WHITE, a));
            if (row.tag != null) {
               this.text(m, row.tag, x + PAD + nameW + 4.0F, cy, TEXT_SIZE - 1.0F, withAlpha(row.tagColor, a));
            }

            this.text(m, row.value, x + w - PAD - textWidth(row.value, TEXT_SIZE), cy, TEXT_SIZE, withAlpha(row.valueColor, a));
         }

         ry += rowH;
      }
   }
}
