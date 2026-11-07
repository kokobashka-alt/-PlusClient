package ru.zov.implement.features.modules.movement;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.implement.events.player.JumpEvent;
import ru.zov.implement.events.player.PostTickEvent;

public class AirJump extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final SelectSetting modeSetting = new SelectSetting("Mode", "Selects the type of mode").value("Polar Block Collision");

   public AirJump() {
      super("AirJump", "Air Jump", ModuleCategory.MOVEMENT);
      this.setup(this.modeSetting);
   }

   @EventHandler
   public void onJump(JumpEvent e) {
      this.stopWatch.reset();
   }

   @EventHandler
   @Native
   public void onPostTick(PostTickEvent e) {
      if (this.modeSetting.isSelected("Polar Block Collision")) {
         if (mc.options.jumpKey.isPressed()) {
            return;
         }

         Box playerBox = mc.player.getBoundingBox().expand(-0.001);
         Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
         if (this.stopWatch.finished(400.0) && PlayerIntersectionUtil.isBox(box, this::hasCollision)) {
            box = new Box(playerBox.minX, playerBox.minY + 1.0, playerBox.minZ, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
            if (PlayerIntersectionUtil.isBox(box, this::hasCollision)) {
               mc.player.setOnGround(true);
               mc.player.velocity.y = 0.6;
            } else {
               mc.player.setOnGround(true);
               mc.player.jump();
            }
         }
      }
   }

   private boolean hasCollision(BlockPos blockPos) {
      return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
   }
}
