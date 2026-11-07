package ru.zov.implement.features.modules.movement;

import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.implement.events.player.SwimmingEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.common.util.rotation.RotationController;

public class WaterSpeed extends Module {
   private final SelectSetting modeSetting = new SelectSetting("Mode", "Select bypass mode").value("FunTime");

   public WaterSpeed() {
      super("WaterSpeed", "Water Speed", ModuleCategory.MOVEMENT);
      this.setup(this.modeSetting);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (this.modeSetting.isSelected("FunTime") && mc.player.isSwimming() && mc.player.isOnGround()) {
         mc.player.jump();
         mc.player.velocity.y = 0.1;
      }
   }

   @EventHandler
   @Native
   public void onSwimming(SwimmingEvent e) {
      if (this.modeSetting.isSelected("FunTime")) {
         if (mc.options.jumpKey.isPressed()) {
            float pitch = RotationController.INSTANCE.getRotation().getPitch();
            float boost = pitch >= 0.0F ? MathHelper.clamp(pitch / 45.0F, 1.0F, 1.98F) : 1.0F;
            e.getVector().y = 0.6 * boost;
         } else if (mc.options.sneakKey.isPressed()) {
            e.getVector().y = -0.6;
         }
      }
   }
}
