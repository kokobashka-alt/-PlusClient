package ru.zov.implement.features.draggables.oldcelestial;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.implement.features.modules.render.OldCelestialHud;

public class OldCelestialWatermark extends OldCelestialDraggable {
   private static final int PILL = 0xF5FFFFFF;
   private static final int TITLE = 0xFF16161A;
   private static final int SUB = 0xFF8A8A92;

   public OldCelestialWatermark() {
      super("OldCelestialWatermark", 120, 28, 0.012F, 0.02F, false, false);
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return mc.player != null;
   }

   @Override
   public void drawDraggable(DrawContext context) {
      OldCelestialHud hud = OldCelestialHud.getInstance();
      if (hud == null || mc.player == null) {
         return;
      }

      this.clock();
      String title = "Celestial";
      String uid = "UID: " + (int) hud.uid.getValue();
      float w = 5.0F + 22.0F + 7.0F + Math.max(textWidth(title, 10.5F), textWidth(uid, 8.0F)) + 12.0F;
      float h = 28.0F;
      this.layout(w, h);

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      this.fill(m, x, y, w, h, 9.0F, PILL);
      // Логотип-розетка из оригинала в проекте отсутствует: рисуем красный круг с буквой.
      this.fill(m, x + 5.0F, y + 3.0F, 22.0F, 22.0F, 11.0F, OLD_RED);
      this.text(m, "C", x + 5.0F + (22.0F - textWidth("C", 11.0F)) / 2.0F, y + 14.0F, 11.0F, WHITE);
      this.text(m, title, x + 34.0F, y + 9.5F, 10.5F, TITLE);
      this.text(m, uid, x + 34.0F, y + 19.5F, 8.0F, SUB);
   }
}
