package ru.zov.implement.screens.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class CheckComponent extends AbstractComponent {
   private boolean state;
   private Runnable runnable;
   private final Animation alphaAnimation = (new DecelerateAnimation()).setMs(300).setValue((double)255.0F);
   private final Animation stencilAnimation = (new DecelerateAnimation()).setMs(200).setValue((double)8.0F);

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.alphaAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      this.stencilAnimation.setDirection(this.state ? Direction.FORWARDS : Direction.BACKWARDS);
      int stateColor = this.state ? ColorUtil.getClientColor() : ColorUtil.getGuiRectColor(1.0F);
      int outlineStateColor = this.state ? ColorUtil.getClientColor() : ColorUtil.getOutline();
      int opacity = this.alphaAnimation.getOutput().intValue();
      rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)8.0F, (double)8.0F).round(1.5F).thickness(2.0F).softness(0.5F).outlineColor(outlineStateColor).color(MathUtil.applyOpacity(stateColor, (float)opacity)).build());
      ScissorManager scissor = main.getScissorManager();
      scissor.push(matrix, this.x, (float)window.getScaledHeight() / 2.0F - 96.0F, (float)this.stencilAnimation.getOutput().intValue(), 220.0F);
      image.setTexture("textures/check.png").render(ShapeProperties.create(matrix, (double)(this.x + 2.0F), (double)this.y + (double)2.5F, (double)4.0F, (double)3.0F).color(MathUtil.applyOpacity(-1, (float)opacity)).build());
      scissor.pop();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)8.0F, (double)8.0F) && button == 0) {
         this.runnable.run();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public CheckComponent setState(boolean state) {
      this.state = state;
      return this;
   }

   public CheckComponent setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }
}
