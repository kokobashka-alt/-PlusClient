package ru.zov.implement.events.keyboard;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;
import ru.zov.api.event.events.Event;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.common.QuickImports;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event, QuickImports {
   public boolean isKeyDown(BindSetting setting) {
      return this.isKeyDown(setting.getKey()) && setting.isVisible() && mc.currentScreen == null;
   }

   public boolean isKeyDown(int key) {
      return this.key == key && this.action == 1;
   }

   public boolean isKeyReleased(BindSetting setting) {
      return this.isKeyReleased(setting.getKey()) && setting.isVisible() && mc.currentScreen == null;
   }

   public boolean isKeyReleased(int key) {
      return this.key == key && this.action == 0;
   }
}
