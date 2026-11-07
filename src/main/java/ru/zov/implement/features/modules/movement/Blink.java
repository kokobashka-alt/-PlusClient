package ru.zov.implement.features.modules.movement;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class Blink extends Module {
   private final List<Packet<?>> packets = new CopyOnWriteArrayList();
   private Box box;

   public Blink() {
      super("Blink", ModuleCategory.MOVEMENT);
      this.box = new Box(BlockPos.ORIGIN);
      this.setup(new Setting[0]);
   }

   public void activate() {
      this.box = mc.player.getBoundingBox();
   }

   public void deactivate() {
      this.packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
      this.packets.clear();
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         Packet var10000 = e.getPacket();
         Objects.requireNonNull(var10000);
         Packet var6 = var10000;
         if (var6 instanceof PlayerRespawnS2CPacket || var6 instanceof HandshakeC2SPacket || var6 instanceof GameJoinS2CPacket) {
            this.setState(false);
         } else if (var6 instanceof ClientStatusC2SPacket status) {
            if (status.getMode().equals(Mode.PERFORM_RESPAWN)) {
               this.setState(false);
            } else if (e.isSend()) {
               this.packets.add(e.getPacket());
               e.cancel();
            }
         } else if (e.isSend()) {
            this.packets.add(e.getPacket());
            e.cancel();
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      Render3DUtil.drawBox(this.box, ColorUtil.getClientColor(), 1.0F);
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.setState(false);
   }
}
