package ru.zov.implement.screens.menu.components.implement.settings.multiselect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;
import ru.zov.implement.screens.menu.components.implement.settings.select.SelectedButton;

public class MultiSelectedButton extends AbstractComponent {
   private final MultiSelectSetting setting;
   private final String text;
   private float alpha;
   private final Animation alphaAnimation = (new DecelerateAnimation()).setMs(300).setValue((double)0.5F);

   public MultiSelectedButton(MultiSelectSetting setting, String text) {
      this.setting = setting;
      this.text = text;
      this.alphaAnimation.setDirection(Direction.BACKWARDS);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.alphaAnimation.setDirection(this.setting.getSelected().contains(this.text));
      float opacity = this.alphaAnimation.getOutput().floatValue();
      int selectedOpacity = ColorUtil.multAlpha(ColorUtil.getClientColor(), opacity * this.alpha);
      if (!this.alphaAnimation.isFinished(Direction.BACKWARDS)) {
         rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)(this.height + 0.15F)).round(SelectedButton.getRound(this.setting.getList(), this.text)).color(selectedOpacity).build());
      }

      Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrix, this.text, (double)(this.x + 4.0F), (double)(this.y + 5.0F), ColorUtil.multAlpha(-2828575, this.alpha));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height) && button == 0) {
         List<String> selected = new ArrayList(this.setting.getSelected());
         if (selected.contains(this.text)) {
            selected.remove(this.text);
         } else {
            selected.add(this.text);
            this.sortSelectedAccordingToList(selected, this.setting.getList());
         }

         this.setting.setSelected(selected);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void sortSelectedAccordingToList(List<String> selected, List<String> list) {
      Objects.requireNonNull(list);
      selected.sort(Comparator.comparingInt(list::indexOf));
   }

   public MultiSelectedButton setAlpha(float alpha) {
      this.alpha = alpha;
      return this;
   }
}
