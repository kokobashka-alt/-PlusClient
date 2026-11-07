package ru.zov.implement.screens.menu.components.implement.settings;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;

public class ValueComponent extends AbstractSettingComponent {
   public static final int SLIDER_WIDTH = 45;
   private final ValueSetting setting;
   private boolean dragging;
   private double animation;

   public ValueComponent(ValueSetting setting) {
      super(setting);
      this.setting = setting;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      String wrapped = StringUtil.wrap(this.setting.getDescription(), 70, 12);
      this.height = (float)((int)(18.0F + Fonts.getSize(12).getStringHeight(wrapped) / 3.0F));
      String value = String.valueOf(this.setting.getValue());
      Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrix, value, (double)(this.x + this.width - 9.0F - Fonts.getSize(12).getStringWidth(value)), (double)(this.y + 8.0F), ColorUtil.getClientColor());
      this.changeValue(this.getDifference(mouseX, matrix));
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(matrix, this.setting.getName(), (double)(this.x + 9.0F), (double)(this.y + 6.0F), -2828575);
      Fonts.getSize(12).drawString(matrix, wrapped, (double)(this.x + 9.0F), (double)(this.y + 15.0F), -7894892);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.dragging = MathUtil.isHovered(mouseX, mouseY, (double)(this.x + this.width - 45.0F - 9.0F), (double)(this.y + 13.0F), (double)45.0F, (double)4.0F) && button == 0;
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private float getDifference(int mouseX, org.joml.Matrix3x2fStack matrix) {
      float percentValue = 45.0F * (this.setting.getValue() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin());
      float difference = MathHelper.clamp((float)mouseX - (this.x + this.width - 45.0F - 9.0F), 0.0F, 45.0F);
      this.animation = MathUtil.interpolate(this.animation, (double)percentValue);
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 45.0F - 9.0F), (double)(this.y + 15.0F), (double)45.0F, (double)1.0F).color(758006093).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 45.0F - 9.0F), (double)(this.y + 15.0F), (double)((float)this.animation), (double)1.0F).color(ColorUtil.getClientColor(), ColorUtil.getClientColor(), (new Color(ColorUtil.getClientColor())).darker().getRGB(), (new Color(ColorUtil.getClientColor())).darker().getRGB()).build());
      float v = MathHelper.clamp((float)((double)(this.x + this.width - 45.0F) + this.animation), 0.0F, this.x + this.width - 4.0F);
      rectangle.render(ShapeProperties.create(matrix, (double)(v - 10.0F), (double)(this.y + 12.5F), (double)6.0F, (double)6.0F).round(3.0F).color(ColorUtil.getMainGuiColor()).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(v - 8.8F), (double)(this.y + 13.5F), (double)4.0F, (double)4.0F).round(2.0F).color(ColorUtil.getClientColor()).build());
      return difference;
   }

   private void changeValue(float difference) {
      BigDecimal bd = BigDecimal.valueOf((double)(difference / 45.0F * (this.setting.getMax() - this.setting.getMin()) + this.setting.getMin())).setScale(2, RoundingMode.HALF_UP);
      if (this.dragging) {
         float value = difference == 0.0F ? this.setting.getMin() : bd.floatValue();
         if (this.setting.isInteger()) {
            value = (float)((int)value);
         }

         this.setting.setValue(value);
      }

   }
}
