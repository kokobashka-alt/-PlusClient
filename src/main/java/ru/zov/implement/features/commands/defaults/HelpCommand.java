package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.IBaritoneChatControl;
import ru.zov.api.feature.command.ICommand;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.exception.CommandNotFoundException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.core.Main;
import ru.zov.implement.features.commands.manager.CommandRepository;

public class HelpCommand extends Command {
   Main main;

   protected HelpCommand(Main main) {
      super("help");
      this.main = main;
   }

   @Native
   public void execute(String label, IArgConsumer args) throws CommandException {
      args.requireMax(1);
      CommandRepository commandRepository = this.main.getCommandRepository();
      if (args.hasAny() && !args.is(Integer.class)) {
         String commandName = args.getString().toLowerCase();
         ICommand command = commandRepository.getCommand(commandName);
         if (command == null) {
            throw new CommandNotFoundException(commandName);
         }

         this.logDirect("");
         command.getLongDesc().forEach(this::logDirect);
         this.logDirect("");
         MutableText returnComponent = Text.literal("Нажмите что бы вернуться обратно в меню");
          returnComponent.setStyle(returnComponent.getStyle().withClickEvent(new ClickEvent.RunCommand(IBaritoneChatControl.FORCE_COMMAND_PREFIX + label)));
         this.logDirect(new Text[]{returnComponent});
      } else {
         Paginator.paginate(args, new Paginator((List)commandRepository.getRegistry().descendingStream().filter((commandx) -> !commandx.hiddenFromHelp()).collect(Collectors.toList())), () -> this.logDirect("Доступные команды в чите:"), (commandx) -> {
            ICommand commandValue = (ICommand)commandx;
            String names = String.join("/", commandValue.getNames());
            String name = (String)commandValue.getNames().getFirst();
            MutableText shortDescComponent = Text.literal(" - " + commandValue.getShortDesc());
            shortDescComponent.setStyle(shortDescComponent.getStyle().withColor(Formatting.DARK_GRAY));
            MutableText namesComponent = Text.literal(names);
            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
            MutableText hoverComponent = Text.literal("");
            hoverComponent.setStyle(hoverComponent.getStyle().withColor(Formatting.GRAY));
            hoverComponent.append(namesComponent);
            hoverComponent.append("\n" + commandValue.getShortDesc());
            hoverComponent.append("\n\nНажмите, чтобы просмотреть полную справку о команде");
            String var10000 = IBaritoneChatControl.FORCE_COMMAND_PREFIX;
            String clickCommand = var10000 + String.format("%s %s", label, commandValue.getNames().getFirst());
            MutableText component = Text.literal(name);
            component.setStyle(component.getStyle().withColor(Formatting.GRAY));
            component.append(shortDescComponent);
             component.setStyle(component.getStyle().withHoverEvent(new HoverEvent.ShowText(hoverComponent)).withClickEvent(new ClickEvent.RunCommand(clickCommand)));
            return component;
         }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
      }

   }

   @Native
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return args.hasExactlyOne() ? (new TabCompleteHelper()).addCommands(this.main.getCommandRepository()).filterPrefix(args.getString()).stream() : Stream.empty();
   }

   @Native
   public String getShortDesc() {
      return "Просмотр всех доступных команд";
   }

   @Native
   public List<String> getLongDesc() {
      return Arrays.asList("С помощью этой команды можно просмотреть подробную справочную информацию о том, как использовать определенные команды", "", "Использование:", "> help - Перечисляет все команды и их краткие описания.", "> help <command> - Отображение справочной информации по конкретной команде.");
   }
}
