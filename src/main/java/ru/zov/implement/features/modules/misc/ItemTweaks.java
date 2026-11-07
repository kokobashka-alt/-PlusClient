package ru.zov.implement.features.modules.misc;

import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.implement.events.container.ClickSlotEvent;
import ru.zov.implement.events.container.HandledScreenEvent;

public class ItemTweaks extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final ValueSetting scrollerSetting = new ValueSetting("Delay", "Select Item Scroller delay").setValue(100.0F).range(0, 200);

   public ItemTweaks() {
      super("ItemTweaks", "Item Tweaks", ModuleCategory.MISC);
      this.setup(this.scrollerSetting);
   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      Slot hoverSlot = e.screen().focusedSlot;
      SlotActionType actionType = PlayerIntersectionUtil.isKey(mc.options.dropKey)
         ? SlotActionType.THROW
         : (PlayerIntersectionUtil.isKey(mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null);
      if (PlayerIntersectionUtil.isKey(mc.options.sneakKey)
         && !PlayerIntersectionUtil.isKey(mc.options.sprintKey)
         && hoverSlot != null
         && hoverSlot.hasStack()
         && actionType != null
         && this.stopWatch.every(this.scrollerSetting.getValue())) {
         mc.interactionManager
            .clickSlot(mc.player.currentScreenHandler.syncId, hoverSlot.id, actionType.equals(SlotActionType.THROW) ? 1 : 0, actionType, mc.player);
      }
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent e) {
      int slotId = e.getSlotId();
      if (slotId >= 0 && slotId <= mc.player.currentScreenHandler.slots.size()) {
         Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
         Item item = slot.getStack().getItem();
         if (item != null
            && PlayerIntersectionUtil.isKey(mc.options.sneakKey)
            && PlayerIntersectionUtil.isKey(mc.options.sprintKey)
            && this.stopWatch.every(50.0)) {
            PlayerInventoryUtil.slots()
               .filter(s -> s.getStack().isOf(item) && s.inventory.equals(slot.inventory))
               .forEach(s -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, s.id, 1, e.getActionType(), mc.player));
         }
      }
   }
}
