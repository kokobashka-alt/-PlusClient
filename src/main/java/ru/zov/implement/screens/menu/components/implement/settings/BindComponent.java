package ru.zov.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;

public class BindComponent extends AbstractSettingComponent {
   private final BindSetting setting;
   private boolean binding;

   public BindComponent(BindSetting setting) {
      super(setting);
      this.setting = setting;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      String bindName = StringUtil.getBindName(this.setting.getKey());
      String name = this.binding ? "(" + bindName + ") ..." : bindName;
      float stringWidth = Fonts.getSize(13, Fonts.Type.BOLD).getStringWidth(name) - 2.0F;
      String wrapped = StringUtil.wrap(this.setting.getDescription(), (int)(this.width - stringWidth - 28.0F), 12);
      this.height = (float)((int)(18.0F + Fonts.getSize(12).getStringHeight(wrapped) / 3.0F));
      rectangle.render(ShapeProperties.create(matrix, (double)(this.x + this.width - stringWidth - 17.0F), (double)(this.y + 5.0F), (double)(stringWidth + 10.0F), (double)12.0F).round(2.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1.0F)).build());
      int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);
      Fonts.getSize(13, Fonts.Type.BOLD).drawString(matrix, name, (double)(this.x + this.width - 12.0F - stringWidth - 1.0F), (double)this.y + (double)9.5F, bindingColor);
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(matrix, this.setting.getName(), (double)(this.x + 9.0F), (double)(this.y + 6.0F), -2828575);
      Fonts.getSize(12).drawString(matrix, wrapped, (double)(this.x + 9.0F), (double)(this.y + 15.0F), -7894892);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height)) {
            this.binding = !this.binding;
         } else {
            this.binding = false;
         }
      }

      if (this.binding && button > 1) {
         this.setting.setKey(button);
         this.binding = false;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      int key = keyCode == 261 ? -1 : keyCode;
      if (this.binding) {
         this.setting.setKey(key);
         this.binding = false;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }
}
