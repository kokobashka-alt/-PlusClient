package ru.zov.implement.features.modules.movement;

import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.implement.events.player.FireworkEvent;
import ru.zov.common.util.rotation.RotationController;

public class FireWorkBooster extends Module {
   private final SelectSetting modeSetting = new SelectSetting("Mode", "Selects the type of mode").value("Grim");

   public FireWorkBooster() {
      super("FireWorkBooster", "FireWork Booster", ModuleCategory.MOVEMENT);
      this.setup(this.modeSetting);
   }

   @EventHandler
   @Native
   public void onFirework(FireworkEvent e) {
      if (this.modeSetting.isSelected("Grim")) {
         int yawAcceleration = RotationController.INSTANCE.getRotation().getYaw() > 0.0F ? 45 : -45;
         double acceleration = Math.abs((RotationController.INSTANCE.getRotation().getYaw() + yawAcceleration) % 90.0F - yawAcceleration) / 45.0F;
         double boost = 1.0 + 0.3 * acceleration * acceleration;
         boolean yAcceleration = Math.abs(RotationController.INSTANCE.getRotation().getPitch()) > 60.0F;
         e.setVector(e.getVector().multiply(boost, yAcceleration ? boost : 0.0, boost));
      }
   }
}
