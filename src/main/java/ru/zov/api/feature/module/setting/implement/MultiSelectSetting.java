package ru.zov.api.feature.module.setting.implement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import ru.zov.api.feature.module.setting.Setting;

public class MultiSelectSetting extends Setting {
   private List<String> list;
   private List<String> selected = new ArrayList<>();

   public MultiSelectSetting(String name, String description) {
      super(name, description);
   }

   public MultiSelectSetting value(String... settings) {
      this.list = Arrays.asList(settings);
      return this;
   }

   public MultiSelectSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public MultiSelectSetting selected(String... list) {
      this.selected = Arrays.stream(list).toList();
      return this;
   }

   public boolean isSelected(String name) {
      return this.selected.contains(name);
   }

   public List<String> getList() {
      return this.list;
   }

   public List<String> getSelected() {
      return this.selected;
   }

   public void setList(List<String> list) {
      this.list = list;
   }

   public void setSelected(List<String> selected) {
      this.selected = selected;
   }
}
