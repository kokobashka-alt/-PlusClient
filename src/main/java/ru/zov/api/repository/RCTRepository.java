package ru.zov.api.repository;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;

public class RCTRepository implements QuickImports, QuickLogger {
   private final StopWatch stopWatch = new StopWatch();
   private boolean lobby;
   private int anarchy;

   public RCTRepository(EventManager eventManager) {
      eventManager.register(this);
   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      switch (e.getPacket()) {
         case GameMessageS2CPacket message when this.anarchy != 0:
            String text = message.content().getString().toLowerCase();
            if (!text.contains("хаб") && text.contains("не удалось") && !text.contains("заполнен")) {
               Notifications.getInstance().addList("[RCT] На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти", 3000L);
               this.anarchy = 0;
            }
            break;
         case OpenScreenS2CPacket openScreen:
            this.stopWatch.reset();
            break;
         default:
      }
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (this.anarchy != 0) {
         if (ServerUtil.isHolyWorld() && !ServerUtil.isPvp()) {
            int currentAnarchy = ServerUtil.getAnarchy();
            if (this.lobby) {
               if (ServerUtil.getHeader().getString().contains("Лобби")) {
                  this.lobby = false;
               } else {
                  mc.player.networkHandler.sendChatCommand("hub");
               }
            } else if (currentAnarchy == this.anarchy) {
               this.anarchy = 0;
            } else if (mc.currentScreen instanceof GenericContainerScreen screen
               && screen.getTitle().getString().equals("Выбор Лайт анархии:")
               && this.stopWatch.every(150.0)) {
               int[] slots = this.anarchy < 15
                  ? new int[]{0, 0}
                  : (this.anarchy < 33 ? new int[]{1, 14} : (this.anarchy < 48 ? new int[]{2, 32} : new int[]{3, 47}));
               if (screen.getScreenHandler().getRows() == 1) {
                  PlayerInventoryUtil.clickSlot(slots[0], 0, SlotActionType.PICKUP, true);
               } else {
                  PlayerInventoryUtil.clickSlot(17 + this.anarchy - slots[1], 0, SlotActionType.PICKUP, true);
               }
            } else {
               if (this.stopWatch.every(1100.0)) {
                  mc.player.networkHandler.sendChatCommand("lite");
               }
            }
         } else {
            this.anarchy = 0;
         }
      }
   }

   @Native
   public void reconnect(int anarchy) {
      if (anarchy > 0 && anarchy < 64) {
         this.anarchy = anarchy;
         this.lobby = true;
      } else {
         Notifications.getInstance().addList("[RCT] Не верный " + Formatting.RED + "лайт", 3000L);
      }
   }
}
