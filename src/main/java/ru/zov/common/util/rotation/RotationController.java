package ru.zov.common.util.rotation;

import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.api.feature.module.Module;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.task.TaskProcessor;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.PlayerVelocityStrafeEvent;
import ru.zov.implement.events.player.RotationUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.event.PacketReceiveEvent;
import ru.zov.implement.features.modules.combat.killaura.event.PacketSendEvent;
import ru.zov.implement.features.modules.combat.killaura.event.PlayerTickEvent;
import ru.zov.implement.features.modules.combat.killaura.type.CombatPauseManager;
import ru.zov.implement.features.modules.combat.killaura.type.EventPhase;
import ru.zov.implement.features.modules.combat.killaura.type.TickStage;
import ru.zov.implement.features.modules.combat.killaura.util.GrimDelayHandler;
import ru.zov.implement.features.modules.combat.killaura.util.PlayerSnapshotManager;

public class RotationController implements QuickImports {
   public static RotationController INSTANCE = new RotationController();
   private RotationPlan lastRotationPlan;
   public final TaskProcessor<RotationPlan> rotationPlanTaskProcessor = new TaskProcessor<RotationPlan>();
   private Angle currentAngle;
   private Angle previousAngle;
   private Angle serverAngle;
   private boolean donorRotationActive;

   public RotationController() {
      this.serverAngle = Angle.DEFAULT;
      main.getEventManager().register(this);
   }

   public void setRotation(Angle value) {
      this.previousAngle = this.currentAngle;
      this.currentAngle = value;
   }

   public Angle getInterpolatedRotation() {
      return MathUtil.interpolate(this.getPreviousRotation(), this.getRotation());
   }

   public Angle getRotation() {
      return this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public Angle getPreviousRotation() {
      return this.currentAngle != null && this.previousAngle != null ? this.previousAngle : new Angle(mc.player.lastYaw, mc.player.lastPitch);
   }

   public Angle getMoveRotation() {
      RotationPlan rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.moveCorrection() ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public RotationPlan getCurrentRotationPlan() {
      ru.zov.implement.features.modules.combat.killaura.math.ScheduledRotation donorPlan = ru.zov.implement.features.modules.combat.killaura.util.RotationManager.INSTANCE.getCurrentStrategy();
      if (donorPlan != null) {
         ru.zov.implement.features.modules.combat.killaura.math.Rotation rotation = donorPlan.rotation();
         return new RotationPlan(
            new Angle(rotation.getYaw(), rotation.getPitch()),
            donorPlan.vec3d(),
            donorPlan.entity(),
            new ru.zov.common.util.rotation.angle.LinearSmoothMode(),
            donorPlan.ticksUntilReset(),
            donorPlan.resetThreshold(),
            donorPlan.moveCorrection(),
            !donorPlan.focusedCorrection()
         );
      }
      return this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? (RotationPlan)this.rotationPlanTaskProcessor.fetchActiveTaskValue() : this.lastRotationPlan;
   }

   public void rotateTo(Angle.VecRotation vecRotation, LivingEntity entity, int reset, RotationConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(vecRotation.angle(), vecRotation.vec(), entity, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, int reset, RotationConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), (Entity)null, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, RotationConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), (Entity)null, 1), taskPriority, provider);
   }

   public void rotateTo(RotationPlan plan, TaskPriority taskPriority, Module provider) {
      this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task(1, taskPriority.getPriority(), provider, plan));
   }

   public void update() {
      RotationPlan activePlan = this.getCurrentRotationPlan();
      if (activePlan != null) {
         Angle clientAngle = AngleUtil.cameraAngle();
         if (this.lastRotationPlan != null && activePlan.ticksUntilReset() <= this.rotationPlanTaskProcessor.tickCounter && computeRotationDifference(this.serverAngle, clientAngle) < (double)4.0F) {
            this.setRotation((Angle)null);
            this.clear();
         } else {
            Angle newAngle = activePlan.nextRotation(this.currentAngle != null ? this.currentAngle : clientAngle, this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
            this.setRotation(newAngle);
            this.lastRotationPlan = activePlan;
            this.rotationPlanTaskProcessor.tick(1);
         }
      }
   }

   public static double computeRotationDifference(Angle a, Angle b) {
      return Math.hypot((double)Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), (double)Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return MathHelper.wrapDegrees(a - b);
   }

   public void clear() {
      this.rotationPlanTaskProcessor.tickCounter = 0;
      this.lastRotationPlan = null;
   }

   @EventHandler
   public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
      RotationPlan currentRotationPlan = this.getCurrentRotationPlan();
      if (currentRotationPlan != null && currentRotationPlan.moveCorrection()) {
         e.setVelocity(this.currentAngle);
      }

   }

   @EventHandler
   public void onTick(TickEvent e) {
      EventManager.callEvent(new RotationUpdateEvent((byte)0));
      PlayerInventoryComponent.tick();
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(new PlayerTickEvent(TickStage.PRE));
      PlayerSnapshotManager.INSTANCE.tick();
      CombatPauseManager.INSTANCE.update();
      GrimDelayHandler.tick();
      ru.zov.implement.features.modules.combat.killaura.util.ServerUtil.sync();
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(new ru.zov.implement.features.modules.combat.killaura.event.RotationUpdateEvent(EventPhase.PRE));
      ru.zov.implement.features.modules.combat.killaura.util.RotationManager.INSTANCE.updateRotation();
      ru.zov.implement.features.modules.combat.killaura.math.Rotation donorRotation = ru.zov.implement.features.modules.combat.killaura.util.RotationManager.INSTANCE.getCurrentRotationOriginal();
      if (donorRotation != null) {
         this.donorRotationActive = true;
         this.previousAngle = this.currentAngle;
         this.currentAngle = new Angle(donorRotation.getYaw(), donorRotation.getPitch());
      } else {
         if (this.donorRotationActive) {
            this.previousAngle = this.currentAngle;
            this.currentAngle = null;
            this.donorRotationActive = false;
         }
         this.update();
      }
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(new ru.zov.implement.features.modules.combat.killaura.event.RotationUpdateEvent(EventPhase.POST));
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(new PlayerTickEvent(TickStage.POST));
      EventManager.callEvent(new RotationUpdateEvent((byte)2));
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      ru.zov.implement.features.modules.combat.killaura.util.SlotSyncHandler.packet(event.getPacket());
      if (event.isSend()) {
         PacketSendEvent donorEvent = new PacketSendEvent(event.getPacket());
         AuraRuntime.INSTANCE.eventDispatcher().dispatch(donorEvent);
         if (donorEvent.isCancelled()) event.cancel();
      } else {
         PacketReceiveEvent donorEvent = new PacketReceiveEvent(event.getPacket());
         AuraRuntime.INSTANCE.eventDispatcher().dispatch(donorEvent);
         if (donorEvent.isCancelled()) event.cancel();
      }
      if (!event.isCancelled()) {
         Packet var10000 = event.getPacket();
         Objects.requireNonNull(var10000);
         Packet var4 = var10000;
         if (var4 instanceof PlayerMoveC2SPacket player) {
            if (player.changesLook()) {
               this.serverAngle = new Angle(player.getYaw(this.serverAngle.getYaw()), player.getPitch(this.serverAngle.getPitch()));
            }
         } else if (var4 instanceof PlayerPositionLookS2CPacket player) {
            this.serverAngle = new Angle(player.change().yaw(), player.change().pitch());
         }
      }
   }

   public RotationPlan getLastRotationPlan() {
      return this.lastRotationPlan;
   }

   public TaskProcessor<RotationPlan> getRotationPlanTaskProcessor() {
      return this.rotationPlanTaskProcessor;
   }

   public Angle getCurrentAngle() {
      return this.currentAngle;
   }

   public Angle getPreviousAngle() {
      return this.previousAngle;
   }

   public Angle getServerAngle() {
      return this.serverAngle;
   }
}
