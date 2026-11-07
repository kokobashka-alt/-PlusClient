package ru.zov.api.feature.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import ru.zov.api.feature.module.setting.SettingRepository;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.sound.SoundManager;
import ru.zov.common.QuickImports;
import ru.zov.core.Main;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.modules.render.Hud;

public class Module extends SettingRepository implements QuickImports {
   public final Animation animation = new DecelerateAnimation().setMs(150).setValue(1.0).setDirection(Direction.BACKWARDS);
   private final String name;
   private final String visibleName;
   private final ModuleCategory category;
   private int key = -1;
   private int type = 1;
   public boolean state;

   public Module(String name, ModuleCategory category) {
      this.name = name;
      this.category = category;
      this.visibleName = name;
   }

   public Module(String name, String visibleName, ModuleCategory category) {
      this.name = name;
      this.visibleName = visibleName;
      this.category = category;
   }

   public Module silentEnable() {
      this.state = true;
      return this;
   }

   public void switchState() {
      this.setState(!this.state);
   }

   public void setState(boolean state) {
      this.animation.setDirection(state);
      if (state != this.state) {
         this.handleStateChange(state);
      }
   }

   private void handleStateChange(boolean state) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         if (state) {
            if (Hud.getInstance().notificationSettings.isSelected("Module Switch")) {
               Notifications.getInstance()
                  .addList("Модуль " + Formatting.GREEN + this.visibleName + Formatting.RESET + " - включен!", 2000L, SoundManager.ENABLE_MODULE);
            }

            this.activate();
         } else {
            if (Hud.getInstance().notificationSettings.isSelected("Module Switch")) {
               Notifications.getInstance()
                  .addList("Модуль " + Formatting.RED + this.visibleName + Formatting.RESET + " - выключен!", 2000L, SoundManager.DISABLE_MODULE);
            }

            this.deactivate();
         }
      }

      this.toggleSilent(state);
   }

   public void toggleSilent(boolean activate) {
      if (activate) {
         this.state = true;
         Main.getInstance().getEventManager().register(this);
      } else {
         this.state = false;
         Main.getInstance().getEventManager().unregister(this);
      }
   }

   public void activate() {
   }

   public void deactivate() {
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public String getName() {
      return this.name;
   }

   public String getVisibleName() {
      return this.visibleName;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public int getKey() {
      return this.key;
   }

   public int getType() {
      return this.type;
   }

   public boolean isState() {
      return this.state;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public void setType(int type) {
      this.type = type;
   }
}
