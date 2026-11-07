package ru.zov.implement.features.modules.misc;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.implement.events.player.TickEvent;

public class ContainerStealer extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final SelectSetting modeSetting = new SelectSetting("Stealer Type", "Selects the type of stealer").value("FunTime", "WhiteList", "Default");
   private final ValueSetting delaySetting = new ValueSetting("Delay", "Delay between click on slot")
      .setValue(100.0F)
      .range(0, 1000)
      .visible(() -> this.modeSetting.isSelected("WhiteList") || this.modeSetting.isSelected("Default"));
   private final MultiSelectSetting itemSettings = new MultiSelectSetting("Items", "Select the items that the stealer will pick up")
      .value(
         "Player Head",
         "Totem Of Undying",
         "Elytra",
         "Netherite Sword",
         "Netherite Helmet",
         "Netherite ChestPlate",
         "Netherite Leggings",
         "Netherite Boots",
         "Netherite Ingot",
         "Netherite Scrap"
      )
      .visible(() -> this.modeSetting.isSelected("WhiteList"));

   public ContainerStealer() {
      super("ContainerStealer", "Container Stealer", ModuleCategory.MISC);
      this.setup(this.modeSetting, this.delaySetting, this.itemSettings);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      switch (this.modeSetting.getSelected()) {
         case "FunTime":
            if (mc.currentScreen instanceof GenericContainerScreen sh
               && sh.getTitle().getString().toLowerCase().contains("мистический")
               && ItemUtil.getCooldownProgress(Items.GUNPOWDER) == 0.0F) {
               sh.getScreenHandler()
                  .slots
                  .stream()
                  .filter(s -> s.hasStack() && !s.inventory.equals(mc.player.inventory) && this.stopWatch.every(150.0))
                  .forEach(s -> PlayerInventoryUtil.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true));
            }
            break;
         case "WhiteList":
         case "Default":
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) {
               sh.slots
                  .forEach(
                     s -> {
                        if (s.hasStack()
                           && !s.inventory.equals(mc.player.inventory)
                           && (this.modeSetting.isSelected("Default") || this.whiteList(s.getStack().getItem()))
                           && this.stopWatch.every(this.delaySetting.getValue())) {
                           PlayerInventoryUtil.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true);
                        }
                     }
                  );
            }
      }
   }

   @Native
   private boolean whiteList(Item item) {
      return this.itemSettings.getSelected().toString().toLowerCase().contains(item.toString().toLowerCase().replace("_", ""));
   }
}
