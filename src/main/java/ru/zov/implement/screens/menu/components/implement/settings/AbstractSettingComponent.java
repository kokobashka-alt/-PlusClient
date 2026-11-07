package ru.zov.implement.screens.menu.components.implement.settings;

import ru.zov.api.feature.module.setting.Setting;
import ru.zov.implement.screens.menu.components.AbstractComponent;

public abstract class AbstractSettingComponent extends AbstractComponent {
   private final Setting setting;

   public Setting getSetting() {
      return this.setting;
   }

   public AbstractSettingComponent(Setting setting) {
      this.setting = setting;
   }
}
