package ru.zov.implement.screens.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.implement.screens.menu.MenuScreen;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class BackgroundComponent extends AbstractComponent {
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)this.height).round(6.0F).softness(1.0F).thickness(2.0F).quality(50.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getMainGuiColor()).build());
      rectangle.render(ShapeProperties.create(context.getMatrices(), (double)(this.x + 85.0F), (double)this.y, (double)0.5F, (double)this.height).color(ColorUtil.getOutline(0.5F, 1.0F)).build());
      rectangle.render(ShapeProperties.create(context.getMatrices(), (double)(this.x + 85.5F), (double)(this.y + 28.0F), (double)(this.width - 85.5F), (double)0.5F).color(ColorUtil.getOutline(0.5F, 1.0F)).build());
      Fonts.getSize(16).drawString(matrix, MenuScreen.INSTANCE.getCategory().getReadableName(), (double)(this.x + 95.0F), (double)(this.y + 13.0F), -2828575);
   }
}
