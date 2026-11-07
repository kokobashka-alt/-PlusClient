package ru.zov.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.module.setting.implement.ButtonSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.screens.menu.components.implement.other.ButtonComponent;

public class SButtonComponent extends AbstractSettingComponent {
   private final ButtonComponent buttonComponent = new ButtonComponent();
   private final ButtonSetting setting;

   public SButtonComponent(ButtonSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      String wrapped = StringUtil.wrap(this.setting.getDescription(), 80, 12);
      this.height = (int)(18.0F + Fonts.getSize(12).getStringHeight(wrapped) / 3.0F);
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), this.setting.getName(), this.x + 9.0F, this.y + 6.0F, -2828575);
      Fonts.getSize(12).drawString(context.getMatrices(), wrapped, this.x + 9.0F, this.y + 15.0F, -7894892);
      ((ButtonComponent)this.buttonComponent
            .setText("Click on me")
            .setRunnable(this.setting.getRunnable())
            .position(this.x + this.width - 9.0F - this.buttonComponent.width, this.y + 5.0F))
         .render(context, mouseX, mouseY, delta);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.buttonComponent.mouseClicked(mouseX, mouseY, button);
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
