package ru.zov.implement.features.modules.movement;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.implement.events.player.TickEvent;

public class WaterLeave extends Module {
   private final SelectSetting mode = new SelectSetting("Mode", "Selects the type of mode").value("FunTime");

   public WaterLeave() {
      super("WaterLeave", "Water Leave", ModuleCategory.MOVEMENT);
      this.setup(this.mode);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (this.mode.isSelected("FunTime")) {
         Box playerBox = mc.player.getBoundingBox();
         Box box = new Box(playerBox.minX, playerBox.minY - 2.0, playerBox.minZ, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
         if (mc.player.isTouchingWater() && PlayerIntersectionUtil.isBoxInBlock(box, Blocks.SOUL_SAND)) {
            mc.player.velocity.y = 1.6;
         }
      }
   }
}
