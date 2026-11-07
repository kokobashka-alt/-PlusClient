package ru.zov.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.screens.menu.components.implement.window.AbstractWindow;
import ru.zov.implement.screens.menu.components.implement.window.implement.settings.color.ColorWindow;

public class ColorComponent extends AbstractSettingComponent {
   private final ColorSetting setting;

   public ColorComponent(ColorSetting setting) {
      super(setting);
      this.setting = setting;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      String wrapped = StringUtil.wrap(this.setting.getDescription(), 100, 12);
      this.height = (float)((int)(18.0F + Fonts.getSize(12).getStringHeight(wrapped) / 3.0F));
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(matrix, this.setting.getName(), (double)(this.x + 9.0F), (double)(this.y + 6.0F), -2828575);
      Fonts.getSize(12).drawString(matrix, wrapped, (double)(this.x + 9.0F), (double)(this.y + 15.0F), -7894892);
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 14.0F), (double)(this.y + 7.0F), (double)7.0F, (double)7.0F).round(3.5F).color(this.setting.getColor()).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 14.0F), (double)(this.y + 7.0F), (double)7.0F, (double)7.0F).round(3.5F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getText()).color(16777215).build());
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)(this.x + this.width - 15.0F), (double)(this.y + 6.7F), (double)7.0F, (double)7.0F) && button == 0) {
         AbstractWindow existingWindow = null;

         for(AbstractWindow window : windowManager.getWindows()) {
            if (window instanceof ColorWindow) {
               existingWindow = window;
               break;
            }
         }

         if (existingWindow != null) {
            windowManager.delete(existingWindow);
         } else {
            AbstractWindow colorWindow = (new ColorWindow(this.setting)).position((float)((int)(mouseX + (double)185.0F)), (float)((int)(mouseY - (double)82.0F))).size(150.0F, 165.0F).draggable(true);
            windowManager.add(colorWindow);
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}
