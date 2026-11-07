package ru.zov.implement.screens.menu.components.implement.settings.select;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public class SelectedButton extends AbstractComponent {
   private final SelectSetting setting;
   private final String text;
   private float alpha;
   private final Animation alphaAnimation = (new DecelerateAnimation()).setMs(300).setValue((double)0.5F);

   public SelectedButton(SelectSetting setting, String text) {
      this.setting = setting;
      this.text = text;
      this.alphaAnimation.setDirection(Direction.BACKWARDS);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrices = context.getMatrices();
      this.alphaAnimation.setDirection(this.setting.getSelected().contains(this.text));
      float opacity = this.alphaAnimation.getOutput().floatValue();
      int selectedOpacity = ColorUtil.multAlpha(ColorUtil.multAlpha(ColorUtil.getClientColor(), opacity), this.alpha);
      if (!this.alphaAnimation.isFinished(Direction.BACKWARDS)) {
         rectangle.render(ShapeProperties.create(matrices, (double)this.x, (double)this.y, (double)this.width, (double)(this.height + 0.15F)).round(getRound(this.setting.getList(), this.text)).color(selectedOpacity).build());
      }

      Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrices, this.text, (double)(this.x + 4.0F), (double)(this.y + 5.0F), ColorUtil.multAlpha(-2828575, this.alpha));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height) && button == 0) {
         this.setting.setSelected(this.text);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public static Vector4f getRound(List<String> list, String text) {
      if (list.size() == 1) {
         return new Vector4f(4.0F);
      } else if (((String)list.getLast()).contains(text)) {
         return new Vector4f(0.0F, 4.0F, 0.0F, 4.0F);
      } else {
         return ((String)list.getFirst()).contains(text) ? new Vector4f(4.0F, 0.0F, 4.0F, 0.0F) : new Vector4f(0.0F);
      }
   }

   public SelectedButton setAlpha(float alpha) {
      this.alpha = alpha;
      return this;
   }
}
