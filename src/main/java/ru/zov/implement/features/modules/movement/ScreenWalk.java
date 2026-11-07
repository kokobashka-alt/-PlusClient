package ru.zov.implement.features.modules.movement;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.implement.events.container.ClickSlotEvent;
import ru.zov.implement.events.container.CloseScreenEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;

public class ScreenWalk extends Module {
   private static ScreenWalk instance;
   private final List<ClickSlotC2SPacket> packets = new ArrayList();
   private final SelectSetting bypassMode = (new SelectSetting("Mode", "Selects the server bypass mode"))
      .value("FunTime", "SpookyTime", "HolyWorld", "ReallyWorld", "CopyTime", "Vanilla");

   public ScreenWalk() {
      super("ScreenWalk", "Screen Walk", ModuleCategory.MOVEMENT);
      instance = this;
      this.setup(new Setting[]{this.bypassMode});
   }

   public static String getBypassMode() {
      return instance == null ? "Vanilla" : instance.bypassMode.getSelected();
   }

   public static boolean isBypassMode(String... modes) {
      String selected = getBypassMode();
      return Arrays.stream(modes).anyMatch(selected::equalsIgnoreCase);
   }

   public static boolean shouldQueueInventoryActions() {
      return !isBypassMode("Vanilla", "ReallyWorld");
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet var3 = e.getPacket();
      if (var3 instanceof CloseScreenS2CPacket close) {
         if (this.usesBufferedClicks() && close.getSyncId() == 0) {
            e.cancel();
         }
      }

   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (!this.usesBufferedClicks() && !this.packets.isEmpty()) {
         this.flushPackets();
      }

      if (!PlayerInventoryUtil.isServerScreen() && PlayerInventoryComponent.shouldSkipExecution() && (!this.packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
         PlayerInventoryComponent.updateMoveKeys();
      }

   }

   @EventHandler(0)
   public void onClickSlot(ClickSlotEvent e) {
      if (mc.player != null && this.usesBufferedClicks()) {
         Screen var3 = mc.currentScreen;
         if (var3 instanceof InventoryScreen) {
            InventoryScreen inventory = (InventoryScreen)var3;
            if (!this.packets.isEmpty() || MovingUtil.hasPlayerMovement()) {
               this.onClick(inventory.getScreenHandler(), e.getActionType(), e.getSlotId(), e.getButton());
               e.cancel();
            }
         }
      }

   }

   @EventHandler
   @Native
   public void onCloseScreen(CloseScreenEvent e) {
      if (mc.player != null && (this.usesBufferedClicks() || !this.packets.isEmpty())) {
         mc.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
         if (!this.packets.isEmpty()) {
            PlayerInventoryComponent.addTask(this::flushPackets);
            mc.setScreen((Screen)null);
            e.cancel();
         } else if (this.usesBufferedClicks() && MovingUtil.hasPlayerMovement()) {
            mc.setScreen((Screen)null);
            e.cancel();
         }
      }

   }

   private void onClick(ScreenHandler screenHandler, SlotActionType actionType, int slotId, int button) {
      if (!this.bypassMode.isSelected("HolyWorld") || slotId != 6 || !actionType.equals(SlotActionType.PICKUP) && !actionType.equals(SlotActionType.SWAP)) {
         if (!actionType.equals(SlotActionType.PICKUP) && !actionType.equals(SlotActionType.PICKUP_ALL)) {
            screenHandler.onSlotClick(slotId, button, actionType, mc.player);
            this.packets.add(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), (short)slotId, (byte)button, actionType, Int2ObjectMaps.emptyMap(), ItemStackHash.fromItemStack(screenHandler.getCursorStack(), mc.getNetworkHandler().getComponentHasher())));
         } else {
            ItemStack cursor = slotId == -999 ? ItemStack.EMPTY : screenHandler.getSlot(slotId).getStack().copy();
            this.packets.add(new ClickSlotC2SPacket(screenHandler.syncId, this.packets.size(), (short)slotId, (byte)button, actionType, Int2ObjectMaps.emptyMap(), ItemStackHash.fromItemStack(cursor, mc.getNetworkHandler().getComponentHasher())));
            screenHandler.onSlotClick(slotId, button, actionType, mc.player);
         }

      }
   }

   private boolean usesBufferedClicks() {
      return isBypassMode("FunTime", "SpookyTime", "HolyWorld", "CopyTime");
   }

   private void flushPackets() {
      this.packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
      this.packets.clear();
      PlayerInventoryUtil.updateSlots();
   }

   public List<ClickSlotC2SPacket> getPackets() {
      return this.packets;
   }
}
