package ru.zov.api.feature.module;

public enum ModuleCategory {
   COMBAT("Combat"),
   MOVEMENT("Movement"),
   PLAYER("Player"),
   RENDER("Render"),
   MISC("Misc");

   final String readableName;

   ModuleCategory(final String readableName) {
      this.readableName = readableName;
   }

   public String getReadableName() {
      return this.readableName;
   }
}
