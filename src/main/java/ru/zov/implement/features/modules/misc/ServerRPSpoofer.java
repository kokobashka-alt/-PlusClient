package ru.zov.implement.features.modules.misc;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.math.StopWatch;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;

public class ServerRPSpoofer extends Module {
   private ServerRPSpoofer.ResourcePackAction currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
   private final StopWatch stopWatch = new StopWatch();

   public ServerRPSpoofer() {
      super("ServerRPSpoof", "Server RP Spoof", ModuleCategory.MISC);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
         this.currentAction = ServerRPSpoofer.ResourcePackAction.ACCEPT;
         e.cancel();
      }
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
      if (networkHandler != null) {
         if (this.currentAction == ServerRPSpoofer.ResourcePackAction.ACCEPT) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.ACCEPTED));
            this.currentAction = ServerRPSpoofer.ResourcePackAction.SEND;
            this.stopWatch.reset();
         } else if (this.currentAction == ServerRPSpoofer.ResourcePackAction.SEND && this.stopWatch.finished(300.0)) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.SUCCESSFULLY_LOADED));
            this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
         }
      }
   }

   @Override
   public void deactivate() {
      this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
      super.deactivate();
   }

   public enum ResourcePackAction {
      ACCEPT,
      SEND,
      WAIT;
   }
}
