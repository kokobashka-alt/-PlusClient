package ru.zov.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.screens.menu.components.implement.other.CheckComponent;

public class CheckboxComponent extends AbstractSettingComponent {
   private final CheckComponent checkComponent = new CheckComponent();
   private final BooleanSetting setting;

   public CheckboxComponent(BooleanSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      String wrapped = StringUtil.wrap(this.setting.getDescription(), 100, 12);
      this.height = (int)(18.0F + Fonts.getSize(12).getStringHeight(wrapped) / 3.0F);
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), this.setting.getName(), this.x + 9.0F, this.y + 6.0F, -2828575);
      Fonts.getSize(12).drawString(context.getMatrices(), wrapped, this.x + 9.0F, this.y + 15.0F, -7894892);
      ((CheckComponent)this.checkComponent.position(this.x + this.width - 16.0F, this.y + 7.5F))
         .setRunnable(() -> this.setting.setValue(!this.setting.isValue()))
         .setState(this.setting.isValue())
         .render(context, mouseX, mouseY, delta);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.checkComponent.mouseClicked(mouseX, mouseY, button);
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
