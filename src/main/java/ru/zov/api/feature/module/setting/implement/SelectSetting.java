package ru.zov.api.feature.module.setting.implement;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import ru.zov.api.feature.module.setting.Setting;

public class SelectSetting extends Setting {
   private String selected;
   private List<String> list;

   public SelectSetting(String name, String description) {
      super(name, description);
   }

   public SelectSetting value(String... values) {
      List<String> list = Arrays.asList(values);
      this.selected = list.getFirst();
      this.list = list;
      return this;
   }

   public SelectSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public SelectSetting selected(String string) {
      this.selected = string;
      return this;
   }

   public boolean isSelected(String... name) {
      return Arrays.stream(name).anyMatch(str -> this.selected.equalsIgnoreCase(str));
   }

   public String getSelected() {
      return this.selected;
   }

   public List<String> getList() {
      return this.list;
   }

   public void setSelected(String selected) {
      this.selected = selected;
   }
}
