package ru.zov.implement.features.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.modules.render.Hud;

public class AutoArmor extends Module {
   public static AutoArmor getInstance() {
      return (AutoArmor)Instance.get(AutoArmor.class);
   }

   public AutoArmor() {
      super("AutoArmor", "Auto Armor", ModuleCategory.PLAYER);
      this.setup(new Setting[0]);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (!ServerUtil.isHolyWorld()) {
         if (!PlayerInventoryUtil.isServerScreen()) {
            if (PlayerInventoryComponent.script.isFinished()) {
               List<Runnable> list = new ArrayList();

               for(EquipmentSlot equipment : EquipmentSlot.values()) {
                  ItemStack equipStack = mc.player.getEquippedStack(equipment);
                  if (equipment.getType() == Type.HUMANOID_ARMOR && (!equipment.equals(EquipmentSlot.CHEST) || !equipStack.isOf(Items.ELYTRA))) {
                     int armorSlot = 8 - equipment.getEntitySlotId();
                     Slot slot = PlayerInventoryUtil.getSlot((Predicate<Slot>)((s) -> {
                        ItemStack stack = s.getStack();
                         return s.id != armorSlot && !this.isBroken(stack) && !this.hasCurseOfBinding(stack) && this.isArmor(stack) && equipment.equals(this.getEquipmentSlot(stack));
                      }), Comparator.comparingDouble((s) -> this.calculateArmorValue(s.getStack())));
                     if (slot != null && this.isBetter(slot.getStack(), equipStack)) {
                        list.add((Runnable)() -> PlayerInventoryUtil.moveItem(slot, armorSlot));
                     } else if (this.isBroken(equipStack)) {
                        Hud hud = Hud.getInstance();
                        if (slot != null) {
                           list.add((Runnable)() -> PlayerInventoryUtil.moveItem(slot, armorSlot));
                           if (hud.state && hud.notificationSettings.isSelected("Auto Armor")) {
                              Notifications var10000 = Notifications.getInstance();
                              String var10001 = String.valueOf(Formatting.GREEN);
                              var10000.addList((Text)Text.literal("Заменил - " + var10001 + this.equipName(equipment) + String.valueOf(Formatting.RESET) + " на ").append(equipStack.getName()), 3000L);
                           }
                        } else if (PlayerInventoryUtil.getSlot(Items.AIR, (Predicate<Slot>)((s) -> s.id >= 9)) != null) {
                           list.add((Runnable)() -> PlayerInventoryUtil.clickSlot(armorSlot, 0, SlotActionType.QUICK_MOVE, false));
                           if (hud.state && hud.notificationSettings.isSelected("Auto Armor")) {
                              Notifications.getInstance().addList((Text)Text.literal("Засейвил - ").append(equipStack.getName()), 3000L);
                           }
                        }
                     }
                  }
               }

               if (!list.isEmpty()) {
                  PlayerInventoryComponent.addTask(() -> {
                     list.forEach(Runnable::run);
                     PlayerInventoryUtil.updateSlots();
                  });
               }

            }
         }
      }
   }

   private float calculateArmorValue(ItemStack stack) {
      Registry<Enchantment> enchantments = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
      EquipmentSlot equipmentSlot = this.getEquipmentSlot(stack);
      if (equipmentSlot == null) {
         return Float.NEGATIVE_INFINITY;
      }

      AttributeModifiersComponent modifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
      float protection = (float)modifiers.applyOperations(EntityAttributes.ARMOR, 0.0, equipmentSlot) + (float)modifiers.applyOperations(EntityAttributes.ARMOR_TOUGHNESS, 0.0, equipmentSlot) + (float)EnchantmentHelper.getLevel((RegistryEntry)enchantments.getEntry(Enchantments.PROTECTION.getValue()).orElseThrow(), stack);
      float unbreaking = (float)EnchantmentHelper.getLevel((RegistryEntry)enchantments.getEntry(Enchantments.UNBREAKING.getValue()).orElseThrow(), stack);
      float mending = (float)EnchantmentHelper.getLevel((RegistryEntry)enchantments.getEntry(Enchantments.MENDING.getValue()).orElseThrow(), stack);
      return protection + unbreaking * 0.1F + mending * 0.2F;
   }

   private boolean hasCurseOfBinding(ItemStack stack) {
      RegistryEntry<Enchantment> curseEntry = (RegistryEntry)mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.BINDING_CURSE.getValue()).orElse(null);
      if (curseEntry == null) {
         return false;
      } else {
         return EnchantmentHelper.getLevel(curseEntry, stack) > 0;
      }
   }

   public boolean isBroken(ItemStack stack) {
      return stack.isDamageable() && (double)stack.getDamage() / (double)stack.getMaxDamage() > 0.97;
   }

   public boolean isFully(ItemStack stack) {
      return !stack.isDamageable() || (double)stack.getDamage() / (double)stack.getMaxDamage() < 0.01;
   }

   private boolean isBetter(ItemStack newArmor, ItemStack currentArmor) {
      if (currentArmor.isEmpty()) {
         return true;
      } else {
         return this.isArmor(newArmor) && this.isArmor(currentArmor) && this.calculateArmorValue(newArmor) > this.calculateArmorValue(currentArmor);
      }
   }

   private boolean isArmor(ItemStack stack) {
      return stack.isIn(ItemTags.HEAD_ARMOR) || stack.isIn(ItemTags.CHEST_ARMOR) || stack.isIn(ItemTags.LEG_ARMOR) || stack.isIn(ItemTags.FOOT_ARMOR);
   }

   private EquipmentSlot getEquipmentSlot(ItemStack stack) {
      EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
      return equippable == null ? null : equippable.slot();
   }

   @Native
   private String equipName(EquipmentSlot equipmentSlot) {
      String var10000;
      switch (equipmentSlot) {
         case FEET -> var10000 = "Ботинки";
         case LEGS -> var10000 = "Поножи";
         case CHEST -> var10000 = "Нагрудник";
         case HEAD -> var10000 = "Шлем";
         default -> var10000 = "ДЕЗЗ СВИНЬЯ СУКААА!";
      }

      return var10000;
   }
}
