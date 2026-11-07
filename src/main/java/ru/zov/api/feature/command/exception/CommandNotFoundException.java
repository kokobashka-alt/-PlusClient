package ru.zov.api.feature.command.exception;

import java.util.List;
import ru.zov.api.feature.command.ICommand;
import ru.zov.api.feature.command.argument.ICommandArgument;
import ru.zov.common.QuickLogger;

public class CommandNotFoundException extends CommandException implements QuickLogger {
   public final String command;

   public CommandNotFoundException(String command) {
      super(String.format("Команда не найдена: %s", command));
      this.command = command;
   }

   @Override
   public void handle(ICommand command, List<ICommandArgument> args) {
      this.logDirect(this.getMessage());
   }
}
