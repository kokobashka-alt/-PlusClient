package ru.zov.implement.features.modules.movement;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.CrossbowItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.entity.SimulatedPlayer;
import ru.zov.common.util.item.ItemUsage;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.block.PushEvent;
import ru.zov.implement.events.item.UsingItemEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.PlayerCollisionEvent;
import ru.zov.implement.events.player.PushAwayEvent;
import ru.zov.implement.features.draggables.Notifications;

public class NoSlow extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final StopWatch webWatch = new StopWatch();
   private int ticks;
   public final MultiSelectSetting slowType = (new MultiSelectSetting("Target Type", "Filters the entire list of targets by type")).value("Item", "Web", "Water", "Block", "Entity Collision", "Powder Snow", "Berry");
   public final SelectSetting itemMode = (new SelectSetting("Item Mode", "Select bypass mode")).value("ReallyWorld", "HolyWorld", "FunTime CrossBow", "Grim 50%").visible(() -> this.slowType.isSelected("Item"));
   public final SelectSetting webMode = (new SelectSetting("Web Mode", "Select bypass mode")).value("Grim 2.3.72", "Ignore").visible(() -> this.slowType.isSelected("Web"));

   public static NoSlow getInstance() {
      return (NoSlow)Instance.get(NoSlow.class);
   }

   public NoSlow() {
      super("NoSlow", "No Slow", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.slowType, this.itemMode, this.webMode});
   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      Packet var3 = e.getPacket();
      if (var3 instanceof ScreenHandlerSlotUpdateS2CPacket slotUpdate) {
         if (this.itemMode.isSelected("HolyWorld", "Grim 2.3.72") && !this.stopWatch.finished((double)500.0F) && mc.player != null && (slotUpdate.getSlot() - 36 == mc.player.inventory.selectedSlot || slotUpdate.getSlot() == 45)) {
            ++this.ticks;
            e.cancel();
         }
      }

   }

   @EventHandler
   @Native
   public void onUsingItem(UsingItemEvent e) {
      if (!ItemUsage.INSTANCE.isUseItem() && mc.player != null && this.slowType.isSelected("Item")) {
         int itemTime = mc.player.getItemUseTime();
         Hand active = mc.player.getActiveHand();
         switch (e.getType()) {
            case -1:
               if (ServerUtil.isHolyWorld() && !this.stopWatch.finished((double)200.0F)) {
                  e.cancel();
               }
               break;
            case 0:
               this.ticks = 0;
               break;
            case 1:
               switch (this.itemMode.getSelected()) {
                  case "ReallyWorld":
                     if (mc.player.isSwimming()) {
                        if (this.stopWatch.every((double)100.0F)) {
                           e.cancel();
                        }
                     } else if (!mc.player.isSubmergedInWater() && mc.player.isOnGround() && PlayerIntersectionUtil.isKey(mc.options.jumpKey) && this.stopWatch.every((double)200.0F)) {
                        e.cancel();
                     } else {
                        mc.player.setSprinting(false);
                     }
                     break;
                  case "HolyWorld":
                     if (PlayerInventoryUtil.isServerScreen() || !ServerUtil.isHolyWorld()) {
                        return;
                     }

                     if (this.ticks <= 1 && itemTime > 0) {
                        PlayerInventoryComponent.script.addTickStep(1, () -> {
                           PlayerInventoryUtil.swapHand(PlayerInventoryUtil.mainHandSlot(), Hand.OFF_HAND, false);
                           PlayerInventoryUtil.swapHand(PlayerInventoryUtil.offHandSlot(), Hand.MAIN_HAND, false);
                           PlayerInventoryUtil.closeScreen(true);
                        });
                        this.stopWatch.reset();
                     }

                     if (this.ticks > 1 || itemTime % 2 == 0) {
                        e.cancel();
                     }
                     break;
                  case "FunTime CrossBow":
                     if (mc.player.getActiveItem().getItem() instanceof CrossbowItem) {
                        e.cancel();
                     }
                     break;
                  case "Grim 50%":
                     if (itemTime > 1 && (!SimulatedPlayer.simulateLocalPlayer(1).onGround || !PlayerIntersectionUtil.isKey(mc.options.jumpKey)) && this.stopWatch.every((double)100.0F)) {
                        e.cancel();
                     }
                     break;
                  case "Grim 2.3.72":
                     if (itemTime < 5) {
                        if (itemTime != 1) {
                           PlayerInventoryUtil.swapHand(PlayerInventoryUtil.mainHandSlot(), Hand.OFF_HAND, false);
                           PlayerIntersectionUtil.interactItem(active.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND);
                           PlayerIntersectionUtil.interactItem(active);
                           PlayerInventoryUtil.swapHand(PlayerInventoryUtil.offHandSlot(), Hand.MAIN_HAND, false);
                           PlayerInventoryUtil.closeScreen(true);
                           this.stopWatch.reset();
                        }

                        AutoSprint.getInstance().stopWatch.setMs(-50);
                        mc.player.setSprinting(false);
                     }

                     if (itemTime != 1) {
                        e.cancel();
                     }
               }
         }
      }

   }

   @EventHandler
   public void onPushAway(PushAwayEvent e) {
      if (this.slowType.isSelected("Entity Collision")) {
         e.cancel();
      }

   }

   @EventHandler
   public void onPush(PushEvent e) {
      if (!e.isCancelled()) {
         switch (e.getType()) {
            case WATER -> e.setCancelled(this.slowType.isSelected("Water"));
            case BLOCK -> e.setCancelled(this.slowType.isSelected("Block"));
         }
      }

   }

   @EventHandler
   @Native
   public void onPlayerCollision(PlayerCollisionEvent e) {
      Block block = e.getBlock();
      if (block.equals(Blocks.POWDER_SNOW)) {
         e.setCancelled(this.slowType.isSelected("Powder Snow"));
      } else if (block.equals(Blocks.SWEET_BERRY_BUSH)) {
         e.setCancelled(this.slowType.isSelected("Berry"));
      } else if (block.equals(Blocks.COBWEB) && this.slowType.isSelected("Web")) {
         if (this.webMode.isSelected("Ignore")) {
            e.cancel();
         } else if (this.webMode.isSelected("Grim 2.3.72")) {
            if (PlayerIntersectionUtil.getProtocolVersion() < 758) {
               Notifications.getInstance().addListIfEmpty("[Web, Grim 2.3.72] нужна версия " + String.valueOf(Formatting.RED) + "1.18.1-1.21.8", 2000L);
               return;
            }

            double[] xz = MovingUtil.calculateDirection(0.64);
            mc.player.setVelocity(xz[0], mc.options.jumpKey.isPressed() ? 1.2 : (mc.options.sneakKey.isPressed() ? (double)-2.0F : (double)0.0F), xz[1]);
         }
      }

   }
}
