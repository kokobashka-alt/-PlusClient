package ru.zov.api.feature.command.exception;

import java.util.List;
import net.minecraft.util.Formatting;
import ru.zov.api.feature.command.ICommand;
import ru.zov.api.feature.command.argument.ICommandArgument;
import ru.zov.common.QuickLogger;

public interface ICommandException extends QuickLogger {
   String getMessage();

   default void handle(ICommand command, List<ICommandArgument> args) {
      this.logDirect(this.getMessage(), Formatting.RED);
   }
}
