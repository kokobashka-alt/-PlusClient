package ru.zov.implement.features.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.events.player.AttackEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.RotationController;

public class Criticals extends Module {
   private final SelectSetting mode = new SelectSetting("Mode", "Selects the type of mode").value("Grim Advanced");

   public Criticals() {
      super("Criticals", ModuleCategory.COMBAT);
      this.setup(this.mode);
   }

   @EventHandler
   @Native
   public void onAttack(AttackEvent e) {
      int version = PlayerIntersectionUtil.getProtocolVersion();
      if (version < 755 || version > 766) {
         Notifications.getInstance().addList("[Criticals] нужна версия " + Formatting.RED + "1.17-1.20.6", 2000L);
      } else if (mc.player != null && PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB, Blocks.LAVA, Blocks.SCAFFOLDING)) {
         Angle angle = RotationController.INSTANCE.getRotation().random(0.001F);
         PlayerIntersectionUtil.moveBypass$$$(MathUtil.getRandom(0.003, 0.004), angle.random(1.0F), false);
         PlayerIntersectionUtil.moveBypass$$$(-MathUtil.getRandom(0.001, 0.002), angle.random(0.001F), false);
         mc.player.fallDistance = 1.0E-4F;
      }
   }
}
