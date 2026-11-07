package ru.zov.implement.features.modules.movement;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.core.listener.impl.EventListener;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.JumpEvent;
import ru.zov.implement.events.player.MotionEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;

public class ElytraFly extends Module {
   private final StopWatch startFlyWatch = new StopWatch();
   private final StopWatch swapWatch = new StopWatch();
   private final SelectSetting mode = new SelectSetting("Mode", "Selects the type of mode").value("Grim");
   private final BooleanSetting setBack = new BooleanSetting("Acceleration Through Setbacks", "Allows you to accelerate by using setbacks")
      .visible(() -> this.mode.isSelected("Grim"))
      .setValue(false);
   private final BooleanSetting yBoost = new BooleanSetting("Y acceleration", "Allows you to quickly gain altitude")
      .visible(() -> this.mode.isSelected("Grim"))
      .setValue(true);

   public static ElytraFly getInstance() {
      return Instance.get(ElytraFly.class);
   }

   public ElytraFly() {
      super("ElytraFly", "Elytra Fly", ModuleCategory.MOVEMENT);
      this.setup(this.mode, this.setBack, this.yBoost);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.player != null) {
         Packet packet = e.getPacket();
         if (packet instanceof PlaySoundS2CPacket sound && sound.getSound().value().id().getPath().contains("item.armor.equip")) {
            e.cancel();
         } else if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slotUpdate
            && (slotUpdate.getSlot() == 6 || slotUpdate.getSlot() == 44)
            && !this.swapWatch.finished(500.0)) {
            e.cancel();
         }
      }
   }

   @EventHandler
   public void onJump(JumpEvent e) {
      this.startFlyWatch.reset();
   }

   @EventHandler(1)
   @Native
   public void onTick(TickEvent e) {
      if ((MovingUtil.hasPlayerMovement() || PlayerIntersectionUtil.isKey(mc.options.jumpKey))
         && !mc.player.isGliding()
         && !mc.player.isOnGround()
         && !PlayerInventoryUtil.isServerScreen()
         && PlayerInventoryComponent.script.isFinished()) {
         if (PlayerIntersectionUtil.getProtocolVersion() > 757) {
            Notifications.getInstance().addListIfEmpty("[Elytra Fly] нужна версия " + Formatting.RED + "1.16.5-1.18.1", 2500L);
            return;
         }

         if (!this.startFlyWatch.finished(300.0) && ServerUtil.isSpookyTime()) {
            return;
         }

         if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            PlayerIntersectionUtil.startFallFlying();
            return;
         }

         Slot elytra = PlayerInventoryUtil.getSlot(slot -> slot.id >= 36 && slot.getStack().isOf(Items.ELYTRA));
         if (elytra == null) {
            Notifications.getInstance().addListIfEmpty("[Elytra Fly] нужна " + Formatting.RED + "элитра" + Formatting.RESET + " в хотбаре", 2500L);
            return;
         }

         AutoSprint.getInstance().stopWatch.setMs(-500);
         if (EventListener.serverSprint) {
            return;
         }

         PlayerInventoryUtil.moveItem(elytra, 6);
         PlayerIntersectionUtil.startFallFlying();
         PlayerInventoryUtil.moveItem(elytra, 6);
         PlayerInventoryUtil.closeScreen(true);
         this.swapWatch.reset();
      }
   }

   @EventHandler
   public void onMotion(MotionEvent e) {
      if (mc.player.isGliding() && PlayerIntersectionUtil.getProtocolVersion() <= 757) {
         if (this.yBoost.isValue() && mc.player.velocity.horizontalLengthSquared() <= 1.0E-7 && PlayerIntersectionUtil.isKey(mc.options.jumpKey)) {
            RotationController.INSTANCE.rotateTo(AngleUtil.pitch(0.0F), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_2, this);
            mc.player.velocity.y += 0.06;
         } else if (this.setBack.isValue() && MovingUtil.hasPlayerMovement()) {
            Angle angle = RotationController.INSTANCE.getMoveRotation();
            double radians = Math.toRadians(angle.getYaw());
            double x = -Math.sin(radians) * 0.085;
            double z = Math.cos(radians) * 0.085;
            double multiply = 1.25;
            mc.player.setPosition(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
            mc.player.setVelocity(x * multiply, mc.player.velocity.y, z * multiply);
         }
      }
   }

   @EventHandler
   public void onInput(InputEvent e) {
      if (this.setBack.isValue()
         && mc.player != null
         && mc.player.isOnGround()
         && MovingUtil.hasPlayerMovement()
         && PlayerInventoryUtil.getSlot(Items.ELYTRA) != null) {
         e.setJump(true);
      }
   }
}
