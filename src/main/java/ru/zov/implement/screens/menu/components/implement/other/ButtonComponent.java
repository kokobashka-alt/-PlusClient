package ru.zov.implement.screens.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class ButtonComponent extends AbstractComponent {
   private String text;
   private Runnable runnable;
   private int color = -8288257;

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.width = Fonts.getSize(12).getStringWidth(this.text) + 13.0F;
      this.height = 12.0F;
      rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)this.height).round(2.0F).color(this.color).build());
      Fonts.getSize(12, Fonts.Type.BOLD).drawCenteredString(matrix, this.text, (double)this.x + (double)this.width / (double)2.0F, (double)(this.y + 5.0F), -1);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height) && button == 0) {
         this.runnable.run();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public ButtonComponent setText(String text) {
      this.text = text;
      return this;
   }

   public ButtonComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public ButtonComponent setColor(int color) {
      this.color = color;
      return this;
   }
}
