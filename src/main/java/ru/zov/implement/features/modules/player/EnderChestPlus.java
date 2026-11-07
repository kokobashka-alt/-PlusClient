package ru.zov.implement.features.modules.player;

import java.util.List;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.implement.events.container.CloseScreenEvent;
import ru.zov.implement.events.container.OpenScreenEvent;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.features.draggables.Notifications;

public class EnderChestPlus extends Module {
   private HandledScreen<?> screen;
   private final BindSetting bindSetting = new BindSetting("Fold Items Button", "Puts all items in the ender chest");

   public EnderChestPlus() {
      super("EnderChestPlus", "Ender-Chest Plus", ModuleCategory.PLAYER);
      this.setup(this.bindSetting);
   }

   @Override
   public void deactivate() {
      if (this.screen != null) {
         this.screen = null;
         PlayerInventoryUtil.closeScreen(true);
         Notifications.getInstance().addList("Ender Chest - " + Formatting.RED + "закрыт", 5000L);
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.bindSetting.getKey()) && this.screen != null) {
         List<Slot> slots = PlayerInventoryUtil.getSlots();
         slots.stream()
            .filter(s -> s.id < slots.size() - 36 && s.getStack().isEmpty())
            .findFirst()
            .ifPresent(s -> PlayerInventoryUtil.swapHand(s, Hand.OFF_HAND, false));
         slots.stream()
            .filter(s -> s.id >= slots.size() - 36 && !s.getStack().isEmpty())
            .forEach(s -> PlayerInventoryUtil.clickSlot(s, 0, SlotActionType.QUICK_MOVE, false));
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.screen != null && mc.player != null) {
         Packet packet = e.getPacket();
         if (packet instanceof GameJoinS2CPacket
            || packet instanceof OpenScreenS2CPacket
            || packet instanceof CloseScreenS2CPacket
            || packet instanceof PlayerRespawnS2CPacket) {
            this.deactivate();
         } else if (packet instanceof CloseHandledScreenC2SPacket) {
            e.cancel();
         } else if (packet instanceof PlayerActionC2SPacket player && player.getAction().equals(Action.SWAP_ITEM_WITH_OFFHAND)) {
            PlayerInventoryUtil.swapHand(PlayerInventoryUtil.mainHandSlot(), Hand.OFF_HAND, false);
            e.cancel();
         }
      }
   }

   @EventHandler
   public void onOpenScreen(OpenScreenEvent e) {
      if (e.getScreen() instanceof InventoryScreen && this.screen != null) {
         e.setScreen(this.screen);
      }
   }

   @EventHandler
   public void onCloseScreen(CloseScreenEvent e) {
      if (e.getScreen() instanceof GenericContainerScreen scr && scr.getTitle().getString().contains(Text.translatable("container.enderchest").getString())) {
         this.screen = scr;
      }

      if (this.screen != null) {
         mc.setScreen(null);
         e.cancel();
      }
   }
}
