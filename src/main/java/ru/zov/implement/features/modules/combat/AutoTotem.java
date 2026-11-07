package ru.zov.implement.features.modules.combat;

import java.util.Comparator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.player.TickEvent;

public class AutoTotem extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final Script script = new Script();
   private final ValueSetting healthSetting = new ValueSetting("Max Health", "Maximum health for triggering Auto Totem").setValue(4.0F).range(2.0F, 8.0F);
   private final MultiSelectSetting triggerSetting = new MultiSelectSetting("Triggers", "Select in which case the Totem will be taken").value("Crystal", "TNT");
   private final ValueSetting TNTRangeSetting = new ValueSetting("TNT Distance", "Distance to TNT for triggering Auto Totem")
      .setValue(8.0F)
      .range(0.0F, 50.0F)
      .visible(() -> this.triggerSetting.isSelected("TNT"));

   public AutoTotem() {
      super("AutoTotem", "Auto Totem", ModuleCategory.COMBAT);
      this.setup(this.healthSetting, this.triggerSetting, this.TNTRangeSetting);
   }

   @Override
   public void deactivate() {
      this.script.update();
      super.deactivate();
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (this.trigger()) {
         Slot offhand = PlayerInventoryUtil.offHandSlot();
         if (PlayerInventoryComponent.script.isFinished()) {
            PlayerInventoryUtil.slots()
               .filter(s -> s.getStack().isOf(Items.TOTEM_OF_UNDYING) && !s.equals(offhand))
               .min(Comparator.comparing(s -> s.getStack().hasEnchantments()))
               .ifPresent(slot -> {
                  if (!offhand.getStack().isOf(Items.TOTEM_OF_UNDYING) || offhand.getStack().hasEnchantments() && !slot.getStack().hasEnchantments()) {
                     PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true);
                     if (this.script.isFinished()) {
                        this.script.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true));
                     }
                  }
               });
         }

         this.stopWatch.reset();
      } else if (!this.script.isFinished() && this.stopWatch.every(300.0)) {
         this.script.update();
      }
   }

   @Native
   public boolean trigger() {
      float elytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) ? 4.0F : 0.0F;
      float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      if (ItemUtil.getCooldownProgress(Items.TOTEM_OF_UNDYING) != 0.0F) {
         return false;
      } else if (health < this.healthSetting.getValue() + elytra) {
         return true;
      } else {
         return this.triggerSetting.isSelected("Crystal")
               && PlayerIntersectionUtil.streamEntities()
                  .anyMatch(e -> e instanceof EndCrystalEntity && mc.player.distanceTo(e) < 5.0F && e.getY() > mc.player.getEyeY())
            ? true
            : this.triggerSetting.isSelected("TNT")
               && PlayerIntersectionUtil.streamEntities().anyMatch(e -> e instanceof TntEntity && mc.player.distanceTo(e) < this.TNTRangeSetting.getValue());
      }
   }

   public StopWatch getStopWatch() {
      return this.stopWatch;
   }

   public Script getScript() {
      return this.script;
   }

   public ValueSetting getHealthSetting() {
      return this.healthSetting;
   }

   public MultiSelectSetting getTriggerSetting() {
      return this.triggerSetting;
   }

   public ValueSetting getTNTRangeSetting() {
      return this.TNTRangeSetting;
   }
}
