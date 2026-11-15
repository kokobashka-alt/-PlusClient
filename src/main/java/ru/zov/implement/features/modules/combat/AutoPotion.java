package ru.zov.implement.features.modules.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.implement.events.player.TickEvent;

/**
 * Бросает взрывные зелья под ноги, когда нужного эффекта нет.
 * Восстановлено по структуре исходника (значения настроек в нём зашифрованы), числа по умолчанию — выбор автора порта.
 */
public class AutoPotion extends Module {
   private static final float THROW_PITCH = 90.0F;
   private static final double GROUND_CHECK_DEPTH = 3.0;
   private static final int HOTBAR_SIZE = 9;
   private static final int INVENTORY_SIZE = 36;

   private enum Potion {
      SPEED("Скорость", StatusEffects.SPEED),
      STRENGTH("Сила", StatusEffects.STRENGTH),
      FIRE_RESISTANCE("Огнестойкость", StatusEffects.FIRE_RESISTANCE),
      REGENERATION("Регенерация", StatusEffects.REGENERATION),
      RESISTANCE("Сопротивление", StatusEffects.RESISTANCE);

      private final String label;
      private final RegistryEntry<StatusEffect> effect;

      Potion(String label, RegistryEntry<StatusEffect> effect) {
         this.label = label;
         this.effect = effect;
      }
   }

   private final MultiSelectSetting potions = new MultiSelectSetting("Зелья", "Какие эффекты поддерживать")
      .value("Скорость", "Сила", "Огнестойкость", "Регенерация", "Сопротивление")
      .selected("Скорость", "Сила", "Огнестойкость");
   private final ValueSetting delay = new ValueSetting("Задержка", "Пауза между бросками (тики)")
      .setValue(20.0F).range(5.0F, 100.0F);
   private final BooleanSetting groundCheck = new BooleanSetting("Проверка земли", "Бросать только если под ногами есть блок")
      .setValue(true);
   private final BooleanSetting returnSlot = new BooleanSetting("Возвращать слот", "Вернуть выбранный слот после броска")
      .setValue(true);
   private final BindSetting forceKey = new BindSetting("Принудительный бросок", "Бросить выбранные зелья независимо от эффектов");

   private int cooldown;
   private boolean forceKeyDown;

   public AutoPotion() {
      super("AutoPotion", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.potions, this.delay, this.groundCheck, this.returnSlot, this.forceKey});
   }

   public static AutoPotion getInstance() {
      return (AutoPotion) Instance.get(AutoPotion.class);
   }

   @Override
   public void activate() {
      this.cooldown = 0;
      this.forceKeyDown = false;
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      boolean pressed = PlayerIntersectionUtil.isKey(this.forceKey);
      boolean force = pressed && !this.forceKeyDown;
      this.forceKeyDown = pressed;

      if (this.cooldown > 0) {
         this.cooldown--;
      }

      if ((!force && this.cooldown > 0) || !this.canThrow()) {
         return;
      }

      this.throwMissing(force);
   }

   private boolean canThrow() {
      if (!mc.player.isAlive() || mc.currentScreen != null || mc.player.isUsingItem()) {
         return false;
      }

      if (!this.groundCheck.isValue()) {
         return true;
      }

      Vec3d eye = mc.player.getEyePos();
      RaycastContext context = new RaycastContext(
         eye, eye.add(0.0, -GROUND_CHECK_DEPTH, 0.0),
         RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
      );
      return mc.world.raycast(context).getType() != HitResult.Type.MISS;
   }

   private void throwMissing(boolean force) {
      List<Integer> plan = new ArrayList<>();
      Set<Integer> used = new HashSet<>();
      for (Potion potion : Potion.values()) {
         if (!this.potions.isSelected(potion.label)) {
            continue;
         }

         if (!force && mc.player.hasStatusEffect(potion.effect)) {
            continue;
         }

         int slot = this.findSplash(potion.effect, used);
         if (slot != -1) {
            used.add(slot);
            plan.add(slot);
         }
      }

      if (plan.isEmpty()) {
         return;
      }

      PlayerInventory inventory = mc.player.getInventory();
      int previousSlot = inventory.getSelectedSlot();
      for (int slot : plan) {
         this.throwFrom(slot);
      }

      if (this.returnSlot.isValue() && inventory.getSelectedSlot() != previousSlot) {
         this.selectSlot(previousSlot);
      }

      this.cooldown = (int) this.delay.getValue();
   }

   // Индексы инвентаря: 0..8 хотбар, 9..35 основной инвентарь; хотбар проверяется первым
   private int findSplash(RegistryEntry<StatusEffect> effect, Set<Integer> used) {
      PlayerInventory inventory = mc.player.getInventory();
      for (int i = 0; i < INVENTORY_SIZE; i++) {
         if (used.contains(i)) {
            continue;
         }

         ItemStack stack = inventory.getStack(i);
         if (!stack.isOf(Items.SPLASH_POTION)) {
            continue;
         }

         PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
         for (StatusEffectInstance instance : contents.getEffects()) {
            if (instance.getEffectType().equals(effect)) {
               return i;
            }
         }
      }

      return -1;
   }

   private void throwFrom(int slot) {
      int selected = mc.player.getInventory().getSelectedSlot();
      if (slot < HOTBAR_SIZE) {
         if (slot != selected) {
            this.selectSlot(slot);
         }

         this.throwDown();
         return;
      }

      mc.interactionManager.clickSlot(0, slot, selected, SlotActionType.SWAP, mc.player);
      this.throwDown();
      mc.interactionManager.clickSlot(0, slot, selected, SlotActionType.SWAP, mc.player);
   }

   private void throwDown() {
      PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, AngleUtil.pitch(THROW_PITCH));
   }

   private void selectSlot(int slot) {
      mc.player.getInventory().setSelectedSlot(slot);
      mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }
}
