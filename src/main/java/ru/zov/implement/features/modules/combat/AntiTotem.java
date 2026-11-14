package ru.zov.implement.features.modules.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.AttackEvent;
import ru.zov.implement.events.player.TickEvent;

/**
 * When the player you are fighting loses a totem to you, puts the strongest "ball" (a player_head item) into the
 * offhand, then returns the previous offhand item after a short hold. "Strongest" means the largest gain of the
 * ATTACK_DAMAGE attribute that the head applies in the offhand (ATTRIBUTE_MODIFIERS component).
 *
 * The swap goes through PlayerInventoryUtil.swapHand with the task queue, the same path AutoTotem and AutoSwap use
 * (movement keys are released around the click), instead of a raw window click.
 */
public class AntiTotem extends Module implements QuickLogger {
   /** EntityStatusS2CPacket status of the vanilla "totem of undying used" animation. */
   private static final int STATUS_TOTEM_USED = 35;
   private static final long POP_MAX_AGE_MS = 500L;
   private static final long RECENT_ATTACK_MS = 1500L;
   private static final double COOLDOWN_MS = 1500.0;
   private static final int SWAP_TIMEOUT_TICKS = 12;
   /** Slot ids of the main inventory and the hotbar in the player screen handler (offhand is 45, armor 5..8). */
   private static final int FIRST_STORAGE_SLOT = 9;
   private static final int LAST_STORAGE_SLOT = 44;

   private final ValueSetting range = new ValueSetting("Max Distance", "Maximum distance to the player who lost the totem")
      .setValue(8.0F)
      .range(2.0F, 16.0F);
   private final ValueSetting hold = new ValueSetting("Hold Time", "Ticks the head stays in the offhand before the previous item comes back")
      .setValue(20.0F)
      .range(5.0F, 100.0F);
   private final BooleanSetting onlyMyTarget = new BooleanSetting("Only My Target", "React only to the Aura/TriggerBot target or a player you just hit")
      .setValue(true);
   private final BooleanSetting restore = new BooleanSetting("Restore Item", "Return the previous offhand item after the hold time")
      .setValue(true);
   private final BooleanSetting debug = new BooleanSetting("Debug", "Print the damage value found on each head to chat")
      .setValue(false);

   private final StopWatch cooldown = new StopWatch();

   private volatile EntityStatusS2CPacket pendingPop;
   private volatile long popTime;
   private volatile int lastAttackedId = -1;
   private volatile long lastAttackTime;

   private boolean holding;
   private Slot sourceSlot;
   private ItemStack previousOffhand = ItemStack.EMPTY;
   private ItemStack placedHead = ItemStack.EMPTY;
   private int activatedAge;
   private int restoreAtAge;

   public AntiTotem() {
      super("AntiTotem", "Anti Totem", ModuleCategory.COMBAT);
      this.setup(this.range, this.hold, this.onlyMyTarget, this.restore, this.debug);
   }

   @Override
   public void deactivate() {
      this.pendingPop = null;
      this.reset();
      super.deactivate();
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (!e.isSend() && e.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == STATUS_TOTEM_USED) {
         this.pendingPop = packet;
         this.popTime = System.currentTimeMillis();
      }
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      Entity entity = e.getEntity();
      if (entity != null) {
         this.lastAttackedId = entity.getId();
         this.lastAttackTime = System.currentTimeMillis();
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player == null || mc.world == null) {
         this.pendingPop = null;
         this.reset();
         return;
      }

      this.tickRestore();

      EntityStatusS2CPacket pop = this.pendingPop;
      if (pop == null) {
         return;
      }

      this.pendingPop = null;
      if (System.currentTimeMillis() - this.popTime > POP_MAX_AGE_MS) {
         return;
      }

      if (pop.getEntity(mc.world) instanceof PlayerEntity victim
         && victim != mc.player
         && mc.player.distanceTo(victim) <= this.range.getValue()
         && (!this.onlyMyTarget.isValue() || this.isMine(victim))) {
         this.onPop();
      }
   }

   private boolean isMine(PlayerEntity victim) {
      if (victim == Aura.getInstance().getTarget() || victim == TriggerBot.getInstance().getTarget()) {
         return true;
      }

      return victim.getId() == this.lastAttackedId && System.currentTimeMillis() - this.lastAttackTime <= RECENT_ATTACK_MS;
   }

   private void onPop() {
      if (this.holding || !this.cooldown.finished(COOLDOWN_MS) || mc.currentScreen != null) {
         return;
      }

      AutoTotem autoTotem = Instance.get(AutoTotem.class);
      if ((autoTotem != null && autoTotem.isState() && autoTotem.trigger()) || !PlayerInventoryComponent.script.isFinished()) {
         return;
      }

      Slot offhand = PlayerInventoryUtil.offHandSlot();
      double baseline = mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
      double current = offhand.getStack().isOf(Items.PLAYER_HEAD) ? this.damageGain(offhand.getStack(), baseline) : 0.0;

      Slot best = null;
      double bestGain = Math.max(0.0, current);
      for (Slot slot : PlayerInventoryUtil.getSlots()) {
         if (slot.id < FIRST_STORAGE_SLOT || slot.id > LAST_STORAGE_SLOT || !(slot.inventory instanceof PlayerInventory)) {
            continue;
         }

         ItemStack stack = slot.getStack();
         if (!stack.isOf(Items.PLAYER_HEAD)) {
            continue;
         }

         double gain = this.damageGain(stack, baseline);
         if (this.debug.isValue()) {
            this.logDirect("AntiTotem: slot " + slot.id + " head damage " + String.format("%+.2f", gain)
               + (stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS) == null ? " (no attribute modifiers on this head)" : ""));
         }

         if (gain > bestGain) {
            bestGain = gain;
            best = slot;
         }
      }

      if (best == null) {
         return;
      }

      this.sourceSlot = best;
      this.placedHead = best.getStack().copy();
      this.previousOffhand = offhand.getStack().copy();
      this.activatedAge = mc.player.age;
      this.restoreAtAge = mc.player.age + Math.round(this.hold.getValue());
      this.holding = true;
      this.cooldown.reset();
      PlayerInventoryUtil.swapHand(best, Hand.OFF_HAND, true, true);
   }

   private void tickRestore() {
      if (!this.holding) {
         return;
      }

      ItemStack offhand = mc.player.getOffHandStack();
      if (!sameStack(offhand, this.placedHead)) {
         if (mc.player.age - this.activatedAge > SWAP_TIMEOUT_TICKS) {
            this.reset();
         }

         return;
      }

      if (mc.player.age < this.restoreAtAge) {
         return;
      }

      if (!this.restore.isValue()) {
         this.reset();
         return;
      }

      if (!PlayerInventoryComponent.script.isFinished() || mc.currentScreen != null) {
         return;
      }

      // The slot the head came from now holds the old offhand item; swap with the offhand again to undo.
      if (sameStack(this.sourceSlot.getStack(), this.previousOffhand)) {
         PlayerInventoryUtil.swapHand(this.sourceSlot, Hand.OFF_HAND, true, true);
      }

      this.reset();
   }

   private void reset() {
      this.holding = false;
      this.sourceSlot = null;
      this.previousOffhand = ItemStack.EMPTY;
      this.placedHead = ItemStack.EMPTY;
   }

   private static boolean sameStack(ItemStack a, ItemStack b) {
      if (a.isEmpty() || b.isEmpty()) {
         return a.isEmpty() && b.isEmpty();
      }

      return a.getItem() == b.getItem() && a.getCount() == b.getCount() && a.getName().getString().equals(b.getName().getString());
   }

   /**
    * Gain of the ATTACK_DAMAGE attribute if the stack is held in the offhand, using the vanilla formula
    * (base + sum of ADD_VALUE) * (1 + sum of ADD_MULTIPLIED_BASE) * product of (1 + ADD_MULTIPLIED_TOTAL),
    * with the player's current attack damage as the base.
    */
   private double damageGain(ItemStack stack, double baseline) {
      AttributeModifiersComponent component = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (component == null) {
         return 0.0;
      }

      double add = 0.0;
      double multipliedBase = 0.0;
      double multipliedTotal = 1.0;
      for (AttributeModifiersComponent.Entry entry : component.modifiers()) {
         if (!entry.attribute().equals(EntityAttributes.ATTACK_DAMAGE) || !entry.slot().matches(EquipmentSlot.OFFHAND)) {
            continue;
         }

         double value = entry.modifier().value();
         Operation operation = entry.modifier().operation();
         if (operation == Operation.ADD_VALUE) {
            add += value;
         } else if (operation == Operation.ADD_MULTIPLIED_BASE) {
            multipliedBase += value;
         } else if (operation == Operation.ADD_MULTIPLIED_TOTAL) {
            multipliedTotal *= 1.0 + value;
         }
      }

      return (baseline + add) * (1.0 + multipliedBase) * multipliedTotal - baseline;
   }
}
