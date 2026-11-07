package ru.zov.api.feature.module.setting.implement;

import java.util.function.Supplier;
import ru.zov.api.feature.module.setting.Setting;

public class BindSetting extends Setting {
   private int key = -1;
   private int type = 1;

   public BindSetting(String name, String description) {
      super(name, description);
   }

   public BindSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public int getKey() {
      return this.key;
   }

   public int getType() {
      return this.type;
   }

   public BindSetting setKey(int key) {
      this.key = key;
      return this;
   }

   public BindSetting setType(int type) {
      this.type = type;
      return this;
   }
}
