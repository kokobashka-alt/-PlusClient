package ru.zov.implement.features.modules.movement;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.JumpEvent;

public class ElytraRecast extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", "Jumps Automatically.").setValue(false);
   public final BooleanSetting ignoreDurability = new BooleanSetting(
         "Ignore Durability", "Ignores the durability of elytra, allowing them to fly even when they are broken."
      )
      .setValue(false);

   public static ElytraRecast getInstance() {
      return Instance.get(ElytraRecast.class);
   }

   public ElytraRecast() {
      super("ElytraRecast", "Elytra Recast", ModuleCategory.MOVEMENT);
      this.setup(this.autoJump, this.ignoreDurability);
   }

   @EventHandler
   public void onJump(JumpEvent e) {
      this.stopWatch.reset();
   }

   @EventHandler
   @Native
   public void onInput(InputEvent e) {
      if (!this.stopWatch.finished(50.0)) {
         e.setJump(false);
      } else {
         if (this.autoJump.isValue() && this.canStart() && mc.player != null) {
            e.setJump(true);
         }
      }
   }

   public boolean canStart() {
      return this.isState()
         && MovingUtil.hasPlayerMovement()
         && this.stopWatch.finished(50.0)
         && !ElytraFly.getInstance().isState()
         && LivingEntity.canGlideWith(mc.player.getEquippedStack(EquipmentSlot.CHEST), EquipmentSlot.CHEST);
   }
}
