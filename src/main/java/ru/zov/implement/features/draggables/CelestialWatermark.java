package ru.zov.implement.features.draggables.celestial;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.implement.features.modules.render.CelestialHud;

public class CelestialWatermark extends CelestialDraggable {
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

   public CelestialWatermark() {
      super("CelestialWatermark", 4, 4, 120, 16);
   }

   @Override
   public void drawDraggable(DrawContext context) {
      CelestialHud hud = CelestialHud.getInstance();
      if (hud == null || mc.player == null) {
         return;
      }

      this.clock();
      float size = hud.watermarkSize.getValue();
      String raw = "Celestial » " + mc.getSession().getUsername() + " » UID: 2 » " + LocalTime.now().format(TIME);
      String text = hud.watermarkLower.isValue() ? raw.toLowerCase(Locale.ROOT) : raw.toUpperCase(Locale.ROOT);
      float w = textWidth(text, size) + 12.0F;
      float h = size + 8.0F;
      this.setWidth((int) Math.ceil(w));
      this.setHeight((int) Math.ceil(h));

      Matrix3x2fStack m = context.getMatrices();
      this.panel(m, this.getX(), this.getY(), w, h, 6.0F);
      this.text(m, text, this.getX() + 6.0F, this.getY() + h / 2.0F, size, WHITE);
   }
}
