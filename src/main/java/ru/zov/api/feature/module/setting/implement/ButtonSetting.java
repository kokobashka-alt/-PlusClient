package ru.zov.api.feature.module.setting.implement;

import java.util.function.Supplier;
import ru.zov.api.feature.module.setting.Setting;

public class ButtonSetting extends Setting {
   private Runnable runnable;
   private String buttonName;

   public ButtonSetting(String name, String description) {
      super(name, description);
   }

   public ButtonSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public Runnable getRunnable() {
      return this.runnable;
   }

   public String getButtonName() {
      return this.buttonName;
   }

   public ButtonSetting setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public ButtonSetting setButtonName(String buttonName) {
      this.buttonName = buttonName;
      return this;
   }
}
