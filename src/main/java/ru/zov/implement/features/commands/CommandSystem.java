package ru.zov.implement.features.commands;

import ru.zov.api.feature.command.ICommandSystem;
import ru.zov.api.feature.command.argparser.IArgParserManager;
import ru.zov.implement.features.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
   INSTANCE;

   @Override
   public IArgParserManager getParserManager() {
      return ArgParserManager.INSTANCE;
   }
}
