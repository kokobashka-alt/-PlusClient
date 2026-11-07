package ru.zov.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.screens.menu.components.implement.other.CheckComponent;
import ru.zov.implement.screens.menu.components.implement.other.SettingComponent;
import ru.zov.implement.screens.menu.components.implement.window.AbstractWindow;
import ru.zov.implement.screens.menu.components.implement.window.implement.settings.group.GroupWindow;

public class GroupComponent extends AbstractSettingComponent {
   private final CheckComponent checkComponent = new CheckComponent();
   private final SettingComponent settingComponent = new SettingComponent();
   private final GroupSetting setting;

   public GroupComponent(GroupSetting setting) {
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
      ((SettingComponent)this.settingComponent.position(this.x + this.width - 28.0F, this.y + 8.0F))
         .setRunnable(() -> this.spawnWindow(mouseX, mouseY))
         .render(context, mouseX, mouseY, delta);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.checkComponent.mouseClicked(mouseX, mouseY, button);
      this.settingComponent.mouseClicked(mouseX, mouseY, button);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void spawnWindow(int mouseX, int mouseY) {
      AbstractWindow existingWindow = null;

      for (AbstractWindow window : windowManager.getWindows()) {
         if (window instanceof GroupWindow && ((GroupWindow)window).getSetting() == this.setting) {
            existingWindow = window;
            break;
         }
      }

      if (existingWindow != null) {
         windowManager.delete(existingWindow);
      } else {
         AbstractWindow groupWindow = new GroupWindow(this.setting).position(mouseX + 5, mouseY + 5).size(137.0F, 23.0F).draggable(false);
         windowManager.add(groupWindow);
      }
   }
}
