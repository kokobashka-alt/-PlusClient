package ru.zov.implement.features.commands.defaults;

import java.util.List;
import java.util.stream.Stream;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandException;

public class DebugCommand extends Command {
   public static boolean debug = false;

   public DebugCommand() {
      super("debug", "dbg");
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      debug = !debug;
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return null;
   }

   @Native
   @Override
   public String getShortDesc() {
      return "developer";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return List.of("Developer");
   }
}
