package ru.zov.implement.screens.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class SettingComponent extends AbstractComponent {
   private Runnable runnable;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      image.setTexture("textures/settings.png").render(ShapeProperties.create(context.getMatrices(), this.x, this.y, 7.0, 7.0).color(-5263172).build());
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x, this.y, 7.0, 7.0) && button == 0) {
         this.runnable.run();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public SettingComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }
}
