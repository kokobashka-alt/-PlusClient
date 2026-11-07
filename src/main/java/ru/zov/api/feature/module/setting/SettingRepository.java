package ru.zov.api.feature.module.setting;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import ru.zov.common.trait.Setupable;

public class SettingRepository implements Setupable {
   private final List<Setting> settings = Lists.newArrayList();

   @Override
   public final void setup(Setting... setting) {
      this.settings.addAll(Arrays.asList(setting));
   }

   public Setting get(String name) {
      return this.settings.stream().filter(setting -> setting.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public List<Setting> settings() {
      return this.settings;
   }
}
