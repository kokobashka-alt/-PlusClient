package ru.zov.implement.screens.menu.components.implement.settings.multiselect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.screens.menu.components.implement.settings.AbstractSettingComponent;

public class MultiSelectComponent extends AbstractSettingComponent {
   private final List<MultiSelectedButton> multiSelectedButtons = new ArrayList();
   private final MultiSelectSetting setting;
   private boolean open;
   private float dropdownListX;
   private float dropDownListY;
   private float dropDownListWidth;
   private float dropDownListHeight;
   private final Animation alphaAnimation = (new DecelerateAnimation()).setMs(300).setValue((double)1.0F);

   public MultiSelectComponent(MultiSelectSetting setting) {
      super(setting);
      this.setting = setting;
      this.alphaAnimation.setDirection(Direction.BACKWARDS);

      for(String s : setting.getList()) {
         this.multiSelectedButtons.add(new MultiSelectedButton(setting, s));
      }

   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrices = context.getMatrices();
      String wrapped = StringUtil.wrap(this.setting.getDescription(), 45, 12);
      this.height = (float)((int)(18.0F + Fonts.getSize(12).getStringHeight(wrapped) / 3.0F));
      List<String> fullSettingsList = this.setting.getList();
      this.dropdownListX = this.x + this.width - 75.0F;
      this.dropDownListY = this.y + 20.0F;
      this.dropDownListWidth = 66.0F;
      this.dropDownListHeight = (float)(fullSettingsList.size() * 12);
      this.alphaAnimation.setDirection(this.open ? Direction.FORWARDS : Direction.BACKWARDS);
      this.renderSelected(matrices);
      if (!this.alphaAnimation.isFinished(Direction.BACKWARDS)) {
         this.renderSelectList(context, mouseX, mouseY, delta);
      }

      Fonts.getSize(14, Fonts.Type.BOLD).drawString(matrices, this.setting.getName(), (double)(this.x + 9.0F), (double)(this.y + 6.0F), -2828575);
      Fonts.getSize(12).drawString(matrices, wrapped, (double)(this.x + 9.0F), (double)(this.y + 15.0F), -7894892);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (MathUtil.isHovered(mouseX, mouseY, (double)(this.x + this.width - 75.0F), (double)(this.y + 4.0F), (double)66.0F, (double)14.0F)) {
            this.open = !this.open;
         } else if (this.open && !this.isHoveredList(mouseX, mouseY)) {
            this.open = false;
         }

         if (this.open) {
            this.multiSelectedButtons.forEach((selectedButton) -> selectedButton.mouseClicked(mouseX, mouseY, button));
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean isHover(double mouseX, double mouseY) {
      return this.open && this.isHoveredList(mouseX, mouseY);
   }

   private void renderSelected(org.joml.Matrix3x2fStack matrix) {
      FontRenderer font = Fonts.getSize(12);
      int x1 = (int)(this.x + this.width - 72.0F);
      rectangle.render(ShapeProperties.create(matrix, (double)(x1 - 3), (double)(this.y + 4.0F), (double)66.0F, (double)14.0F).round(2.0F).thickness(2.0F).softness(0.5F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(0.5F)).build());
      String selectedName = String.join(", ", this.setting.getSelected());
      float offset = 64.0F;
      ScissorManager scissor = main.getScissorManager();
      scissor.push(matrix, (float)(x1 - 2), (float)window.getScaledHeight() / 2.0F - 96.0F, 64.0F, 220.0F);
      font.drawStringWithScroll(matrix, selectedName, (double)x1, (double)(this.y + 10.0F), offset, ColorUtil.getText());
      scissor.pop();
      if (font.getStringWidth(selectedName) - offset > 0.0F) {
         rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - 13.75F), (double)(this.y + 5.0F), (double)4.0F, (double)12.0F).round(3.0F).color(ColorUtil.getGuiRectColor(0.0F), ColorUtil.getGuiRectColor(0.0F), ColorUtil.getGuiRectColor(1.0F), ColorUtil.getGuiRectColor(1.0F)).build());
         rectangle.render(ShapeProperties.create(matrix, (double)((float)x1 - 2.25F), (double)(this.y + 5.0F), (double)4.0F, (double)12.0F).round(3.0F).color(ColorUtil.getGuiRectColor(1.0F), ColorUtil.getGuiRectColor(1.0F), ColorUtil.getGuiRectColor(0.0F), ColorUtil.getGuiRectColor(0.0F)).build());
      }

   }

   private void renderSelectList(DrawContext context, int mouseX, int mouseY, float delta) {
      float opacity = this.alphaAnimation.getOutput().floatValue();
      rectangle.render(ShapeProperties.create(context.getMatrices(), (double)this.dropdownListX, (double)this.dropDownListY, (double)this.dropDownListWidth, (double)this.dropDownListHeight).round(4.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline(opacity, 1.0F)).color(ColorUtil.getGuiRectColor(opacity)).build());
      float offset = this.dropDownListY;

      for(MultiSelectedButton button : this.multiSelectedButtons) {
         button.x = this.dropdownListX;
         button.y = offset;
         button.width = this.dropDownListWidth;
         button.height = 12.0F;
         button.setAlpha(opacity);
         button.render(context, mouseX, mouseY, delta);
         offset += 12.0F;
      }

   }

   private boolean isHoveredList(double mouseX, double mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, (double)this.dropdownListX, (double)(this.dropDownListY - 16.0F), (double)this.dropDownListWidth, (double)(this.dropDownListHeight + 16.0F));
   }
}
