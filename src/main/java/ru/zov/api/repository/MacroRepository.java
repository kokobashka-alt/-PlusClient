package ru.zov.api.repository;

import java.util.ArrayList;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.implement.events.keyboard.KeyEvent;

public class MacroRepository implements QuickImports, QuickLogger {
   public List<MacroRepository.Macro> macroList = new ArrayList<>();

   public MacroRepository(EventManager eventManager) {
      eventManager.register(this);
   }

   @Native
   public void addMacro(String name, String message, int key) {
      this.macroList.add(new MacroRepository.Macro(name, message, key));
   }

   public boolean hasMacro(String text) {
      return this.macroList.stream().anyMatch(macro -> macro.name().equalsIgnoreCase(text));
   }

   @Native
   public void deleteMacro(String text) {
      this.macroList.removeIf(macro -> macro.name().equalsIgnoreCase(text));
   }

   @Native
   public void clearList() {
      this.macroList.clear();
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (mc.player != null && e.action() == 0 && mc.currentScreen == null) {
         this.macroList
            .stream()
            .filter(macro -> macro.key() == e.key())
            .findFirst()
            .ifPresent(macro -> mc.player.networkHandler.sendChatMessage(macro.message()));
      }
   }

   public record Macro(String name, String message, int key) {
   }
}
