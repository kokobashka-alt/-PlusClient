package ru.zov.implement.features.commands.defaults;

import java.util.List;
import java.util.stream.Stream;
import ru.nexusguard.UserProfile;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.IRCRepository;
import ru.zov.common.QuickImports;
import ru.zov.core.Main;

public class IRCCommand extends Command implements QuickImports {
   private final IRCRepository repository;

   protected IRCCommand(Main main) {
      super("irc");
      this.repository = main.getIrcRepository();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String msg = args.rawRest();
         switch (msg.toLowerCase()) {
            case "unshow":
               this.repository.shutdown();
               break;
            case "show":
               this.repository.init();
               break;
            default:
               this.repository.send(msg);
         }
      }
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      boolean admin = UserProfile.role.equals("BETA") || UserProfile.role.equals("ADMIN");
      if (args.hasExactly(4)) {
         if (admin && args.getString().equalsIgnoreCase("/mute")) {
            return new TabCompleteHelper().sortAlphabetically().prepend("<cause>").stream();
         }
      } else if (args.hasExactly(3)) {
         if (admin && args.getString().equalsIgnoreCase("/mute")) {
            return new TabCompleteHelper().sortAlphabetically().prepend("<hours>").stream();
         }
      } else if (args.hasExactly(2)) {
         String string = args.getString();
         if (admin && (string.equalsIgnoreCase("/mute") || string.equalsIgnoreCase("/unmute"))) {
            return new TabCompleteHelper().sortAlphabetically().prepend("<player>").stream();
         }
      } else if (args.hasExactly(1)) {
         TabCompleteHelper tabComplete = new TabCompleteHelper().sortAlphabetically().prepend("<message>", "show", "unshow");
         if (admin) {
            tabComplete.prepend("/mute", "/unmute");
         }

         return tabComplete.sortAlphabetically().stream();
      }

      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет общаться между юзерами в чате маинкрафта";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return List.of(
         "С помощью этой команды можно общаться с юзерами чита в чате маинкрафта",
         "",
         "Использование:",
         "> irc <message> - отправляет сообщение",
         "> irc show - позволяет видить сообщения юзеров и отпарвлять их",
         "> irc unshow - не позволяет видить сообщения юзеров и отпарвлять их"
      );
   }
}
