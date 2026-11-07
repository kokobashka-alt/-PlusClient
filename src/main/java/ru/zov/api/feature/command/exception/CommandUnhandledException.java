package ru.zov.api.feature.command.exception;

import java.util.List;
import ru.zov.api.feature.command.ICommand;
import ru.zov.api.feature.command.argument.ICommandArgument;
import ru.zov.common.QuickLogger;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {
   public CommandUnhandledException(String message) {
      super(message);
   }

   public CommandUnhandledException(Throwable cause) {
      super(cause);
   }

   @Override
   public void handle(ICommand command, List<ICommandArgument> args) {
   }
}
