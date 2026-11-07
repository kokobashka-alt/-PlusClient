package ru.zov.implement.features.modules.player;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;

public class NoDelay extends Module {
   public final MultiSelectSetting ignoreSetting = new MultiSelectSetting("Type", "Allows the actions you choose")
      .value("Jump", "Right Click", "Break CoolDown");

   public static NoDelay getInstance() {
      return Instance.get(NoDelay.class);
   }

   public NoDelay() {
      super("NoDelay", "No Delay", ModuleCategory.PLAYER);
      this.setup(this.ignoreSetting);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.ignoreSetting.isSelected("Break CoolDown")) {
         mc.interactionManager.blockBreakingCooldown = 0;
      }

      if (this.ignoreSetting.isSelected("Jump")) {
         mc.player.jumpingCooldown = 0;
      }

      if (this.ignoreSetting.isSelected("Right Click")) {
         mc.itemUseCooldown = 0;
      }
   }
}
