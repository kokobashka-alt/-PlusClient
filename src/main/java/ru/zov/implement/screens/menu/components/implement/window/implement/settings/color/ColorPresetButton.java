package ru.zov.implement.screens.menu.components.implement.window.implement.settings.color;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class ColorPresetButton extends AbstractComponent {
   private final ColorSetting setting;
   private final int color;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      rectangle.render(ShapeProperties.create(context.getMatrices(), this.x, this.y, 8.0, 8.0).round(2.0F).color(this.color).build());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, 8.0, 8.0) && button == 0) {
         this.setting.setColor(this.color);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public ColorPresetButton(ColorSetting setting, int color) {
      this.setting = setting;
      this.color = color;
   }
}
