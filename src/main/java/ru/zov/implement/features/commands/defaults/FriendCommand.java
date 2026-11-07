package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.IBaritoneChatControl;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.datatypes.FriendDataType;
import ru.zov.api.feature.command.datatypes.TabPlayerDataType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.FriendUtils;

public class FriendCommand extends Command {
   protected FriendCommand() {
      super("friend");
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      args.requireMax(1);
      if (arg.contains("add")) {
         String name = args.getString();
         if (!FriendUtils.isFriend(name)) {
            FriendUtils.addFriend(name);
            this.logDirect("Вы успешно добавили " + name + " в список друзей!");
         } else {
            this.logDirect(name + " уже есть в списке друзей!", Formatting.RED);
         }
      }

      if (arg.contains("remove")) {
         String name = args.getString();
         if (FriendUtils.isFriend(name)) {
            FriendUtils.removeFriend(name);
            this.logDirect("Вы успешно удалили " + name + " из списка друзей!");
            return;
         }

         this.logDirect(name + " не найден в списке друзей", Formatting.RED);
      }

      if (arg.contains("list")) {
         Paginator.paginate(args, new Paginator<>(FriendUtils.getFriends()), () -> this.logDirect("Список друзей:"), friend -> {
            MutableText namesComponent = Text.literal(friend);
            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
            return namesComponent;
         }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
      }

      if (arg.contains("clear")) {
         FriendUtils.clear();
         this.logDirect("Список друзей очищен.");
      }
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(arg).stream();
         }

         if (arg.equalsIgnoreCase("add")) {
            return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
         }

         if (arg.equalsIgnoreCase("remove")) {
            return args.tabCompleteDatatype(FriendDataType.INSTANCE);
         }
      }

      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет управлять списком друзей";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно добавлять/удалять друзей в чите",
         "",
         "Использование:",
         "> friend add <name> - Добавляет имя в список друзей.",
         "> friend remove <name> - Удаляет имя из списка друзей.",
         "> friend list - Возвращает список друзей",
         "> friend clear - Очищает список друзей."
      );
   }
}
