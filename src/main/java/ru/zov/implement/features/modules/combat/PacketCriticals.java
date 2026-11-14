package ru.zov.implement.features.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.implement.events.player.AttackEvent;

public class PacketCriticals extends Module {
   private static final double HOP_HEIGHT = 0.003;

   public PacketCriticals() {
      super("PacketCriticals", ModuleCategory.COMBAT);
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (!PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB)) {
         return;
      }

      double x = mc.player.getX();
      double y = mc.player.getY();
      double z = mc.player.getZ();
      mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y + HOP_HEIGHT, z, false, false));
      mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false, false));
   }
}
