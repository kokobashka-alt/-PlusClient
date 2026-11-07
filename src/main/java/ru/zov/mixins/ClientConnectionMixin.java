package ru.zov.mixins;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.packet.PacketEvent;

@Mixin({ClientConnection.class})
public class ClientConnectionMixin {
   @Inject(
      method = {"handlePacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static <T extends PacketListener> void handlePacketHook(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      PacketEvent event = new PacketEvent(packet, PacketEvent.Type.RECEIVE);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendHook(Packet<?> packet, CallbackInfo info) {
      PacketEvent event = new PacketEvent(packet, PacketEvent.Type.SEND);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }
}
