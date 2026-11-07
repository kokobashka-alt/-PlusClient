package ru.zov.implement.screens.menu.components.implement.window.implement;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.screens.menu.components.implement.window.AbstractWindow;

public abstract class AbstractBindWindow extends AbstractWindow {
   private boolean binding;

   protected abstract int getKey();

   protected abstract void setKey(int var1);

   protected abstract int getType();

   protected abstract void setType(int var1);

   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)this.height).round(4.0F).softness(25.0F).color(838860800).build());
      rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)this.height).round(4.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline(0.8F, 1.0F)).color(ColorUtil.getRect(1.0F)).build());
      Fonts.getSize(14).drawString(matrix, "Binding module", (double)(this.x + 5.0F), (double)(this.y + 8.0F), -1);
      image.setTexture("textures/trash.png").render(ShapeProperties.create(matrix, (double)(this.x + this.width - 13.0F), (double)(this.y + 5.3F), (double)8.0F, (double)8.0F).build());
      this.drawKeyButton(matrix);
      this.drawTypeButton(matrix);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (MathUtil.isHovered(mouseX, mouseY, (double)(this.x + this.width - 57.0F), (double)(this.y + 37.0F), (double)52.0F, (double)13.0F)) {
            this.setType(this.getType() != 1 ? 1 : 0);
         }

         float stringWidth = Fonts.getSize(14).getStringWidth(StringUtil.getBindName(this.getKey()));
         if (MathUtil.isHovered(mouseX, mouseY, (double)(this.x + this.width - stringWidth - 15.0F), (double)(this.y + 18.8F), (double)(stringWidth + 10.0F), (double)13.0F)) {
            this.binding = !this.binding;
         }

         if (MathUtil.isHovered(mouseX, mouseY, (double)(this.x + this.width - 13.0F), (double)(this.y + 5.3F), (double)8.0F, (double)8.0F)) {
            this.setKey(-1);
         }
      }

      if (this.binding && button > 1) {
         this.setKey(button);
         this.binding = false;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      int key = keyCode == 261 ? -1 : keyCode;
      if (this.binding) {
         this.setKey(key);
         this.binding = false;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void drawKeyButton(org.joml.Matrix3x2fStack matrix) {
      float stringWidth = Fonts.getSize(14).getStringWidth(StringUtil.getBindName(this.getKey()));
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - stringWidth - 15.0F), (double)(this.y + 18.8F), (double)(stringWidth + 10.0F), (double)13.0F).round(2.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline(0.8F, 1.0F)).color(ColorUtil.getOutline(0.1F, 1.0F)).build());
      int bindingColor = this.binding ? -8288257 : -2828575;
      Fonts.getSize(14).drawString(matrix, StringUtil.getBindName(this.getKey()), (double)(this.x + this.width - 10.0F - stringWidth), (double)(this.y + 23.6F), bindingColor);
      Fonts.getSize(14).drawString(matrix, "Key", (double)((int)(this.x + 5.0F)), (double)((int)((double)this.y + 24.3)), -2828575);
   }

   private void drawTypeButton(org.joml.Matrix3x2fStack matrix) {
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 57.0F), (double)(this.y + 37.0F), (double)52.0F, (double)13.0F).round(2.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline(0.8F, 1.0F)).color(ColorUtil.getOutline(0.1F, 1.0F)).build());
      if (this.getType() == 1) {
         rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 34.0F), (double)(this.y + 37.0F), (double)29.0F, (double)13.0F).round(2.0F, 2.0F, 0.0F, 0.0F).color(-8288257).build());
      } else {
         rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 57.0F), (double)(this.y + 37.0F), (double)23.0F, (double)13.0F).round(0.0F, 0.0F, 2.0F, 2.0F).color(-8288257).build());
      }

      Fonts.getSize(12).drawString(matrix, "HOLD", (double)(this.x + 52.0F), (double)this.y + 42.3, -2828575);
      Fonts.getSize(12).drawString(matrix, "TOGGLE", (double)(this.x + 73.0F), (double)this.y + 42.3, -2828575);
      Fonts.getSize(14).drawString(matrix, "Bind mode", (double)((int)(this.x + 5.0F)), (double)((int)(this.y + 42.3F)), -2828575);
   }
}
