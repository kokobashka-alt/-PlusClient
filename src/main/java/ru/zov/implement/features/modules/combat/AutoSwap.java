package ru.zov.implement.features.modules.combat;

import java.util.Comparator;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.implement.events.keyboard.KeyEvent;

public class AutoSwap extends Module {
   private final BindSetting bind = new BindSetting("Item use key", "Uses item when pressed");
   private final SelectSetting firstItem = new SelectSetting("First item", "Select first swap item.")
      .value("Totem of Undying", "Player Head", "Golden Apple", "Shield");
   private final SelectSetting secondItem = new SelectSetting("Second item", "Select second swap item.")
      .value("Totem of Undying", "Player Head", "Golden Apple", "Shield");

   public AutoSwap() {
      super("AutoSwap", "Auto Swap", ModuleCategory.COMBAT);
      this.setup(this.firstItem, this.secondItem, this.bind);
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (mc.player != null && e.isKeyDown(this.bind)) {
         Slot first = PlayerInventoryUtil.getSlot(
            this.getItemByType(this.firstItem.getSelected()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45
         );
         Slot second = PlayerInventoryUtil.getSlot(
            this.getItemByType(this.secondItem.getSelected()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45
         );
         Slot validSlot = first != null && !mc.player.getOffHandStack().isOf(first.getStack().getItem()) ? first : second;
         PlayerInventoryUtil.swapHand(validSlot, Hand.OFF_HAND, true, true);
      }
   }

   @Native
   private Item getItemByType(String itemType) {
      return switch (itemType) {
         case "Totem of Undying" -> Items.TOTEM_OF_UNDYING;
         case "Player Head" -> Items.PLAYER_HEAD;
         case "Golden Apple" -> Items.GOLDEN_APPLE;
         case "Shield" -> Items.SHIELD;
         default -> Items.AIR;
      };
   }

   public BindSetting getBind() {
      return this.bind;
   }

   public SelectSetting getFirstItem() {
      return this.firstItem;
   }

   public SelectSetting getSecondItem() {
      return this.secondItem;
   }
}
