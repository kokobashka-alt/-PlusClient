package ru.zov.implement.features.modules.misc;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.CrosshairTargetEvent;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.MoveEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.CameraPositionEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RaytracingUtil;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.common.util.rotation.angle.LinearSmoothMode;

public class DebugCamera extends Module {
   private final List<Packet<?>> packets = new CopyOnWriteArrayList();
   private Vec3d pos;
   private Vec3d prevPos;
   private final ValueSetting speedSetting = (new ValueSetting("Speed", "Select debug camera speed  ")).setValue(2.0F).range(0.5F, 5.0F);
   private final BooleanSetting freezeSetting = (new BooleanSetting("Freeze", "You freeze in place")).setValue(false);

   public DebugCamera() {
      super("DebugCamera", "Debug Camera", ModuleCategory.MISC);
      this.setup(new Setting[]{this.speedSetting, this.freezeSetting});
   }

   @Native
   public void deactivate() {
      this.packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
      this.packets.clear();
      super.deactivate();
   }

   public void activate() {
      this.prevPos = this.pos = new Vec3d(mc.getEntityRenderDispatcher().camera.getCameraPos().toVector3f());
      super.activate();
   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof GameJoinS2CPacket || var2 instanceof PlayerRespawnS2CPacket) {
         this.setState(false);
      } else if (var2 instanceof ClientStatusC2SPacket status) {
         if (status.getMode().equals(Mode.PERFORM_RESPAWN)) {
            this.setState(false);
         }
      } else if (this.freezeSetting.isValue() && (var2 instanceof CommonPongC2SPacket || var2 instanceof PlayerMoveC2SPacket)) {
         this.packets.add(var2);
         e.cancel();
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      Render3DUtil.drawBox(mc.player.getBoundingBox().offset(MathUtil.interpolate(mc.player).subtract(mc.player.getEntityPos())), -1, 1.0F);
   }

   @EventHandler(1)
   public void onTick(TickEvent e) {
      HitResult var3 = mc.crosshairTarget;
      if (var3 instanceof BlockHitResult result) {
         RotationController.INSTANCE.rotateTo(AngleUtil.calculateAngle(result.getBlockPos().toCenterPos()), new RotationConfig(new LinearSmoothMode(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);
      }

   }

   @EventHandler
   @Native
   public void onMove(MoveEvent e) {
      if (this.freezeSetting.isValue()) {
         e.setMovement(Vec3d.ZERO);
      }

   }

   @EventHandler
   public void onInput(InputEvent e) {
      float speed = this.speedSetting.getValue();
      double[] motion = MovingUtil.calculateDirection((float)e.forward(), (float)e.sideways(), mc.player.getYaw(), (double)speed);
      this.prevPos = this.pos;
      this.pos = this.pos.add(motion[0], e.getInput().jump() ? (double)speed : (e.getInput().sneak() ? (double)(-speed) : (double)0.0F), motion[1]);
      e.inputNone();
   }

   @EventHandler
   public void onCameraPosition(CameraPositionEvent e) {
      e.setPos(MathUtil.interpolate(this.prevPos, this.pos));
      mc.options.setPerspective(Perspective.FIRST_PERSON);
   }

   @EventHandler
   public void onCrosshairTarget(CrosshairTargetEvent e) {
      e.setResult(RaytracingUtil.raycast(this.pos, (double)4.5F, AngleUtil.cameraAngle(), false));
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.setState(false);
   }
}
