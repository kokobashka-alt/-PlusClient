package ru.zov.implement.features.modules.player;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.ItemUsage;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.player.TickEvent;

public class AutoUse extends Module {
   private final StopWatch stopWatch = new StopWatch();
   public final Script script = new Script();
   private final MultiSelectSetting multiSetting = new MultiSelectSetting("Mode", "Choose what will be used").value("Eat", "Invisibility");

   public static AutoUse getInstance() {
      return Instance.get(AutoUse.class);
   }

   public AutoUse() {
      super("AutoUse", "Auto Use", ModuleCategory.PLAYER);
      this.setup(this.multiSetting);
   }

   @Override
   public void deactivate() {
      this.script.update();
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (PlayerInventoryUtil.isServerScreen()) {
         this.stopWatch.reset();
      }

      if (this.stopWatch.finished(200.0)) {
         for (String string : this.multiSetting.getSelected()) {
            switch (string) {
               case "Eat":
                  Slot slotx = PlayerInventoryUtil.getFoodMaxSaturationSlot();
                  if (slotx != null && mc.player.getHungerManager().isNotFull() && this.swapAndEat(slotx)) {
                     return;
                  }
                  break;
               case "Invisibility":
                  Slot slot = PlayerInventoryUtil.getPotion(StatusEffects.INVISIBILITY);
                  if (slot != null && !PlayerIntersectionUtil.isPotionActive(StatusEffects.INVISIBILITY) && this.swapAndEat(slot)) {
                     return;
                  }
            }
         }

         this.script.update();
      }
   }

   @Native
   public boolean swapAndEat(Slot slot) {
      ItemStack stack = slot.getStack();
      if (!ItemUtil.hasCooldown(stack)) {
         if (PlayerInventoryUtil.offHandSlot().getStack().equals(stack)) {
            ItemUsage.INSTANCE.useHand(Hand.OFF_HAND);
            return true;
         }

         if (PlayerInventoryComponent.script.isFinished()) {
            PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true, true);
            this.script.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true, true));
            this.stopWatch.reset();
            return true;
         }
      }

      return false;
   }
}
