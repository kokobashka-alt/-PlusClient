package ru.zov.implement.features.modules.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.block.BlockCollisionEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.draggables.Notifications;

public class Phase extends Module {
   private final List<Packet<?>> list = new ArrayList();
   private Box box;
   private final SelectSetting mode;

   public Phase() {
      super("Phase", ModuleCategory.MOVEMENT);
      this.box = new Box(BlockPos.ORIGIN);
      this.mode = (new SelectSetting("Mode", "Selects the type of mode")).value("ReallyWorld");
      this.setup(new Setting[]{this.mode});
   }

   @Native
   public void activate() {
      int version = PlayerIntersectionUtil.getProtocolVersion();
      if (version < 755 || version > 766) {
         Notifications.getInstance().addList("[Phase] нужна версия " + String.valueOf(Formatting.RED) + "1.17-1.20.6", 2000L);
         this.setState(false);
      }

   }

   public void deactivate() {
      PlayerIntersectionUtil.sendPacketWithOutEvent(new PlayerMoveC2SPacket.Full((double)0.0F, (double)-999.0F, (double)0.0F, mc.player.getYaw(), mc.player.getPitch(), false, false));
      this.list.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
      this.list.clear();
      PlayerIntersectionUtil.sendPacketWithOutEvent(new PlayerMoveC2SPacket.Full((double)0.0F, (double)-999.0F, (double)0.0F, mc.player.getYaw(), mc.player.getPitch(), false, false));
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (!this.list.isEmpty()) {
         Render3DUtil.drawBox(this.box, ColorUtil.getClientColor(), 1.0F);
      }

   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof PlayerRespawnS2CPacket || var2 instanceof HandshakeC2SPacket || var2 instanceof GameJoinS2CPacket) {
         this.setState(false);
      } else if (var2 instanceof ClientStatusC2SPacket status) {
         if (status.getMode().equals(Mode.PERFORM_RESPAWN)) {
            this.setState(false);
         }
      } else if (var2 instanceof PlayerMoveC2SPacket move) {
         if (move.horizontalCollision() || !this.list.isEmpty()) {
            this.list.add(move);
            e.cancel();
         }
      } else if (var2 instanceof PlayerPositionLookS2CPacket setBack) {
         this.box = mc.player.getBoundingBox().offset(setBack.change().position().subtract(mc.player.getEntityPos()));
      }
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (this.list.isEmpty()) {
         this.box = mc.player.getBoundingBox();
      } else {
         mc.player.setOnGround(true);
         mc.player.velocity.y = (double)0.0F;
      }
   }

   @EventHandler
   public void onBlockCollision(BlockCollisionEvent e) {
      if (mc.player != null && !this.list.isEmpty()) {
         if (mc.player.getBlockY() > e.getPos().getY() && !mc.player.isSneaking()) {
            e.setState(Blocks.BARRIER.getDefaultState());
         } else {
            e.setState(Blocks.AIR.getDefaultState());
         }

      }
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.setState(false);
   }
}
