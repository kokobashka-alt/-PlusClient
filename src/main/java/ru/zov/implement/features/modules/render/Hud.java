package ru.zov.implement.features.modules.render;

import dev.redstones.mediaplayerinfo.IMediaSession;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.features.draggables.MediaPlayer;

public class Hud extends Module {
   public final MultiSelectSetting interfaceSettings = new MultiSelectSetting("Elements", "Customize the interface elements")
      .value(
         "Watermark",
         "Hot Keys",
         "Potions",
         "Staff List",
         "Target Hud",
         "Armor",
         "CoolDowns",
         "Inventory",
         "Player Info",
         "Boss Bars",
         "Notifications",
         "Score Board",
         "Media Player",
         "HotBar",
         "Mini Map"
      );
   public final MultiSelectSetting notificationSettings = new MultiSelectSetting("Notifications", "Choose when the notification will appear")
      .value("Module Switch", "Staff Join", "Item Pick Up", "Auto Armor", "Break Shield")
      .visible(() -> this.interfaceSettings.isSelected("Notifications"));
   public final ColorSetting colorSetting = new ColorSetting("Client Color", "Select your client's color")
      .setColor(-9659651)
      .presets(-9659651, -7569409, -23178, -33925);
   private final BindSetting preSetting = new BindSetting("Previous Audio", "Turn on previous audio")
      .visible(() -> this.interfaceSettings.isSelected("Media Player"));
   private final BindSetting playSetting = new BindSetting("Stop/Play Audio", "Stop/Play current audio")
      .visible(() -> this.interfaceSettings.isSelected("Media Player"));
   private final BindSetting nextSetting = new BindSetting("Next Audio", "Turn on next audio").visible(() -> this.interfaceSettings.isSelected("Media Player"));

   public static Hud getInstance() {
      return Instance.get(Hud.class);
   }

   public Hud() {
      super("Hud", ModuleCategory.RENDER);
      this.setup(this.interfaceSettings, this.notificationSettings, this.colorSetting, this.preSetting, this.playSetting, this.nextSetting);
   }

   @EventHandler
   @Native
   public void onKey(KeyEvent e) {
      IMediaSession session = MediaPlayer.getInstance().session;
      if (this.interfaceSettings.isSelected("Media Player") && session != null) {
         if (e.isKeyDown(this.preSetting.getKey())) {
            session.previous();
         }

         if (e.isKeyDown(this.playSetting.getKey())) {
            session.playPause();
         }

         if (e.isKeyDown(this.nextSetting.getKey())) {
            session.next();
         }
      }
   }
}
