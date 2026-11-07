package ru.zov.common.util.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.implement.features.modules.movement.ScreenWalk;

public final class PlayerInventoryUtil implements QuickImports {
   public static void moveItem(Slot from, int to) {
      if (from != null) {
         moveItem(from.id, to, false, false);
      }

   }

   public static void moveItem(Slot from, int to, boolean task) {
      moveItem(from, to, task, false);
   }

   @Native
   public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
      if (from != null) {
         moveItem(from.id, to, task, updateInventory);
      }

   }

   @Native
   public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
      if (from != to && from != -1) {
         int count = Math.toIntExact(slots().count()) - 10;
         if (from >= count && count == 36) {
            if (task) {
               PlayerInventoryComponent.addTask(() -> clickSlot(to, from - count, SlotActionType.SWAP, false));
            } else {
               clickSlot(to, from - count, SlotActionType.SWAP, false);
            }
         } else if (task) {
            PlayerInventoryComponent.addTask(() -> moveItem(from, to, updateInventory));
         } else {
            moveItem(from, to, updateInventory);
         }

      }
   }

   public static void moveItem(int from, int to, boolean updateInventory) {
      clickSlot(from, 8, SlotActionType.SWAP, false);
      clickSlot(to, 8, SlotActionType.SWAP, false);
      clickSlot(from, 8, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }

   }

   public static void swapHand(Slot slot, Hand hand, boolean task) {
      swapHand(slot, hand, task, false);
   }

   public static void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
      if (slot != null && slot.id != -1 && (!hand.equals(Hand.OFF_HAND) || slot.inventory instanceof PlayerInventory || slot.inventory instanceof EnderChestInventory)) {
         int button = hand.equals(Hand.MAIN_HAND) ? mc.player.inventory.selectedSlot : 40;
         if (task) {
            PlayerInventoryComponent.addTask(() -> swap(slot, button, updateInventory));
         } else {
            swap(slot, button, updateInventory);
         }

      }
   }

   public static void swap(Slot slot, int button, boolean updateInventory) {
      clickSlot(slot, button, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }

   }

   public static void swapAndUse(Slot slot, String text, boolean task) {
      if (slot == null) {
         Notifications.getInstance().addList(String.valueOf(Formatting.RED) + text + String.valueOf(Formatting.RESET) + " - не найден!", 3000L);
      } else {
         swapAndUse(slot, AngleUtil.cameraAngle(), task);
      }
   }

   public static void swapAndUse(Item item) {
      swapAndUse(item, AngleUtil.cameraAngle(), true);
   }

   public static void swapAndUse(Item item, Angle angle, boolean task) {
      float cooldownProgress = ItemUtil.getCooldownProgress(item);
      if (cooldownProgress > 0.0F) {
         String time = MathUtil.round((double)cooldownProgress, 0.1) + "с";
         Notifications var6 = Notifications.getInstance();
         String var7 = String.valueOf(Formatting.RED);
         var6.addList(var7 + item.getName().getString() + String.valueOf(Formatting.RESET) + " - в кд еще " + time, 2000L);
      } else {
         Slot slot = getSlot(item);
         if (slot == null) {
            Notifications var10000 = Notifications.getInstance();
            String var10001 = String.valueOf(Formatting.RED);
            var10000.addList(var10001 + item.getName().getString() + String.valueOf(Formatting.RESET) + " - не найден!", 2000L);
         } else {
            swapAndUse(slot, angle, task);
         }
      }
   }

   public static void swapAndUse(Slot slot, Angle angle, boolean task) {
      switch (ScreenWalk.getBypassMode()) {
         case "FunTime":
            if (ServerUtil.getAnarchy() < 100 && PlayerIntersectionUtil.getProtocolVersion() > 767) {
               PlayerInventoryComponent.script.addTickStep(0, PlayerInventoryComponent::disableMoveKeys).addTickStep(1, () -> PlayerInventoryComponent.rotateToAngle(angle)).addTickStep(2, () -> {
                  swapHand(slot, Hand.MAIN_HAND, false);
                  PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, angle, true);
               }).addTickStep(3, () -> {
                  swapHand(slot, Hand.MAIN_HAND, false, true);
                  PlayerInventoryComponent.enableMoveKeys();
               });
               return;
            }
         default:
            if (task) {
               PlayerInventoryComponent.addTask(angle, () -> swapAndUse(slot, angle));
            } else {
               swapAndUse(slot, angle);
            }

            return;
         case "HolyWorld":
            PlayerInventoryComponent.script.addTickStep(1, () -> {
               swapHand(slot, Hand.MAIN_HAND, false);
               closeScreen(true);
               PlayerInventoryComponent.rotateToAngle(angle);
            }).addTickStep(2, () -> PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, angle)).addTickStep(3, () -> {
               swapHand(slot, Hand.MAIN_HAND, false, true);
               closeScreen(true);
            });
      }
   }

   public static void swapAndUse(Slot slot, Angle angle) {
      swapAndUse(slot, angle, Hand.MAIN_HAND);
   }

   public static void swapAndUse(Slot slot, Angle angle, Hand hand) {
      swapHand(slot, hand, false);
      PlayerIntersectionUtil.interactItem(hand, angle, ScreenWalk.isBypassMode("HolyWorld"));
      swapHand(slot, hand, false, true);
   }

   public static void updateSlots() {
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      ItemStack stack = ((Item)Registries.ITEM.get(MathUtil.getRandom(0, 100))).getDefaultStack();
       ItemStackHash stackHash = ItemStackHash.fromItemStack(stack, mc.getNetworkHandler().getComponentHasher());
       ItemStackHash cursorHash = ItemStackHash.fromItemStack(screenHandler.getCursorStack(), mc.getNetworkHandler().getComponentHasher());
       mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), (short)0, (byte)0, SlotActionType.PICKUP_ALL, Int2ObjectMaps.singleton(0, stackHash), cursorHash));
   }

   public static void closeScreen(boolean packet) {
      if (packet) {
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      } else {
         mc.player.closeHandledScreen();
      }

   }

   public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
      if (slot != null) {
         clickSlot(slot.id, button, clickType, silent);
      }

   }

   public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
      clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent);
   }

   public static void clickSlot(int windowId, int slotId, int button, SlotActionType clickType, boolean silent) {
      if (mc.interactionManager != null) {
          mc.interactionManager.clickSlot(windowId, slotId, button, clickType, mc.player);
      }
   }

   public static Slot getSlot(Item item) {
      return getSlot(item, (Predicate)((s) -> true));
   }

   public static Slot getSlot(Item item, Predicate<Slot> filter) {
      return getSlot(item, Comparator.comparingInt((s) -> 0), filter);
   }

   public static Slot getSlot(Predicate<Slot> filter) {
      return (Slot)slots().filter(filter).findFirst().orElse(null);
   }

   public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
      return (Slot)slots().filter(filter).max(comparator).orElse(null);
   }

   public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
      return (Slot)slots().filter((s) -> s.getStack().isOf(item)).filter(filter).max(comparator).orElse(null);
   }

   public static Slot getFoodMaxSaturationSlot() {
      return (Slot)slots().filter((s) -> !((FoodComponent)s.getStack().getOrDefault(DataComponentTypes.FOOD, FoodComponents.CHORUS_FRUIT)).canAlwaysEat()).max(Comparator.comparingDouble((s) -> (double)((FoodComponent)s.getStack().getOrDefault(DataComponentTypes.FOOD, FoodComponents.CHORUS_FRUIT)).saturation())).orElse(null);
   }

   public static Slot getSlot(Item... item) {
      return (Slot)slots().filter((s) -> Arrays.asList(item).contains(s.getStack().getItem())).findFirst().orElse(null);
   }

   public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
      return (Slot)slots().filter((s) -> StreamSupport.stream(((PotionContentsComponent)s.getStack().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)).getEffects().spliterator(), false).anyMatch((e) -> e.getEffectType().equals(effect))).findFirst().orElse(null);
   }

   public static Slot getPotionFromCategory(StatusEffectCategory category) {
      return (Slot)slots().filter((s) -> {
         ItemStack stack = s.getStack();
         if (!stack.isOf(Items.SPLASH_POTION)) {
            return false;
         } else {
            PotionContentsComponent component = (PotionContentsComponent)stack.getComponents().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            StatusEffectCategory category2 = category.equals(StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter((e) -> ((StatusEffect)e.getEffectType().value()).getCategory().equals(category)).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter((e) -> ((StatusEffect)e.getEffectType().value()).getCategory().equals(category2)).count();
            return effects >= effects2;
         }
      }).findFirst().orElse(null);
   }

   public static int getInventoryCount(Item item) {
      return IntStream.range(0, 45).filter((i) -> ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getInventory().getStack(i).isOf(item)).map((i) -> mc.player.getInventory().getStack(i).getCount()).sum();
   }

   public static int getHotbarItems(Item... items) {
      return IntStream.range(0, 9).filter((i) -> Arrays.asList(items).contains(mc.player.getInventory().getStack(i).getItem())).findFirst().orElse(-1);
   }

   public static int getHotbarSlotId(IntPredicate filter) {
      return hotbarSlotsStream(filter).orElse(-1);
   }

   public static OptionalInt hotbarSlotsStream(IntPredicate filter) {
      return IntStream.range(0, 9).filter(filter).findFirst();
   }

   public static int getCount(Predicate<Slot> filter) {
      return slots().filter(filter).mapToInt((s) -> s.getStack().getCount()).sum();
   }

   public static Slot getChestPlate(boolean elytra) {
      return elytra ? getSlot((Predicate<Slot>)((s) -> List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE).contains(s.getStack().getItem()))) : getSlot((Predicate<Slot>)((s) -> Items.ELYTRA.equals(s.getStack().getItem())));
   }

   public static Slot mainHandSlot() {
      long count = slots().count();
      int i = count == 46L ? 10 : 9;
      return (Slot)getSlots().get(Math.toIntExact(count - (long)i + (long)mc.player.getInventory().selectedSlot));
   }

   public static Slot offHandSlot() {
      return (Slot)getSlots().get(45);
   }

   public static boolean isServerScreen() {
      return slots().noneMatch((s) -> s.inventory instanceof CraftingInventory && s.inventory.size() == 4);
   }

   public static Stream<Slot> slots() {
      return getSlots().stream();
   }

   public static DefaultedList<Slot> getSlots() {
      return mc.player.currentScreenHandler.slots;
   }

   private PlayerInventoryUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
