package ru.zov.core.listener.impl;

import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.common.QuickImports;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.core.listener.Listener;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.item.UsingItemEvent;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.event.UseItemEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.features.draggables.MiniMap;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.modules.render.Hud;
import ru.zov.implement.features.modules.render.PouchHud;

public class EventListener implements Listener, QuickImports {
   public static boolean serverSprint;
   public static int selectedSlot;
   private int ticks = 0;

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      MiniMap.getInstance().onPlayerLoad();
   }

@EventHandler(3)
public void onTick(TickEvent e) {
    --this.ticks;
    ServerUtil.tick();

    Hud hud = Hud.getInstance();
    PouchHud pouchHud = PouchHud.getInstance();

    for (AbstractDraggable draggable : main.getDraggableRepository().draggable()) {
        boolean selected = hud.isState()
                && hud.interfaceSettings.isSelected(draggable.getName());

        boolean pouchSelected = pouchHud != null
                && pouchHud.isState()
                && pouchHud.elements.isSelected(draggable.getName());

        if (draggable instanceof Notifications || selected || pouchSelected) {
            draggable.tick();
        }
    }
}

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.player != null) {
         Packet var10000 = e.getPacket();
         Objects.requireNonNull(var10000);
         Packet var7 = var10000;
         if (var7 instanceof PlayerMoveC2SPacket movePacket) {
            if (mc.player.hasVehicle() && ServerUtil.isHolyWorld() && !movePacket.changesPosition()) {
               e.cancel();
            }
         } else if (var7 instanceof ClientCommandC2SPacket command) {
            boolean var10;
            switch (command.getMode()) {
               case START_SPRINTING -> var10 = true;
               case STOP_SPRINTING -> var10 = false;
               default -> var10 = serverSprint;
            }

            serverSprint = var10;
         } else if (var7 instanceof ClickSlotC2SPacket clickSlot) {
            int selected = mc.player.inventory.selectedSlot;
            if (clickSlot.slot() == selected + 36 || clickSlot.button() == selected) {
               this.ticks = 1;
            }
         } else if (var7 instanceof PlayerInteractItemC2SPacket) {
            if (ServerUtil.isHolyWorld() && this.ticks > 0) {
               mc.player.stopUsingItem();
               e.cancel();
            }
         } else if (var7 instanceof UpdateSelectedSlotC2SPacket slot) {
            selectedSlot = slot.getSelectedSlot();
         }
      }

      ServerUtil.packet(e);
      main.getDraggableRepository().draggable().forEach((drag) -> drag.packet(e));
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent e) {
      main.getAutoFarmRepository().onBlockUpdate(e);
   }

   @EventHandler
   public void onUsingItem(UsingItemEvent e) {
      UseItemEvent donorEvent = new UseItemEvent();
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(donorEvent);
      if (donorEvent.isCancelled()) {
         e.cancel();
      }
   }
}
