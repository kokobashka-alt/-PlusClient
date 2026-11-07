package ru.zov.implement.features.modules.player;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.TextSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.implement.events.render.TextFactoryEvent;

public class NameProtect extends Module {
   private final TextSetting nameSetting = new TextSetting("Name", "Nickname that will be replaced with yours").setText("Protect").setMax(16);
   private final BooleanSetting friendsSetting = new BooleanSetting("Friends", "Hides friends' nicknames").setValue(true);

   public NameProtect() {
      super("NameProtect", "Name Protect", ModuleCategory.PLAYER);
      this.setup(this.nameSetting, this.friendsSetting);
   }

   @EventHandler
   public void onTextFactory(TextFactoryEvent e) {
      e.replaceText(mc.getSession().getUsername(), this.nameSetting.getText());
      if (this.friendsSetting.isValue()) {
         FriendUtils.getFriends().forEach(friend -> e.replaceText(friend, this.nameSetting.getText()));
      }
   }
}
