package ru.zov.implement.features.modules.misc;

import java.util.List;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;

public class AutoTpAccept extends Module {
   private final List<String> teleportMessageList = List.of(
      "has requested teleport", "просит телепортироваться", "хочет телепортироваться к вам", "просит к вам телепортироваться"
   );
   private boolean canAccept;
   private final BooleanSetting friendSetting = new BooleanSetting("Only Friends", "Will only accept requests from friends").setValue(false);

   public AutoTpAccept() {
      super("AutoTpAccept", "Auto Tp Accept", ModuleCategory.MISC);
      this.setup(this.friendSetting);
   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof GameMessageS2CPacket gm) {
         String message = gm.content().getString().toLowerCase();
         if (this.teleportMessageList.stream().anyMatch(message::contains)) {
            this.canAccept = !this.friendSetting.isValue() || FriendUtils.getFriends().stream().map(String::toLowerCase).anyMatch(message::contains);
         }
      }
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (!ServerUtil.isPvp() && this.canAccept) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
         this.canAccept = false;
      }
   }
}
