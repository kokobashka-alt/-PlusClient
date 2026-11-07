package ru.zov.common.util.item;

import net.minecraft.util.Hand;
import ru.zov.common.QuickImports;

public class ItemUsage implements QuickImports {
   public static final ItemUsage INSTANCE = new ItemUsage();
   public boolean useItem;

   public void useHand(Hand hand) {
      if (mc.player != null && mc.interactionManager != null && (!mc.player.isUsingItem() || !mc.player.getActiveHand().equals(hand))) {
         mc.interactionManager.interactItem(mc.player, hand);
         mc.player.usingItem = true;
      }

      this.useItem = true;
   }

   public void setUseItem(boolean useItem) {
      this.useItem = useItem;
   }

   public boolean isUseItem() {
      return this.useItem;
   }
}
