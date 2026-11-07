package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.common.util.other.Instance;

public class NoRender extends Module {
   public final MultiSelectSetting modeSetting = new MultiSelectSetting("Elements", "Select elements to be ignored")
      .value("Fire", "Bad Effects", "Block Overlay");

   public static NoRender getInstance() {
      return Instance.get(NoRender.class);
   }

   public NoRender() {
      super("NoRender", "No Render", ModuleCategory.RENDER);
      this.setup(this.modeSetting);
   }
}
