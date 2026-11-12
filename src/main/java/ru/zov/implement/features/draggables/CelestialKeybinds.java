package ru.zov.implement.features.draggables.celestial;

import java.util.ArrayList;
import java.util.List;
import ru.zov.api.feature.module.Module;
import ru.zov.common.util.other.StringUtil;

public class CelestialKeybinds extends CelestialListPanel {
   public CelestialKeybinds() {
      super("CelestialKeybinds", "Keybinds", 0.0F, 0.481F);
   }

   @Override
   protected Row preview() {
      return new Row("p", "Preview Bind", null, 0, "[Key]", WHITE);
   }

   @Override
   protected List<Row> collect() {
      List<Row> out = new ArrayList<>();
      for (Module module : main.getModuleProvider().getModules()) {
         if (module.isState() && module.getKey() != -1) {
            out.add(new Row(module.getName(), module.getVisibleName(), null, 0, "[" + StringUtil.getBindName(module.getKey()) + "]", WHITE));
         }
      }

      return out;
   }
}
