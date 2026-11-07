package ru.zov.implement.features.modules.misc;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.other.StringUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.core.Main;
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
import ru.zov.implement.features.modules.render.Hud;

public class AutoBuy extends Module implements AuctionItems {
   public final Map<Integer, ItemStack> slots = new ConcurrentHashMap();
   public final PlayerInventory playerInventory = new PlayerInventory((PlayerEntity)null, new net.minecraft.entity.EntityEquipment());
   private final StopWatch updateWatch = new StopWatch();
   private final StopWatch updatingWatch = new StopWatch();
   private final StopWatch buyingWatch = new StopWatch();
   private int priority;
   private int clicks;
   private int lastAnarchy = 1;
   private int air;
   private final Script script = new Script();
   private boolean open;
   private boolean search;
   private boolean updated;
   private TextFieldWidget textField;
   private Slot selectedSlot;
   private final BindSetting bind = new BindSetting("Open UI", "key to open UI");

   public static AutoBuy getInstance() {
      return (AutoBuy)Instance.get(AutoBuy.class);
   }

   public AutoBuy() {
      super("AutoBuy", "Auto Buy", ModuleCategory.MISC);
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
   @Native
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof ClickSlotC2SPacket clickSlot) {
         if (clickSlot.syncId() < 0) {
            e.cancel();
         }
      } else if (var2 instanceof ScreenHandlerSlotUpdateS2CPacket slotUpdate) {
         int id = slotUpdate.getSlot();
         ItemStack stack = slotUpdate.getStack();
         if (id < 45) {
            if (!stack.isEmpty() && !stack.isOf(Items.BARRIER)) {
               this.slots.put(id, stack);
            } else {
               this.slots.remove(id);
            }
         } else if (id == 47 && stack.isOf(Items.EMERALD)) {
            this.updateWatch.setMs(-200);
            this.updated = true;
         }
      } else if (var2 instanceof OpenScreenS2CPacket) {
         this.updateWatch.setMs(-200);
         this.updated = true;
         this.slots.clear();
      } else if (var2 instanceof CloseHandledScreenC2SPacket || var2 instanceof CloseScreenS2CPacket) {
         this.slots.clear();
      } else if (var2 instanceof DisconnectS2CPacket) {
         this.search = false;
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.script.update().cleanupIfFinished();
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (this.search && this.canStartBuy() && mc.player.inventory.getEmptySlot() != -1) {
         if (ServerUtil.getHeader().getString().contains("Лобби")) {
            if (this.updateWatch.every((double)15000.0F)) {
               Main.getInstance().getRCTRepository().reconnect(this.lastAnarchy);
            }
         } else if (ServerUtil.getWorldType().equals("spawn_world")) {
            int windowId = mc.player.currentScreenHandler.syncId;
            Scoreboard scoreboard = ((ClientWorld)Objects.requireNonNull(mc.world)).getScoreboard();
            Collection<ScoreboardEntry> scoreboardEntries = scoreboard.getScoreboardEntries(scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR));
            scoreboardEntries.stream().map((entry) -> Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name())).filter(Objects::nonNull).map(Text::getString).filter((s) -> s.contains("Монеток: ")).map((s) -> Integer.valueOf(StringUtils.substringBetween(s.replaceAll(" ", "").replaceAll(",", ""), "Монеток:", "¤"))).findFirst().ifPresentOrElse((balance) -> {
               this.slots.entrySet().stream().filter((entry) -> this.validStack(((ItemStack)entry.getValue()).copy(), balance)).findAny().ifPresentOrElse((entry) -> {
                  if (this.updatingWatch.finished()) {
                     PlayerInventoryUtil.clickSlot(windowId, (Integer)entry.getKey(), 0, this.randomAction(), true);
                     PlayerInventoryUtil.clickSlot(windowId + 1, MathUtil.getRandom(0, 2), 0, this.randomAction(), true);
                     this.slots.remove(entry.getKey());
                     this.updatingWatch.setMs(-600);
                     this.priority += 2;
                     this.clicks += 2;
                  }

               }, () -> {
                  if (this.updated && this.updateWatch.finished() && this.updatingWatch.finished()) {
                     if (this.priority > 200) {
                        int anarchy = ServerUtil.getAnarchy();
                        Main.getInstance().getRCTRepository().reconnect(this.lastAnarchy = anarchy > 62 ? 1 : anarchy + (List.of(2, 16, 34, 40, 49).contains(anarchy + 1) ? 2 : 1));
                        this.priority = 0;
                     } else {
                        PlayerInventoryUtil.clickSlot(windowId, 47, 0, this.randomAction(), true);
                     }

                     this.updated = false;
                     this.slots.clear();
                     ++this.priority;
                     ++this.clicks;
                  }

               });
               if (this.updateWatch.every((double)4000.0F)) {
                  mc.player.networkHandler.sendChatMessage("/ah");
                  this.updatingWatch.reset();
                  this.slots.clear();
               }

            }, () -> {
               if (scoreboardEntries.isEmpty() && this.updatingWatch.every((double)3000.0F)) {
                  mc.player.networkHandler.sendChatCommand("scoreboard full");
                  mc.player.networkHandler.sendChatCommand("scoreboard full");
               }

            });
         } else if (this.updateWatch.every((double)5000.0F)) {
            mc.player.networkHandler.sendChatCommand("spawn");
         }
      }

   }

   @EventHandler
   public void onDrawableChild(DrawableChildEvent e) {
      Screen screen = e.screen();
      String title = screen.getTitle().getString();
      if (title.equals(this.getVisibleName()) || title.toLowerCase().contains("аукцион")) {
         int x = (window.getScaledWidth() - screen.width) / 2 - 50;
         int halfWindow = window.getScaledHeight() / 2;
         int halfHeight = title.equals(this.getVisibleName()) ? 160 : 111;
         e.add(ButtonWidget.builder(Text.literal(this.search ? "Выключить" : "Включить"), (button) -> this.script.addTickStep(0, () -> {
               this.clicks = 0;
               this.buyingWatch.reset();
               this.updated = false;
               this.search = !this.search;
               button.setFocused(false);
               button.setMessage(Text.literal(this.search ? "Выключить" : "Включить"));
            })).width(100).position(x, halfWindow - halfHeight - 28).build());
         e.add(this.textField = new TextFieldWidget(mc.textRenderer, x, halfWindow + halfHeight + 8, 100, 20, Text.literal("Цена")));
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
      this.script.cleanup();
      this.slots.clear();
   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      org.joml.Matrix3x2fStack matrix = e.context().getMatrices();
      HandledScreen<?> screen = e.screen();
      Slot hover = screen.focusedSlot;
      String title = screen.getTitle().getString();
      if (title.equals(this.getVisibleName()) || title.toLowerCase().contains("аукцион")) {
         int time = this.buyingWatch.elapsedInt();
         MathUtil.multAlpha((float)time / 500.0F, () -> {
            int ms = this.search ? time : 1;
            int multiply = Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Boss Bars") ? 22 : 19;
            int x = mc.getWindow().getScaledWidth() / 2;
            int y = mc.inGameHud.getBossBarHud().bossBars.size() * multiply + 10;
            FontRenderer font = Fonts.getSize(15);
            font.drawCenteredString(matrix, "Среднее время обновления: " + String.valueOf(Formatting.RED) + ms / Math.max(this.clicks, 1) + "ms", (double)x, (double)y, ColorUtil.getText());
            font.drawCenteredString(matrix, "Кликов за время работы: " + String.valueOf(Formatting.RED) + this.clicks, (double)x, (double)(y + 9), ColorUtil.getText());
            font.drawCenteredString(matrix, "Время работы: " + String.valueOf(Formatting.RED) + StringUtil.getDurationMS(ms / 100, true), (double)x, (double)(y + 18), ColorUtil.getText());
         });
      }

      if (mc.player != null && title.equals(this.getVisibleName())) {
         if (PlayerIntersectionUtil.isKey(mc.options.pickItemKey) && !this.open && hover != null) {
            ItemStack stack = hover.getStack();
            if (!stack.isEmpty()) {
               mc.player.networkHandler.sendChatCommand("ah search " + AuctionItems.getMessage(stack));
               this.open = true;
            }
         }

         if (this.selectedSlot != null) {
            ItemStack stack = this.selectedSlot.getStack();
            if (!stack.isEmpty()) {
               matrix.pushMatrix();
               matrix.translate((float)screen.x, (float)screen.y);
               PlayerInventoryUtil.getSlots().stream().filter((s) -> s.id <= 54).forEach((is) -> ((ItemEnchantmentsComponent)is.getStack().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)).getEnchantmentEntries().stream().findFirst().ifPresent((entry) -> stack.getEnchantments().getEnchantmentEntries().stream().filter((entry2) -> ((RegistryEntry)entry2.getKey()).equals(entry.getKey())).findFirst().ifPresent((entry2) -> {
                        if (entry.getIntValue() <= entry2.getIntValue()) {
                           int color = entry.getIntValue() == entry2.getIntValue() ? ColorUtil.getColor(120, 140, 255) : ColorUtil.getColor(140, 255, 120);
                           Render3DUtil.highlightSlot(matrix, is, color);
                        }

                     })));
               Render3DUtil.highlightSlot(matrix, this.selectedSlot, ColorUtil.getColor(120, 140, 255));
               matrix.popMatrix();
            }
         }
      }

   }

   @EventHandler
   @Native
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
                     if (this.selectedSlot != null && slotId < 54) {
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
                     } else if (mc.player != null && slotId > 53 && button == 1 && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        if ((this.selectedSlot == null || !this.selectedSlot.equals(slot)) && !slot.getStack().isEmpty()) {
                           this.selectedSlot = slot;
                           this.textField.setText("" + AuctionItems.getPrice(slot.getStack()));
                        } else {
                           this.selectedSlot = null;
                           this.textField.setText("0");
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

         if (slotId < 54) {
            ((Slot)slots.get(slotId)).getStack().getComponents().forEach((component) -> {
               PrintStream var10000 = System.out;
               String var10001 = String.valueOf(component.type());
               var10000.println(var10001 + " | " + String.valueOf(component.value()));
            });
         }

      }
   }

   @Native
   private void openUI() {
      if (mc.player != null) {
         SimpleInventory inventory = new SimpleInventory(54);
         List<ItemStack> var10000 = this.getStacks();
         Objects.requireNonNull(inventory);
         var10000.forEach(inventory::addStack);
         GenericContainerScreen genericContainerScreen = new GenericContainerScreen((GenericContainerScreenHandler)(mc.player.currentScreenHandler = GenericContainerScreenHandler.createGeneric9x6(-52, this.playerInventory, inventory)), this.playerInventory, Text.literal(this.getVisibleName()));
         genericContainerScreen.backgroundHeight = 330;
         mc.setScreen(genericContainerScreen);
      }

   }

   private List<ItemStack> getStacks() {
      return this.selectedSlot != null && !this.selectedSlot.getStack().isEmpty() ? this.getEnchantedBooks(this.selectedSlot.getStack()) : this.getAutoBuyItems().stream().map(ItemStack::copy).toList();
   }

   private boolean validStack(ItemStack stack, int money) {
      int price = AuctionUtil.getPrice(stack);
      return (PlayerInventoryUtil.getInventoryCount(Items.TOTEM_OF_UNDYING) < 4 || !stack.isOf(Items.TOTEM_OF_UNDYING)) && price != -1 && money >= price && this.playerInventory.main.stream().anyMatch((stack1) -> AuctionItems.getPrice(stack1) >= price / Math.max(stack.getCount(), 1) && this.validStack(stack, stack1));
   }

   private boolean validStack(ItemStack stack, ItemStack stack2) {
      return stack.isOf(stack2.getItem()) && this.validEnchanted(stack, stack2) && this.validPotion(stack, stack2) && this.validId(stack, stack2);
   }

   private boolean validId(ItemStack stack, ItemStack stack2) {
      String id = ((NbtComponent)stack2.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).copyNbt().getString("id", "");
      return id.isEmpty() || ((NbtComponent)stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).toString().contains(id);
   }

   private boolean validEnchanted(ItemStack stack, ItemStack stack2) {
      for(Text text : ((LoreComponent)stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)).lines()) {
         String string = text.getString();
         if (string.isEmpty()) {
            break;
         }

         int lvl = string.contains("III") ? 3 : (string.contains("II") ? 2 : 1);
         EnchantmentHelper.apply(stack, (builder) -> builder.add(AuctionItems.enchantBuilder(Text.of(string.replaceAll(" ", "").replaceAll("I", ""))), lvl));
      }

      return stack2.getEnchantments().getEnchantmentEntries().stream().allMatch((entry) -> stack.getEnchantments().getEnchantmentEntries().stream().anyMatch((entry1) -> entry1.getIntValue() >= entry.getIntValue() && ((Enchantment)((RegistryEntry)entry.getKey()).value()).toString().equals(((Enchantment)((RegistryEntry)entry1.getKey()).value()).toString())));
   }

   private boolean validPotion(ItemStack stack, ItemStack stack2) {
      return StreamSupport.stream(((PotionContentsComponent)stack2.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)).getEffects().spliterator(), false).allMatch((sei) -> StreamSupport.stream(((PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)).getEffects().spliterator(), false).anyMatch((sei2) -> sei2.getEffectType().equals(sei.getEffectType()) && sei2.getDuration() >= sei.getDuration() && sei2.getAmplifier() >= sei.getAmplifier()));
   }

   private boolean canStartBuy() {
      return !ServerUtil.isPvp() && ServerUtil.isHolyWorld();
   }

   private SlotActionType randomAction() {
      SlotActionType var10000;
      switch (MathUtil.getRandom(0, 2)) {
         case 0 -> var10000 = SlotActionType.PICKUP;
         case 1 -> var10000 = SlotActionType.SWAP;
         default -> var10000 = SlotActionType.QUICK_MOVE;
      }

      return var10000;
   }
}
