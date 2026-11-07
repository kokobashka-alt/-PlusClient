package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.IBaritoneChatControl;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.datatypes.KeyDataType;
import ru.zov.api.feature.command.datatypes.MacroDataType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.MacroRepository;
import ru.zov.common.util.other.StringUtil;
import ru.zov.core.Main;

public class MacroCommand extends Command {
   private final MacroRepository macroRepository;

   public MacroCommand(Main main) {
      super("macro", "macros");
      this.macroRepository = main.getMacroRepository();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (action) {
         case "add":
            this.handleAddMacro(args);
            break;
         case "remove":
            this.handleRemoveMacro(args);
            break;
         case "list":
            this.handleListMacros(args, label);
            break;
         case "clear":
            this.handleClearMacros(args);
      }
   }

   @Native
   private void handleAddMacro(IArgConsumer args) throws CommandException {
      args.requireMin(3);
      int key = args.<Entry<String, Integer>, KeyDataType>getDatatypeFor(KeyDataType.INSTANCE).getValue();
      String name = args.getString();
      String command = args.rawRest();
      if (this.macroRepository.hasMacro(name)) {
         this.logDirect("Макрос с таким именем уже есть в списке!", Formatting.RED);
      } else {
         this.macroRepository.addMacro(name, command, key);
         this.logDirect(
            Formatting.GREEN
               + "Добавлен макрос с названием "
               + Formatting.RED
               + name
               + Formatting.GREEN
               + " с кнопкой "
               + Formatting.RED
               + StringUtil.getBindName(key).toLowerCase()
               + Formatting.GREEN
               + " с командой "
               + Formatting.RED
               + command
         );
      }
   }

   @Native
   private void handleRemoveMacro(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      String name = args.getString();
      if (this.macroRepository.hasMacro(name)) {
         this.macroRepository.deleteMacro(name);
         this.logDirect(Formatting.GREEN + "Макрос " + Formatting.RED + name + Formatting.GREEN + " был успешно удален!");
      } else {
         this.logDirect("Макрос с таким именем не найден!", Formatting.RED);
      }
   }

   @Native
   private void handleListMacros(IArgConsumer args, String label) throws CommandException {
      args.requireMax(1);
      Paginator.paginate(
         args,
         new Paginator<>(this.macroRepository.macroList),
         () -> this.logDirect("Список макросов:"),
         macro -> {
            String names = macro.name();
            String keys = StringUtil.getBindName(macro.key()).toLowerCase();
            String command = macro.message();
            return Text.literal(Formatting.GRAY + "Название: " + Formatting.WHITE + names)
               .append(Text.literal(Formatting.GRAY + " Клавиша: " + Formatting.WHITE + keys))
               .append(Text.literal(Formatting.GRAY + " Команда: " + Formatting.WHITE + command));
         },
         IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
      );
   }

   @Native
   private void handleClearMacros(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      this.macroRepository.clearList();
      this.logDirect("Все макросы были удалены.", Formatting.GREEN);
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny() && args.hasExactlyOne()) {
         return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
      }

      if (args.hasAny()) {
         String arg = args.getString();
         if (arg.equalsIgnoreCase("add") && args.hasExactlyOne()) {
            return args.tabCompleteDatatype(KeyDataType.INSTANCE);
         }

         if (arg.equalsIgnoreCase("remove") && args.hasExactlyOne()) {
            return args.tabCompleteDatatype(MacroDataType.INSTANCE);
         }
      }

      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет управлять макросами";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Эта команда позволяет управлять макросами, которые автоматически вводят заданные команды в чат.",
         "",
         "Использование:",
         "> macro add <key> <name> <message> - Добавляет новый макрос, который будет активироваться при нажатии на указанную клавишу и вводить указанное сообщение.",
         "> macro remove <name> - Удаляет макрос с указанным именем.",
         "> macro list - Отображает список всех текущих макросов.",
         "> macro clear - Удаляет все макросы из списка."
      );
   }
}
