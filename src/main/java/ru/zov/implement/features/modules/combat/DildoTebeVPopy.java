package ru.zov.implement.features.modules.combat;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.implement.events.player.TickEvent;

public class DildoTebeVPopy extends Module {
   public DildoTebeVPopy() {
      super("Sosal ?", "Raz Dva Sosal?", ModuleCategory.COMBAT);
      this.setup();
   }

   @EventHandler
   public void onTick(TickEvent e) {
   }
}
