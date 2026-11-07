package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.api.system.sound.SoundManager;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.container.OpenScreenEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.features.modules.render.Hud;

public class Notifications extends AbstractDraggable {
   public final List<Notification> list = new ArrayList();
   private final List<Stack> stacks = new ArrayList();

   public static Notifications getInstance() {
      return (Notifications)Instance.getDraggable(Notifications.class);
   }

   public Notifications() {
      super("Notifications", 0, 50, 100, 15, true);
   }

   public void tick() {
      this.list.forEach((notif) -> {
         if (System.currentTimeMillis() > notif.removeTime || notif.text.getString().contains("Пример Уведомления") && !PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            notif.anim.setDirection(Direction.BACKWARDS);
         }

      });
      this.list.removeIf((notif) -> notif.anim.isFinished(Direction.BACKWARDS));

      while(!this.stacks.isEmpty()) {
         this.addTextIfNotEmpty(Notifications.TypePickUp.INVENTORY, "Подняты предметы: ");
         this.addTextIfNotEmpty(Notifications.TypePickUp.SHULKER_INVENTORY, "Сложены предметы в шалкер: ");
         this.addTextIfNotEmpty(Notifications.TypePickUp.SHULKER, "Поднят шалкер с: ");
      }

   }

   public void packet(PacketEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         Packet var10000 = e.getPacket();
         Objects.requireNonNull(var10000);
         Packet var5 = var10000;
         if (var5 instanceof ItemPickupAnimationS2CPacket item) {
            if (Hud.getInstance().notificationSettings.isSelected("Item Pick Up") && item.getCollectorEntityId() == ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getId()) {
               Entity var11 = ((ClientWorld)Objects.requireNonNull(mc.world)).getEntityById(item.getEntityId());
               if (var11 instanceof ItemEntity entity) {
                  ItemStack itemStack = entity.getStack();
                  ContainerComponent component = (ContainerComponent)itemStack.get(DataComponentTypes.CONTAINER);
                  if (component == null) {
                     Text itemText = itemStack.getName();
                     if (itemText.getContent().toString().equals("empty")) {
                        MutableText text = Text.empty().append(itemText);
                        if (itemStack.getCount() > 1) {
                           String var10001 = String.valueOf(Formatting.RESET);
                           text.append(var10001 + " [" + String.valueOf(Formatting.RED) + itemStack.getCount() + String.valueOf(Formatting.GRAY) + "x" + String.valueOf(Formatting.RESET) + "]");
                        }

                        this.stacks.add(new Stack(Notifications.TypePickUp.INVENTORY, text));
                     }
                  } else {
                     component.stream().filter((s) -> s.getName().getContent().toString().equals("empty")).forEach((stack) -> {
                        MutableText text = Text.empty().append(stack.getName());
                        if (stack.getCount() > 1) {
                           String var10001 = String.valueOf(Formatting.RESET);
                           text.append(var10001 + " [" + String.valueOf(Formatting.RED) + stack.getCount() + String.valueOf(Formatting.GRAY) + "x" + String.valueOf(Formatting.RESET) + "]");
                        }

                        this.stacks.add(new Stack(Notifications.TypePickUp.SHULKER, text));
                     });
                  }
               }
            }
         } else if (var5 instanceof ScreenHandlerSlotUpdateS2CPacket slot && Hud.getInstance().notificationSettings.isSelected("Item Pick Up")) {
            int slotId = slot.getSlot();
            ContainerComponent updatedContainer = (ContainerComponent)slot.getStack().get(DataComponentTypes.CONTAINER);
            if (updatedContainer != null && slotId < ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).currentScreenHandler.slots.size() && slot.getSyncId() == 0) {
               ContainerComponent currentContainer = (ContainerComponent)mc.player.currentScreenHandler.getSlot(slotId).getStack().get(DataComponentTypes.CONTAINER);
               if (currentContainer != null) {
                  updatedContainer.stream().filter((stack) -> currentContainer.stream().noneMatch((s) -> Objects.equals(s.getComponents(), stack.getComponents()) && s.toString().equals(stack.toString()))).forEach((stack) -> {
                     MutableText text = Text.empty().append(stack.getName());
                     this.stacks.add(new Stack(Notifications.TypePickUp.SHULKER_INVENTORY, text));
                  });
               }
            }
         }
      }
   }

   public void openScreen(OpenScreenEvent e) {
      if (e.getScreen() instanceof ChatScreen) {
         this.addList("Пример Уведомления", 99999999L);
      }

   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(12, Fonts.Type.DEFAULT);
      this.setX((window.getScaledWidth() - this.getWidth()) / 2);
      float offsetY = 0.0F;
      float offsetX = 5.0F;

      for(Notification notification : this.list) {
         float anim = notification.anim.getOutput().floatValue();
         float width = font.getStringWidth(notification.text) + offsetX * 2.0F;
         float startY = (float)this.getY() + offsetY;
         float startX = (float)this.getX() + ((float)this.getWidth() - width) / 2.0F;
         MathUtil.multAlpha(anim, () -> {
            blur.render(ShapeProperties.create(matrix, (double)startX, (double)startY, (double)width, (double)this.getHeight()).round(3.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
            font.drawText(matrix, notification.text, (double)((int)(startX + offsetX)), (double)(startY + 6.5F));
         });
         offsetY += (float)(this.getHeight() + 3) * anim;
      }

   }

   private void addTextIfNotEmpty(TypePickUp type, String prefix) {
      MutableText text = Text.empty();
      List<Stack> list = this.stacks.stream().filter((stackx) -> stackx.type.equals(type)).toList();
      int i = 0;

      for(int size = list.size(); i < size; ++i) {
         Stack stack = (Stack)list.get(i);
         if (stack.type == type) {
            text.append(stack.text);
            this.stacks.remove(stack);
            if (text.getString().length() > 150) {
               break;
            }

            if (i + 1 != size) {
               text.append(" ,  ");
            }
         }
      }

      if (!text.equals(Text.empty())) {
         this.addList((Text)Text.empty().append(prefix).append(text), 8000L);
      }

   }

   public void addListIfEmpty(String text, long removeTime) {
      if (this.list.isEmpty()) {
         this.addList((String)text, removeTime, (SoundEvent)null);
      }

   }

   public void addList(String text, long removeTime) {
      this.addList((String)text, removeTime, (SoundEvent)null);
   }

   public void addList(Text text, long removeTime) {
      this.addList((Text)text, removeTime, (SoundEvent)null);
   }

   public void addList(String text, long removeTime, SoundEvent sound) {
      this.addList((Text)Text.empty().append(text), removeTime, sound);
   }

   public void addList(Text text, long removeTime, SoundEvent sound) {
      Hud hud = Hud.getInstance();
      if (hud.isState() && hud.interfaceSettings.isSelected("Notifications")) {
         this.list.add(new Notification(text, (new DecelerateAnimation()).setMs(300).setValue((double)1.0F), System.currentTimeMillis() + removeTime));
         if (this.list.size() > 12) {
            this.list.removeLast();
         }

         this.list.sort(Comparator.comparingDouble((notif) -> (double)(-notif.removeTime)));
         if (sound != null) {
            SoundManager.playSound(sound);
         }

      } else {
         if (!text.getString().contains("Пример")) {
            this.logDirect(new Text[]{text});
         }

      }
   }

   public static record Notification(Text text, Animation anim, long removeTime) {
   }

   public static record Stack(TypePickUp type, MutableText text) {
   }

   public static enum TypePickUp {
      INVENTORY,
      SHULKER,
      SHULKER_INVENTORY;

      // $FF: synthetic method
      private static TypePickUp[] $values() {
         return new TypePickUp[]{INVENTORY, SHULKER, SHULKER_INVENTORY};
      }
   }
}
