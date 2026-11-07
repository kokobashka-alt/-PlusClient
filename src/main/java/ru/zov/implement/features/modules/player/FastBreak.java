package ru.zov.implement.features.modules.player;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.implement.events.block.BlockBreakingEvent;

public class FastBreak extends Module {
   public FastBreak() {
      super("FastBreak", ModuleCategory.PLAYER);
   }

   @EventHandler
   public void onBlockBreaking(BlockBreakingEvent e) {
      BlockPos blockPos = e.blockPos();
      Direction direction = e.direction();
      if (mc.interactionManager.currentBreakingProgress >= 0.5) {
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction));
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, blockPos, direction));
      }
   }
}
