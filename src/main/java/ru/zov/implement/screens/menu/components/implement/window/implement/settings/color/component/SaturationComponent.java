package ru.zov.implement.screens.menu.components.implement.window.implement.settings.color.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class SaturationComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean saturationDragging;
   private float X;
   private float Y;
   private float W;
   private float H;

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.X = this.x + 6.0F;
      this.Y = this.y + 73.5F;
      this.W = 138.0F;
      this.H = 4.0F;
      float clampedX = MathHelper.clamp(this.X + this.W * this.setting.getHue(), this.X, this.X + this.W - 4.0F);
      float min = MathHelper.clamp(((float)mouseX - this.X) / this.W, 0.0F, 1.0F);
      image.setTexture("textures/hue.png").render(ShapeProperties.create(matrix, (double)this.X, (double)this.Y + (double)0.5F, (double)this.W, (double)(this.H - 1.0F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)clampedX, (double)this.Y, (double)this.H, (double)this.H).round(this.H / 2.0F).thickness(3.0F).color(16777215).outlineColor(-1).build());
      if (this.saturationDragging) {
         this.setting.setHue(min);
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.saturationDragging = button == 0 && MathUtil.isHovered(mouseX, mouseY, (double)this.X, (double)this.Y, (double)this.W, (double)this.H);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.saturationDragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public SaturationComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
