package ru.zov.implement.screens.menu.components.implement.window.implement.settings.color.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class ColorEditorComponent extends AbstractComponent {
   private final ColorSetting setting;

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + 6.0F), (double)(this.y + 90.5F), (double)31.0F, (double)14.0F).round(1.5F).thickness(2.0F).outlineColor(ColorUtil.getOutline(0.8F)).color(ColorUtil.getRect(1.0F)).build());
      Fonts.getSize(13).drawString(context.getMatrices(), "HEX", (double)(this.x + 10.0F), (double)(this.y + 96.0F), -1);
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + 40.0F), (double)(this.y + 90.5F), (double)80.0F, (double)14.0F).round(1.5F).thickness(2.0F).outlineColor(ColorUtil.getOutline(0.8F)).color(ColorUtil.getRect(1.0F)).build());
      Fonts.getSize(13).drawString(context.getMatrices(), "#" + Integer.toHexString(this.setting.getColor()), (double)(this.x + 45.0F), (double)(this.y + 96.0F), -1);
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + 122.0F), (double)(this.y + 90.5F), (double)22.0F, (double)14.0F).round(1.5F).thickness(2.0F).outlineColor(ColorUtil.getOutline(0.8F)).color(ColorUtil.getRect(1.0F)).build());
      int displayValue = (int)(this.setting.getAlpha() * 100.0F);
      Fonts.getSize(13).drawCenteredString(context.getMatrices(), displayValue + "%", (double)(this.x + 133.0F), (double)(this.y + 96.0F), -1);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)(this.x + 122.0F), (double)(this.y + 90.5F), (double)22.0F, (double)14.0F)) {
         this.setting.setAlpha(MathHelper.clamp((float)((double)this.setting.getAlpha() - amount * (double)2.0F / (double)100.0F), 0.0F, 1.0F));
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public ColorEditorComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
