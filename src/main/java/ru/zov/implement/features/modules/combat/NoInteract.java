package ru.zov.implement.features.modules.combat;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.other.Instance;

public class NoInteract extends Module {
   public static NoInteract getInstance() {
      return Instance.get(NoInteract.class);
   }

   public NoInteract() {
      super("NoInteract", "No Interact", ModuleCategory.COMBAT);
   }
}
