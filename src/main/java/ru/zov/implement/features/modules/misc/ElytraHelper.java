package ru.zov.implement.features.modules.misc;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.common.util.rotation.AngleUtil;

public class ElytraHelper extends Module {
   private final BindSetting elytraSetting = new BindSetting("Elytra Swap", "Changes place chest-plate with elytra");
   private final BindSetting fireworkSetting = new BindSetting("Use FireWork", "Swaps and uses fireworks");
   private final BooleanSetting startSetting = new BooleanSetting("Quick Start", "When swapping to elytra, it takes off and uses fireworks").setValue(false);
   private final Script script = new Script();

   public ElytraHelper() {
      super("ElytraHelper", "Elytra Helper", ModuleCategory.MISC);
      this.setup(this.elytraSetting, this.fireworkSetting, this.startSetting);
   }

   @EventHandler
   @Native
   public void onInput(InputEvent e) {
      if (!this.script.isFinished()) {
         e.setJump(true);
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (this.script.isFinished()) {
         if (e.isKeyDown(this.elytraSetting)) {
            boolean chestPlate = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
            Slot slot = PlayerInventoryUtil.getChestPlate(chestPlate);
            if (slot != null) {
               if (ServerUtil.isHolyWorld()) {
                  this.holySwap(slot, chestPlate);
               } else {
                  PlayerInventoryComponent.addTask(() -> {
                     PlayerInventoryUtil.moveItem(slot, 6, false, true);
                     this.quickStart(chestPlate);
                  });
               }
            }
         } else if (e.isKeyDown(this.fireworkSetting) && mc.player.isGliding()) {
            PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
         }
      }
   }

   @Native
   public void holySwap(Slot elytra, boolean chestPlate) {
      int from = elytra.id;
      int count = Math.toIntExact(PlayerInventoryUtil.slots().count()) - 10;
      if (from >= count && count == 36) {
         PlayerInventoryComponent.script.addTickStep(1, () -> {
            PlayerInventoryUtil.clickSlot(6, from - count, SlotActionType.SWAP, false);
            PlayerInventoryUtil.closeScreen(true);
         });
      } else {
         PlayerInventoryComponent.script.addTickStep(1, () -> {
            PlayerInventoryUtil.clickSlot(elytra, 8, SlotActionType.SWAP, false);
            PlayerInventoryUtil.closeScreen(true);
         }).addTickStep(2, () -> {
            PlayerInventoryUtil.clickSlot(6, 8, SlotActionType.SWAP, false);
            PlayerInventoryUtil.closeScreen(true);
         }).addTickStep(3, () -> {
            PlayerInventoryUtil.clickSlot(elytra, 8, SlotActionType.SWAP, false);
            PlayerInventoryUtil.updateSlots();
            PlayerInventoryUtil.closeScreen(true);
            this.quickStart(chestPlate);
         });
      }
   }

   @Native
   public void quickStart(boolean chestPlate) {
      Slot fireWork = PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET);
      if (this.startSetting.isValue() && fireWork != null && !chestPlate) {
         this.script
            .cleanup()
            .addTickStep(3, PlayerIntersectionUtil::startFallFlying)
            .addTickStep(6, () -> PlayerInventoryUtil.swapAndUse(fireWork, AngleUtil.cameraAngle()));
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.script.update();
   }
}
