package ru.zov.implement.screens.menu.components.implement.window.implement.module;

import ru.zov.api.feature.module.Module;
import ru.zov.implement.screens.menu.components.implement.window.implement.AbstractBindWindow;

public class ModuleBindWindow extends AbstractBindWindow {
   private final Module module;

   @Override
   protected int getKey() {
      return this.module.getKey();
   }

   @Override
   protected void setKey(int key) {
      this.module.setKey(key);
   }

   @Override
   protected int getType() {
      return this.module.getType();
   }

   @Override
   protected void setType(int type) {
      this.module.setType(type);
   }

   public ModuleBindWindow(Module module) {
      this.module = module;
   }
}
