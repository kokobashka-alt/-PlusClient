package ru.zov.implement.screens.menu.components.implement.window.implement.settings.color.component;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class HueComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean hueDragging;
   private float X;
   private float Y;
   private float W;
   private float H;

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.X = this.x + 6.0F;
      this.Y = this.y + 18.5F;
      this.W = 138.0F;
      this.H = 50.0F;
      int[] color = new int[]{-16777216, -1, -16777216, Color.HSBtoRGB(this.setting.getHue(), 1.0F, 1.0F)};
      rectangle.render(ShapeProperties.create(matrix, (double)this.X, (double)this.Y, (double)this.W, (double)this.H).round(2.0F).color(color).build());
      float clampedX = MathHelper.clamp(this.X + this.W * this.setting.getSaturation(), this.X, this.X + this.W - 5.0F);
      float clampedY = MathHelper.clamp(this.Y + this.H * (1.0F - this.setting.getBrightness()), this.Y, this.Y + this.H - 5.0F);
      rectangle.render(ShapeProperties.create(matrix, (double)clampedX, (double)clampedY, (double)5.0F, (double)5.0F).round(2.5F).softness(1.0F).thickness(3.0F).color(16777215).outlineColor(-1).build());
      float min = MathHelper.clamp(((float)mouseX - this.X) / this.W, 0.0F, 1.0F);
      if (this.hueDragging) {
         this.setting.setBrightness(MathHelper.clamp(1.0F - ((float)mouseY - this.Y) / this.H, 0.0F, 1.0F));
         this.setting.setSaturation(min);
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.hueDragging = button == 0 && MathUtil.isHovered(mouseX, mouseY, (double)this.X, (double)this.Y, (double)this.W, (double)this.H);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.hueDragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public HueComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
