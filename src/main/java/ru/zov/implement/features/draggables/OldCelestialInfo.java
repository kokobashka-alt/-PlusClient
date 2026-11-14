package ru.zov.implement.features.draggables.oldcelestial;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix3x2fStack;
import ru.zov.common.util.entity.MovingUtil;

public class OldCelestialInfo extends OldCelestialDraggable {
   private static final float SIZE = 8.5F;
   private static final float LINE_H = 11.0F;

   public OldCelestialInfo() {
      super("OldCelestialInfo", 110, 24, 0.004F, 0.993F, false, true);
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return mc.player != null;
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null) {
         return;
      }

      this.clock();
      BlockPos pos = mc.player.getBlockPos();
      String bps = "BPS: " + String.format(Locale.ROOT, "%.2f", MovingUtil.getSpeedSqrt(mc.player) * 20.0);
      String xyz = "XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
      this.layout(Math.max(textWidth(bps, SIZE), textWidth(xyz, SIZE)), LINE_H * 2.0F);

      float x = this.getX();
      float y = this.getY();
      Matrix3x2fStack m = context.getMatrices();
      this.text(m, bps, x, y + LINE_H / 2.0F, SIZE, WHITE);
      this.text(m, xyz, x, y + LINE_H * 1.5F, SIZE, WHITE);
   }
}
