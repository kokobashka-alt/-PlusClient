package ru.zov.implement.features.draggables.oldcelestial;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.module.Module;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.other.StringUtil;

public class OldCelestialKeybinds extends OldCelestialDraggable {
   private static final float HEAD_H = 17.0F;
   private static final float ROW_H = 12.5F;
   private static final float PAD = 7.0F;
   private static final float TEXT = 8.5F;

   public OldCelestialKeybinds() {
      super("OldCelestialKeybinds", 120, 40, 0.045F, 0.17F, false, false);
   }

   private List<String[]> collect() {
      List<String[]> rows = new ArrayList<>();
      for (Module module : main.getModuleProvider().getModules()) {
         if (module.isState() && module.getKey() != -1) {
            rows.add(new String[]{module.getVisibleName(), "[" + StringUtil.getBindName(module.getKey()) + "]"});
         }
      }
      return rows;
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return mc.player != null && (PlayerIntersectionUtil.isChat(mc.currentScreen) || !this.collect().isEmpty());
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null) {
         return;
      }

      this.clock();
      List<String[]> rows = this.collect();
      if (rows.isEmpty()) {
         rows.add(new String[]{"Preview Bind", "[Key]"});
      }

      float w = 120.0F;
      for (String[] row : rows) {
         w = Math.max(w, PAD * 2.0F + textWidth(row[0], TEXT) + 14.0F + textWidth(row[1], TEXT));
      }

      float h = HEAD_H + rows.size() * ROW_H + 4.0F;
      this.layout(w, h);

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      this.fill(m, x, y, w, h, 4.0F, OLD_BODY);
      this.fill(m, x, y, w, 2.0F, 1.0F, OLD_RED);
      this.text(m, "Keybinds", x + (w - textWidth("Keybinds", 9.5F)) / 2.0F, y + HEAD_H / 2.0F + 1.0F, 9.5F, WHITE);

      float cy = y + HEAD_H + ROW_H / 2.0F;
      for (String[] row : rows) {
         this.text(m, row[0], x + PAD, cy, TEXT, WHITE);
         this.text(m, row[1], x + w - PAD - textWidth(row[1], TEXT), cy, TEXT, WHITE);
         cy += ROW_H;
      }
   }
}
