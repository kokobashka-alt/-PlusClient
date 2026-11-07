package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.nexusguard.UserProfile;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;

public class Watermark extends AbstractDraggable {
   private int fpsCount = 0;

   public Watermark() {
      super("Watermark", 10, 10, 92, 16, true);
   }

   public void tick() {
      this.fpsCount = (int)MathUtil.interpolate((float)this.fpsCount, (float)mc.getCurrentFps());
   }

   @Native
   public void drawDraggable(DrawContext e) {
      org.joml.Matrix3x2fStack matrix = e.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      String offset = "      ";
      String var10000 = "PLUS CLIENT";
      String name = var10000 + offset;
      String version = "Beta user" + offset;
      String fps = this.fpsCount + " FPS";
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)this.getHeight()).round(3.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      font.drawGradientString(matrix, name, (double)(this.getX() + 5), (double)((float)this.getY() + 6.5F), ColorUtil.fade(0), ColorUtil.fade(100));
      font.drawString(matrix, version + fps, (double)((float)this.getX() + font.getStringWidth(name) + 5.0F), (double)((float)this.getY() + 6.5F), ColorUtil.getText());
      rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() + font.getStringWidth(name)), (double)(this.getY() + 4), (double)0.5F, (double)(this.getHeight() - 8)).color(ColorUtil.getOutline(0.75F, 0.5F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() + font.getStringWidth(name + version)), (double)(this.getY() + 4), (double)0.5F, (double)(this.getHeight() - 8)).color(ColorUtil.getOutline(0.75F, 0.5F)).build());
      this.setWidth((int)(font.getStringWidth(name + version + fps) + 9.0F));
   }
}
