package ru.zov.api.feature.module.setting.implement;

import java.util.function.Supplier;
import ru.zov.api.feature.module.setting.Setting;

public class ValueSetting extends Setting {
   private float value;
   private float min;
   private float max;
   private boolean integer;

   public ValueSetting(String name, String description) {
      super(name, description);
   }

   public ValueSetting range(float min, float max) {
      this.min = min;
      this.max = max;
      return this;
   }

   public ValueSetting range(int min, int max) {
      this.min = min;
      this.max = max;
      this.integer = true;
      return this;
   }

   public int getInt() {
      return (int)this.value;
   }

   public ValueSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public float getValue() {
      return this.value;
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public boolean isInteger() {
      return this.integer;
   }

   public ValueSetting setValue(float value) {
      this.value = value;
      return this;
   }

   public ValueSetting setMin(float min) {
      this.min = min;
      return this;
   }

   public ValueSetting setMax(float max) {
      this.max = max;
      return this;
   }

   public ValueSetting setInteger(boolean integer) {
      this.integer = integer;
      return this;
   }
}
