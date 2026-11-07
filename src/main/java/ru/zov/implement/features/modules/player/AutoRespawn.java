package ru.zov.implement.features.modules.player;

import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.implement.events.player.DeathScreenEvent;

public class AutoRespawn extends Module {
   public AutoRespawn() {
      super("AutoRespawn", "Auto Respawn", ModuleCategory.PLAYER);
   }

   @EventHandler
   @Native
   public void onDeathScreen(DeathScreenEvent e) {
      mc.player.requestRespawn();
      mc.setScreen(null);
   }
}
