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

   private final SelectSetting mode = new SelectSetting("Мод", "Режим заморозки").value("Обычный", "LonyGrief").selected("Обычный");
   private final BooleanSetting cancelPackets = new BooleanSetting("Отменять пакеты", "Не отправлять пакеты движения на сервер").setValue(true);
   private final BooleanSetting swapElytra = new BooleanSetting("Свапать элитру", "Менять элитру на нагрудник при включении").setValue(true);

   private Vec3d freezePosition = Vec3d.ZERO;
   private boolean frozen;

   public AirStuck() {
      super("AirStuck", ModuleCategory.MOVEMENT);
      this.setup(this.mode, this.cancelPackets, this.swapElytra);
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
}
