package ru.zov.implement.features.modules.misc;

import net.minecraft.text.Text;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.draggables.StaffList;

public class AutoLeave extends Module {
   private final SelectSetting leaveType = new SelectSetting("Leave Type", "Allows you to select the leave type").value("Hub", "Main Menu");
   private final MultiSelectSetting triggerSetting = new MultiSelectSetting("Triggers", "Select in which case you will exit").value("Players", "Staff");
   private final ValueSetting distanceSetting = new ValueSetting("Max Distance", "Maximum distance for triggering auto leave")
      .setValue(10.0F)
      .range(5, 40)
      .visible(() -> this.triggerSetting.isSelected("Players"));

   public AutoLeave() {
      super("AutoLeave", "Auto Leave", ModuleCategory.MISC);
      this.setup(this.leaveType, this.triggerSetting, this.distanceSetting);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (!ServerUtil.isPvp()) {
         if (this.triggerSetting.isSelected("Players")) {
            mc.world
               .getPlayers()
               .stream()
               .filter(p -> mc.player.distanceTo(p) < this.distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p))
               .findFirst()
               .ifPresent(p -> this.leave(p.getName().copy().append(" - Появился рядом " + mc.player.distanceTo(p) + "м")));
         }

         if (this.triggerSetting.isSelected("Staff") && !StaffList.getInstance().list.isEmpty()) {
            this.leave(Text.of("Стафф на сервере"));
         }
      }
   }

   @Native
   public void leave(Text text) {
      switch (this.leaveType.getSelected()) {
         case "Hub":
            Notifications.getInstance().addList(Text.of("[AutoLeave] ").copy().append(text), 10000L);
            mc.getNetworkHandler().sendChatCommand("hub");
            break;
         case "Main Menu":
            mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
      }

      this.setState(false);
   }
}
