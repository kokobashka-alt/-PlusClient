package ru.zov.api.feature.module.setting;

import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ButtonSetting;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.TextSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.implement.screens.menu.components.implement.settings.AbstractSettingComponent;
import ru.zov.implement.screens.menu.components.implement.settings.BindComponent;
import ru.zov.implement.screens.menu.components.implement.settings.CheckboxComponent;
import ru.zov.implement.screens.menu.components.implement.settings.ColorComponent;
import ru.zov.implement.screens.menu.components.implement.settings.GroupComponent;
import ru.zov.implement.screens.menu.components.implement.settings.SButtonComponent;
import ru.zov.implement.screens.menu.components.implement.settings.TextComponent;
import ru.zov.implement.screens.menu.components.implement.settings.ValueComponent;
import ru.zov.implement.screens.menu.components.implement.settings.multiselect.MultiSelectComponent;
import ru.zov.implement.screens.menu.components.implement.settings.select.SelectComponent;

public class SettingComponentAdder {
   @Native
   public void addSettingComponent(List<Setting> settings, List<AbstractSettingComponent> components) {
      settings.forEach(setting -> {
         if (setting instanceof BooleanSetting booleanSetting) {
            components.add(new CheckboxComponent(booleanSetting));
         }

         if (setting instanceof BindSetting bindSetting) {
            components.add(new BindComponent(bindSetting));
         }

         if (setting instanceof ColorSetting colorSetting) {
            components.add(new ColorComponent(colorSetting));
         }

         if (setting instanceof TextSetting textSetting) {
            components.add(new TextComponent(textSetting));
         }

         if (setting instanceof ValueSetting valueSetting) {
            components.add(new ValueComponent(valueSetting));
         }

         if (setting instanceof GroupSetting groupSetting) {
            components.add(new GroupComponent(groupSetting));
         }

         if (setting instanceof ButtonSetting buttonSetting) {
            components.add(new SButtonComponent(buttonSetting));
         }

         if (setting instanceof SelectSetting selectSetting) {
            components.add(new SelectComponent(selectSetting));
         }

         if (setting instanceof MultiSelectSetting multiSelectSetting) {
            components.add(new MultiSelectComponent(multiSelectSetting));
         }
      });
   }
}
