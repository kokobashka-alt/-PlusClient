package ru.zov.implement.features.modules.misc;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.container.ClickSlotEvent;
import ru.zov.implement.events.container.CloseScreenEvent;
import ru.zov.implement.events.container.HandledScreenEvent;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.DrawableChildEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.modules.misc.auction.AuctionItems;
import ru.zov.implement.features.modules.misc.auction.AuctionUtil;

public class InventoryCollector extends Module implements AuctionItems {
   public final PlayerInventory playerInventory = new PlayerInventory((PlayerEntity)null, new net.minecraft.entity.EntityEquipment());
   private final Map<ItemStack, Integer> buyPriority = new HashMap();
   private final StopWatch stopWatch = new StopWatch();
   private final Script script = new Script();
   private TextFieldWidget textField;
   private boolean open;
   private boolean collection;
   private Slot selectedSlot;
   private int balance;
   private final BindSetting bind = new BindSetting("Open UI", "key to open UI");

   public static InventoryCollector getInstance() {
      return (InventoryCollector)Instance.get(InventoryCollector.class);
   }

   public InventoryCollector() {
      super("InventoryCollector", "Inventory Collector", ModuleCategory.MISC);
      this.setup(new Setting[]{this.bind});
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.bind)) {
         this.openUI();
      } else if (e.isKeyDown(257) && mc.currentScreen != null && mc.currentScreen.getTitle().equals(Text.literal(this.getVisibleName()))) {
         if (this.selectedSlot != null) {
            try {
               AuctionItems.setPrice(this.selectedSlot.getStack(), Integer.parseInt(this.textField.getText()));
            } catch (NumberFormatException var3) {
               this.textField.setText(String.valueOf(AuctionItems.getPrice(this.selectedSlot.getStack())));
            }
         }

         this.textField.setFocused(false);
      }

   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof ClickSlotC2SPacket clickSlot) {
         if (clickSlot.syncId() < 0) {
            e.cancel();
         }
      } else if (var2 instanceof GameMessageS2CPacket message) {
         String text = message.content().getString();
         String[] split = text.split("Ваш баланс: ");
         if (split.length > 1) {
            this.balance = Integer.parseInt(split[1].replace("$", "").replace(",", ""));
         } else if (text.contains("[☃] Вы успешно купили ") || text.contains("[☃] У Вас купили ")) {
            this.updateBalance();
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.script.update().cleanupIfFinished();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.collection && this.balance >= 10 && this.stopWatch.every((double)1100.0F)) {
         Stream<ItemStack> var10000 = this.playerInventory.main.stream().filter((stack) -> !stack.isEmpty() && !this.validStack(stack, mc.player.inventory.main));
         Map<ItemStack, Integer> var10001 = this.buyPriority;
         Objects.requireNonNull(var10001);
         var10000.min(Comparator.comparingInt(var10001::get)).ifPresent((stack) -> {
            String message = AuctionItems.getMessage(stack);
            Screen patt0$temp = mc.currentScreen;
            if (patt0$temp instanceof GenericContainerScreen screen) {
               String title = screen.getTitle().getString().toLowerCase();
               if (title.contains("подтвердите")) {
                  PlayerInventoryUtil.clickSlot(MathUtil.getRandom(0, 2), 0, SlotActionType.QUICK_MOVE, true);
               } else if (title.contains(message)) {
                  ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.stream().filter((slot) -> AuctionUtil.getPrice(slot) <= this.balance && this.validStack(stack, slot.getStack())).min(Comparator.comparingDouble(AuctionUtil::getPrice)).ifPresentOrElse((slot) -> PlayerInventoryUtil.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true), () -> this.buyPriority.computeIfAbsent(stack, (stack2) -> 1));
               }
            } else if (PlayerInventoryUtil.isServerScreen()) {
               PlayerInventoryUtil.closeScreen(false);
            } else {
               mc.player.networkHandler.sendChatCommand("ah search " + message);
            }

         });
      }

   }

   @EventHandler
   public void onDrawableChild(DrawableChildEvent e) {
      Screen screen = e.screen();
      Text title = screen.getTitle();
      if (this.canStartBuy() && title.equals(Text.literal(this.getVisibleName())) || this.collection) {
         int x = (window.getScaledWidth() - screen.width) / 2 - 50;
         int halfHeight = window.getScaledHeight() / 2;
         e.add(ButtonWidget.builder(Text.literal(this.collection ? "Закончить Сбор" : "Начать Сбор"), (button) -> {
            this.balance = 0;
            this.updateBalance();
            this.buyPriority.clear();
            this.collection = !this.collection;
            this.script.addTickStep(0, () -> button.setFocused(false));
            button.setMessage(Text.literal(this.collection ? "Закончить Сбор" : "Начать Сбор"));
         }).width(100).position(x, halfHeight - 135).build());
         e.add(this.textField = new TextFieldWidget(mc.textRenderer, x, halfHeight + 115, 100, 20, Text.literal("Цена")));
      }

   }

   @EventHandler
   public void onCloseScreen(CloseScreenEvent e) {
      if (this.open) {
         if (!e.getScreen().getTitle().equals(Text.literal(this.getVisibleName()))) {
            this.script.addTickStep(0, this::openUI);
         }

         this.open = false;
      }

   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.collection = false;
   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      org.joml.Matrix3x2fStack matrix = e.context().getMatrices();
      HandledScreen<?> screen = e.screen();
      Slot hover = screen.focusedSlot;
      if (mc.player != null && screen.getTitle().equals(Text.literal(this.getVisibleName()))) {
         if (PlayerIntersectionUtil.isKey(mc.options.pickItemKey) && !this.open && hover != null) {
            List<ItemStack> list = this.getCollectorItems();
            if (list.size() > hover.id) {
               ClientPlayNetworkHandler var10000 = mc.player.networkHandler;
               Object var10001 = list.get(hover.id);
               var10000.sendChatCommand("ah search " + AuctionItems.getMessage((ItemStack)var10001));
               this.open = true;
            }
         }

         matrix.pushMatrix();
         matrix.translate((float)screen.x, (float)screen.y);
         mc.player.currentScreenHandler.slots.forEach((slot) -> {
            ItemStack stack = slot.getStack();
            if (slot.id > 54 && !stack.isEmpty() && this.validStack(stack, mc.player.inventory.main)) {
               Render3DUtil.highlightSlot(matrix, slot, ColorUtil.getColor(140, 255, 120));
            }

         });
         if (this.selectedSlot != null) {
            ItemStack stack = this.selectedSlot.getStack();
            if (!stack.isEmpty()) {
               PlayerInventoryUtil.getSlots().stream().filter((s) -> s.id <= 54).forEach((is) -> ((ItemEnchantmentsComponent)is.getStack().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)).getEnchantmentEntries().stream().findFirst().ifPresent((entry) -> stack.getEnchantments().getEnchantmentEntries().stream().filter((entry2) -> ((RegistryEntry)entry2.getKey()).equals(entry.getKey())).findFirst().ifPresent((entry2) -> {
                        if (entry.getIntValue() <= entry2.getIntValue()) {
                           int color = entry.getIntValue() == entry2.getIntValue() ? ColorUtil.getColor(120, 140, 255) : ColorUtil.getColor(140, 255, 120);
                           Render3DUtil.highlightSlot(matrix, is, color);
                        }

                     })));
               Render3DUtil.highlightSlot(matrix, this.selectedSlot, ColorUtil.getColor(120, 140, 255));
            }
         }

         matrix.popMatrix();
      }

   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent e) {
      SlotActionType action = e.getActionType();
      int slotId = e.getSlotId();
      int button = e.getButton();
      List<Slot> slots = PlayerInventoryUtil.getSlots();
      if (slotId >= 0 && slotId <= slots.size()) {
         Screen var7 = mc.currentScreen;
         if (var7 instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)var7;
            if (screen.getTitle().equals(Text.literal(this.getVisibleName()))) {
               switch (action) {
                  case SWAP:
                     e.cancel();
                     break;
                  case PICKUP:
                     Slot slot = (Slot)slots.get(slotId);
                     if (this.selectedSlot != null && slotId <= 54) {
                        ((ItemEnchantmentsComponent)slot.getStack().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)).getEnchantmentEntries().stream().findFirst().ifPresent((entry) -> EnchantmentHelper.apply(this.selectedSlot.getStack(), (builder) -> {
                              AtomicBoolean aBoolean = new AtomicBoolean(false);
                              builder.remove((entry1) -> {
                                 boolean bool = entry1.getKeyOrValue().equals(((RegistryEntry)entry.getKey()).getKeyOrValue()) && builder.getLevel(entry1) == entry.getIntValue();
                                 if (bool) {
                                    aBoolean.set(true);
                                 }

                                 return bool;
                              });
                              if (!aBoolean.get()) {
                                 builder.set((RegistryEntry)entry.getKey(), entry.getIntValue());
                              }

                           }));
                     } else if (mc.player != null && slotId > 54 && button == 1 && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        if (this.selectedSlot != null) {
                           this.selectedSlot = null;
                           this.textField.setText("0");
                        } else {
                           this.selectedSlot = slot;
                           this.textField.setText("" + AuctionItems.getPrice(slot.getStack()));
                        }

                        e.cancel();
                     }
               }

               if (this.selectedSlot != null) {
                  e.cancel();
               }

               this.script.addTickStep(0, () -> AuctionItems.updateScreenSlots(screen.getScreenHandler(), this.getStacks()));
               return;
            }
         }

         if (slotId <= 54) {
            ((Slot)slots.get(slotId)).getStack().getComponents().forEach((component) -> {
               PrintStream var10000 = System.out;
               String var10001 = String.valueOf(component.type());
               var10000.println(var10001 + " | " + String.valueOf(component.value()));
            });
         }

      }
   }

   private void openUI() {
      if (mc.player != null) {
         SimpleInventory inventory = new SimpleInventory(54);
         List<ItemStack> var10000 = this.getStacks();
         Objects.requireNonNull(inventory);
         var10000.forEach(inventory::addStack);
         mc.setScreen(new GenericContainerScreen((GenericContainerScreenHandler)(mc.player.currentScreenHandler = GenericContainerScreenHandler.createGeneric9x6(-52, this.playerInventory, inventory)), this.playerInventory, Text.literal(this.getVisibleName())));
      }

   }

   private void updateBalance() {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatCommand("/bal");
      }

   }

   private List<ItemStack> getStacks() {
      return this.selectedSlot != null && !this.selectedSlot.getStack().isEmpty() ? this.getEnchantedBooks(this.selectedSlot.getStack()) : this.getCollectorItems().stream().map(ItemStack::copy).toList();
   }

   private boolean validStack(ItemStack stack, List<ItemStack> stacks) {
      int count = 0;

      for(ItemStack s : stacks) {
         if (this.validStacks(stack, s)) {
            count += s.getCount();
            if (count >= stack.getCount()) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean validStack(ItemStack stack1, ItemStack stack2) {
      return this.validStacks(stack1, stack2) && stack1.getCount() <= stack2.getCount();
   }

   private boolean validStacks(ItemStack stack, ItemStack stack2) {
      return stack.isOf(stack2.getItem()) && this.validEnchanted(stack, stack2) && this.validPotion(stack, stack2);
   }

   private boolean validEnchanted(ItemStack stack, ItemStack stack2) {
      return stack.getEnchantments().getEnchantmentEntries().stream().allMatch((entry) -> stack2.getEnchantments().getEnchantmentEntries().stream().anyMatch((entry1) -> entry1.getIntValue() >= entry.getIntValue() && ((Enchantment)((RegistryEntry)entry.getKey()).value()).toString().equals(((Enchantment)((RegistryEntry)entry1.getKey()).value()).toString())));
   }

   private boolean validPotion(ItemStack stack, ItemStack stack2) {
      return StreamSupport.stream(((PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)).getEffects().spliterator(), false).anyMatch((sei) -> StreamSupport.stream(((PotionContentsComponent)stack2.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)).getEffects().spliterator(), false).anyMatch((sei2) -> sei2.getEffectType().equals(sei.getEffectType()) && sei2.getDuration() >= sei.getDuration() && sei2.getAmplifier() >= sei.getAmplifier()));
   }

   private boolean canStartBuy() {
      return (ServerUtil.isFunTime() || ServerUtil.isHolyWorld()) && ServerUtil.getAnarchy() != -1;
   }
}
