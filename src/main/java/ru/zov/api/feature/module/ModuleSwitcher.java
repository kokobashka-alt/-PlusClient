package ru.zov.api.feature.module;

import java.util.List;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.api.feature.module.exception.ModuleException;
import ru.zov.api.system.logger.implement.ConsoleLogger;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.implement.events.keyboard.KeyEvent;

public class ModuleSwitcher implements QuickLogger, QuickImports {
   private final List<Module> modules;

   public ModuleSwitcher(List<Module> modules, EventManager eventManager) {
      this.modules = modules;
      eventManager.register(this);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      for (Module module : this.modules) {
         if (event.key() == module.getKey() && mc.currentScreen == null) {
            try {
               this.handleModuleState(module, event.action());
            } catch (Exception e) {
               this.handleException(module.getName(), e);
            }
         }
      }
   }

   private void handleModuleState(Module module, int action) {
      if (module.getType() == 1 && action == 1) {
         module.switchState();
      }
   }

   @Native
   private void handleException(String moduleName, Exception e) {
      ConsoleLogger consoleLogger = new ConsoleLogger();
      if (e instanceof ModuleException) {
         this.logDirect("[" + moduleName + "] " + Formatting.RED + e.getMessage());
      } else {
         consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
      }
   }
}
