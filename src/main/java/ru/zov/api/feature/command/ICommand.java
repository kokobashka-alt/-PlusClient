package ru.zov.api.feature.command;

import java.util.List;
import java.util.stream.Stream;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.common.QuickLogger;

public interface ICommand extends QuickLogger {
   void execute(String var1, IArgConsumer var2) throws CommandException;

   Stream<String> tabComplete(String var1, IArgConsumer var2) throws CommandException;

   String getShortDesc();

   List<String> getLongDesc();

   List<String> getNames();

   default boolean hiddenFromHelp() {
      return false;
   }
}
