package ru.zov.implement.features.modules.movement;

import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.MoveEvent;
import java.util.Collections;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.implement.events.player.AttackEvent;
import ru.zov.implement.events.player.TickEvent;

public class AirStuck extends Module {
   private static final List<Item> CHESTPLATES_BEST_FIRST = List.of(
      Items.NETHERITE_CHESTPLATE,
      Items.DIAMOND_CHESTPLATE,
      Items.IRON_CHESTPLATE,
      Items.CHAINMAIL_CHESTPLATE,
      Items.GOLDEN_CHESTPLATE,
      Items.LEATHER_CHESTPLATE
   );
   private static final int CHEST_ARMOR_SLOT = 6;
   private static final int FIRST_MAIN_SLOT = 9;
   private static final int FIRST_HOTBAR_SLOT = 36;
   private static final int LAST_HOTBAR_SLOT = 44;

   private final SelectSetting mode = new SelectSetting("Мод", "Режим заморозки").value("Обычный", "LonyGrief", "Grim", "SkyTime", "SpookyTime", "SpaceTime").selected("Обычный");
   private final BooleanSetting cancelPackets = new BooleanSetting("Отменять пакеты", "Не отправлять пакеты движения на сервер").setValue(true);
   private final BooleanSetting swapElytra = new BooleanSetting("Свапать элитру", "Менять элитру на нагрудник при включении").setValue(true);
   private final ValueSetting spookyRelease = new ValueSetting("Отпускать сек.", "Через сколько секунд отпустить (SpookyTime)")
      .setValue(4.4F).range(1.0F, 10.0F).visible(() -> this.mode.isSelected("SpookyTime"));
   private final ValueSetting spaceRelease = new ValueSetting("Отпускать сек.", "Через сколько секунд отпустить (SpaceTime)")
      .setValue(1.2F).range(0.5F, 10.0F).visible(() -> this.mode.isSelected("SpaceTime"));
   private final BooleanSetting skyLook = new BooleanSetting("Повороты", "Отправлять повороты головы во время зависания (SkyTime)")
      .setValue(true).visible(() -> this.mode.isSelected("SkyTime"));
   private final BooleanSetting betterCrits = new BooleanSetting("Улучшить криты", "Пометка для криток во время зависания")
      .setValue(true).visible(() -> !this.mode.isSelected("SkyTime"));
   private final BooleanSetting maceAntiFlag = new BooleanSetting("Булава (Анти-Флаг)", "Выключать модуль при ударе булавой").setValue(true);

   private Vec3d freezePosition = Vec3d.ZERO;
   private boolean frozen;
   private boolean holding;
   private boolean released;
   private Vec3d holdPos = Vec3d.ZERO;
   private Vec3d holdVelocity = Vec3d.ZERO;
   private Vec3d lastVelocity = Vec3d.ZERO;
   private double holdFallDistance;
   private long holdStart;
   private float sentYaw;
   private float sentPitch;
   private boolean lookSent;

   public AirStuck() {
      super("AirStuck", ModuleCategory.MOVEMENT);
      this.setup(this.mode, this.cancelPackets, this.swapElytra, this.spookyRelease, this.spaceRelease, this.skyLook, this.betterCrits, this.maceAntiFlag);
   }

   public static AirStuck getInstance() {
      return Instance.get(AirStuck.class);
   }

   public boolean isFrozen() {
      return this.frozen;
   }

   @Override
   public void activate() {
      this.frozen = false;
      this.holding = false;
      this.released = false;
      this.lookSent = false;
      if (mc.player == null) {
         return;
      }

      if (this.swapElytra.isValue()) {
         this.swapChestEquipment();
      }

      if (this.mode.isSelected("Обычный")) {
         this.freezePosition = mc.player.getEntityPos();
         this.frozen = true;
      }
   }

   @Override
   public void deactivate() {
      this.frozen = false;
      this.releaseHold();
   }

   private void swapChestEquipment() {
      if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
         return;
      }

      int slotId = this.findBestChestplateSlotId();
      if (slotId != -1) {
         this.swapIntoChestSlot(slotId);
      }
   }

   // Слоты playerScreenHandler: 9..35 — инвентарь, 36..44 — хотбар
   private int findBestChestplateSlotId() {
      int bestId = -1;
      int bestIndex = Integer.MAX_VALUE;
      for (Slot slot : mc.player.playerScreenHandler.slots) {
         if (slot.id < FIRST_MAIN_SLOT || slot.id > LAST_HOTBAR_SLOT) {
            continue;
         }

         int index = CHESTPLATES_BEST_FIRST.indexOf(slot.getStack().getItem());
         if (index != -1 && index < bestIndex) {
            bestIndex = index;
            bestId = slot.id;
         }
      }

      return bestId;
   }

   private void swapIntoChestSlot(int slotId) {
      if (slotId >= FIRST_HOTBAR_SLOT) {
         mc.interactionManager.clickSlot(0, CHEST_ARMOR_SLOT, slotId - FIRST_HOTBAR_SLOT, SlotActionType.SWAP, mc.player);
      } else {
         mc.interactionManager.clickSlot(0, slotId, 0, SlotActionType.SWAP, mc.player);
         mc.interactionManager.clickSlot(0, CHEST_ARMOR_SLOT, 0, SlotActionType.SWAP, mc.player);
         mc.interactionManager.clickSlot(0, slotId, 0, SlotActionType.SWAP, mc.player);
      }

      mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
   }

   @EventHandler
   public void onMove(MoveEvent e) {
      if (mc.player == null) {
         return;
      }

      if (this.isTimedMode()) {
         this.handleTimedMove(e);
         return;
      }

      if (this.mode.isSelected("LonyGrief") && !this.frozen && mc.player.fallDistance > 0.0F && mc.player.getVelocity().y < 0.0) {
         this.freezePosition = mc.player.getEntityPos();
         this.frozen = true;
      }

      if (this.frozen) {
         e.setMovement(Vec3d.ZERO);
         mc.player.setPosition(this.freezePosition.x, this.freezePosition.y, this.freezePosition.z);
         mc.player.setVelocity(0.0, 0.0, 0.0);
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.holding && this.isTimedMode() && e.isSend() && mc.player != null
         && e.getPacket() instanceof PlayerMoveC2SPacket move) {
         e.cancel();
         if (this.mode.isSelected("SkyTime") && this.skyLook.isValue() && move.changesLook()) {
            this.sendHoldLook(move);
         }
         return;
      }

      if (!this.frozen || !e.isSend() || mc.player == null) {
         return;
      }

      if (e.getPacket() instanceof PlayerMoveC2SPacket move) {
         e.cancel();
         if (!this.cancelPackets.isValue()) {
            PlayerIntersectionUtil.sendPacketWithOutEvent(this.createFrozenPacket(move));
         }
      }
   }

   private PlayerMoveC2SPacket createFrozenPacket(PlayerMoveC2SPacket packet) {
      boolean onGround = packet.isOnGround();
      boolean horizontalCollision = packet.horizontalCollision();
      if (packet.changesPosition() && packet.changesLook()) {
         return new Full(
            this.freezePosition.x,
            this.freezePosition.y,
            this.freezePosition.z,
            packet.getYaw(mc.player.getYaw()),
            packet.getPitch(mc.player.getPitch()),
            onGround,
            horizontalCollision
         );
      }

      if (packet.changesPosition()) {
         return new PositionAndOnGround(this.freezePosition.x, this.freezePosition.y, this.freezePosition.z, onGround, horizontalCollision);
      }

      if (packet.changesLook()) {
         return new LookAndOnGround(packet.getYaw(mc.player.getYaw()), packet.getPitch(mc.player.getPitch()), onGround, horizontalCollision);
      }

      return new OnGroundOnly(onGround, horizontalCollision);
   }

   public boolean isTimedMode() {
      return this.mode.isSelected("Grim", "SkyTime", "SpookyTime", "SpaceTime");
   }

   /** True while the Improve Crits option should let criticals be used during the hold. */
   public boolean isCritBoost() {
      return this.isState() && this.holding && this.holdFallDistance > 0.0F
         && (this.mode.isSelected("SkyTime") || this.betterCrits.isValue());
   }

   private boolean autoRelease() {
      return this.mode.isSelected("SpookyTime", "SpaceTime");
   }

   private long releaseDelayMs() {
      float seconds = this.mode.isSelected("SpaceTime") ? this.spaceRelease.getValue() : this.spookyRelease.getValue();
      return (long) (seconds * 1000.0F);
   }

   private void handleTimedMove(MoveEvent e) {
      Vec3d movement = e.getMovement();
      if (!this.holding && !this.released && this.isGroundCollision(movement)) {
         this.startHold(mc.player.getVelocity());
      } else if (!this.holding && !this.released && mc.player.fallDistance > 0.0F
         && mc.player.getVelocity().y < 0.0
         && (mc.player.fallDistance >= 0.45F || this.isGroundCollision(movement))) {
         this.startHold(this.autoRelease() ? this.lastVelocity : mc.player.getVelocity());
      }

      if (this.holding) {
         e.setMovement(Vec3d.ZERO);
         mc.player.setPosition(this.holdPos.x, this.holdPos.y, this.holdPos.z);
         mc.player.setVelocity(0.0, 0.0, 0.0);
         if (this.isCritBoost()) {
            mc.player.fallDistance = Math.max(mc.player.fallDistance, this.holdFallDistance);
         }
      }
   }

   private void startHold(Vec3d velocity) {
      this.holdVelocity = velocity;
      this.holdFallDistance = mc.player.fallDistance;
      this.holdPos = mc.player.getEntityPos();
      this.holding = true;
      this.holdStart = System.currentTimeMillis();
      this.lookSent = false;
   }

   private void releaseHold() {
      if (this.holding && mc.player != null) {
         mc.player.setVelocity(this.holdVelocity);
         mc.player.fallDistance = this.holdFallDistance;
         mc.player.setPosition(this.holdPos.x, this.holdPos.y, this.holdPos.z);
      }
      this.holding = false;
      this.released = false;
   }

   /** A downward move that the world would stop on a block: the same test as the original AirStuck. */
   private boolean isGroundCollision(Vec3d movement) {
      if (movement == null || movement.y >= 0.0 || mc.player.isOnGround() || mc.player.fallDistance <= 0.0F) {
         return false;
      }
      if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()
         || mc.player.isGliding() || mc.player.getAbilities().flying || mc.player.hasVehicle()) {
         return false;
      }
      Vec3d adjusted = Entity.adjustMovementForCollisions(
         mc.player, movement, mc.player.getBoundingBox(), mc.player.getEntityWorld(), Collections.<VoxelShape>emptyList());
      return adjusted.y > movement.y + 1.0E-7;
   }

   private void sendHoldLook(PlayerMoveC2SPacket move) {
      float yaw = move.getYaw(mc.player.getYaw());
      float pitch = MathHelper.clamp(move.getPitch(mc.player.getPitch()), -90.0F, 90.0F);
      if (!this.lookSent || Math.abs(yaw - this.sentYaw) > 0.01F || Math.abs(pitch - this.sentPitch) > 0.01F) {
         this.sentYaw = yaw;
         this.sentPitch = pitch;
         this.lookSent = true;
         PlayerIntersectionUtil.sendPacketWithOutEvent(
            new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), move.horizontalCollision()));
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player == null || !this.isTimedMode()) {
         return;
      }

      if (!this.holding && !this.released) {
         this.lastVelocity = mc.player.getVelocity();
      }

      if (this.released) {
         this.released = false;
         if (!mc.player.isOnGround()) {
            this.startHold(mc.player.getVelocity());
         }
      }

      if (this.holding && this.autoRelease() && System.currentTimeMillis() - this.holdStart >= this.releaseDelayMs()) {
         this.holding = false;
         this.released = true;
         mc.player.setPosition(this.holdPos.x, this.holdPos.y, this.holdPos.z);
         mc.player.setVelocity(this.holdVelocity);
         mc.player.fallDistance = this.holdFallDistance;
      }

      if (this.holding) {
         mc.player.setVelocity(0.0, 0.0, 0.0);
         mc.player.setPosition(this.holdPos.x, this.holdPos.y, this.holdPos.z);
         if (this.isCritBoost()) {
            mc.player.fallDistance = Math.max(mc.player.fallDistance, this.holdFallDistance);
         }
      }
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      if (this.maceAntiFlag.isValue() && this.isState() && mc.player != null
         && mc.player.getMainHandStack().getItem() == Items.MACE) {
         this.setState(false);
      }
   }
}
