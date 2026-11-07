package ru.zov.implement.screens.menu.components.implement.window.implement.settings.color.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class AlphaComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean alphaDragging;
   private float X;
   private float Y;
   private float W;
   private float H;

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.X = this.x + 6.0F;
      this.Y = this.y + 83.5F;
      this.W = 138.0F;
      this.H = 4.0F;
      float clampedX = MathHelper.clamp(this.X + this.W * this.setting.getAlpha(), this.X, this.X + this.W - 4.0F);
      float min = MathHelper.clamp(((float)mouseX - this.X) / this.W, 0.0F, 1.0F);
      image.setTexture("textures/alpha.png").render(ShapeProperties.create(matrix, (double)this.X, (double)this.Y, (double)this.W, (double)this.H).build());
      rectangle.render(ShapeProperties.create(matrix, (double)this.X, (double)this.Y - 0.2, (double)this.W + (double)0.5F, (double)this.H).round(1.5F).color(Integer.MIN_VALUE, 134217728, this.setting.getColorWithAlpha(), this.setting.getColorWithAlpha()).build());
      rectangle.render(ShapeProperties.create(matrix, (double)clampedX, (double)this.Y, (double)this.H, (double)this.H).round(this.H / 2.0F).thickness(3.0F).color(16777215).outlineColor(-1).build());
      if (this.alphaDragging) {
         this.setting.setAlpha(min);
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.alphaDragging = button == 0 && MathUtil.isHovered(mouseX, mouseY, (double)this.X, (double)this.Y, (double)this.W, (double)this.H);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.alphaDragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public AlphaComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
