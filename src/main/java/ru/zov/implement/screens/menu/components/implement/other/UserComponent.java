package ru.zov.implement.screens.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import ru.nexusguard.UserProfile;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class UserComponent extends AbstractComponent {
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + 5.0F), (double)(this.y - 30.0F), (double)75.0F, (double)25.0F).round(4.0F).thickness(2.0F).softness(0.5F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(0.5F)).build());
      ScissorManager scissor = main.getScissorManager();
      scissor.push(matrix, this.x + 5.5F, this.y - 29.5F, 74.0F, 22.0F);
Fonts.getSize(12).drawString(matrix, "PlusUser", (double)(this.x + 11.0F), (double)(this.y - 21.0F), -2828575);
Fonts.getSize(10).drawGradientString(matrix, "Beta", (double)(this.x + 11.0F), (double)this.y - (double)14.5F, ColorUtil.fade(0), ColorUtil.fade(60));
      scissor.pop();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
