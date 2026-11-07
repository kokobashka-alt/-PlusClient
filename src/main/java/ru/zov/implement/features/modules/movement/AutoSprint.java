package ru.zov.implement.features.modules.movement;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;

public class AutoSprint extends Module {
   public final StopWatch stopWatch = new StopWatch();
   public final BooleanSetting ignoreHunger = new BooleanSetting("Ignore Hunger", "Keep sprint before impact, thus not slowing you down").setValue(true);
   public final BooleanSetting keepSprint = new BooleanSetting("Keep Sprint", "Keep sprint before impact, thus not slowing you down").setValue(true);

   public static AutoSprint getInstance() {
      return Instance.get(AutoSprint.class);
   }

   public AutoSprint() {
      super("AutoSprint", "Auto Sprint", ModuleCategory.MOVEMENT);
      this.setup(this.ignoreHunger, this.keepSprint);
   }
}
