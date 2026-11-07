package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.common.QuickImports;
import ru.zov.implement.features.commands.CommandDispatcher;

public class PrefixCommand extends Command implements QuickImports {
   protected PrefixCommand() {
      super("prefix");
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      if (arg.equals("set")) {
         args.requireMin(1);
         this.logDirect("Установлен префикс '" + Formatting.RED + (CommandDispatcher.prefix = args.getString()) + Formatting.GRAY + "'", Formatting.GRAY);
      }
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         return arg.equalsIgnoreCase("set")
            ? new TabCompleteHelper().sortAlphabetically().prepend("name").stream()
            : new TabCompleteHelper().sortAlphabetically().prepend("set").filterPrefix(arg).stream();
      } else {
         return Stream.empty();
      }
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет менять префикс команд в моде";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно изменить префикс команд в моде", "", "Использование:", "> prefix set <name> - устанавливает префикс команд"
      );
   }
}
