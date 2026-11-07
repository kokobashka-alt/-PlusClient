package ru.zov.implement.features.commands;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Pair;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.api.feature.command.IBaritoneChatControl;
import ru.zov.api.feature.command.argument.ICommandArgument;
import ru.zov.api.feature.command.exception.CommandNotEnoughArgumentsException;
import ru.zov.api.feature.command.exception.CommandNotFoundException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.feature.command.manager.ICommandManager;
import ru.zov.core.Main;
import ru.zov.implement.events.chat.ChatEvent;
import ru.zov.implement.events.chat.TabCompleteEvent;
import ru.zov.implement.features.commands.argument.ArgConsumer;
import ru.zov.implement.features.commands.argument.CommandArguments;
import ru.zov.implement.features.commands.manager.CommandRepository;

public class CommandDispatcher {
   private final ICommandManager manager = Main.getInstance().getCommandRepository();
   public static String prefix = ".";

   public CommandDispatcher(EventManager eventManager) {
      eventManager.register(this);
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      String msg = event.getMessage();
      boolean forceRun = msg.startsWith(IBaritoneChatControl.FORCE_COMMAND_PREFIX);
      if (msg.startsWith(prefix) || forceRun) {
         event.cancel();
         String commandStr = msg.substring(forceRun ? IBaritoneChatControl.FORCE_COMMAND_PREFIX.length() : prefix.length());
         if (!this.runCommand(commandStr) && !commandStr.trim().isEmpty()) {
            new CommandNotFoundException(CommandRepository.expand(commandStr).getLeft()).handle(null, null);
         }
      } else if (this.runCommand(msg)) {
         event.cancel();
      }
   }

   @Native
   public boolean runCommand(String msg) {
      if (msg.isEmpty()) {
         return this.runCommand("help");
      }

      Pair<String, List<ICommandArgument>> pair = CommandRepository.expand(msg);
      String command = pair.getLeft();
      String rest = msg.substring(pair.getLeft().length());
      new ArgConsumer(this.manager, pair.getRight());
      return this.manager.execute(pair);
   }

   @EventHandler
   public void onTabComplete(TabCompleteEvent event) {
      String eventPrefix = event.prefix;
      if (eventPrefix.startsWith(prefix)) {
         String msg = eventPrefix.substring(prefix.length());
         List<ICommandArgument> args = CommandArguments.from(msg, true);
         Stream<String> stream = this.tabComplete(msg);
         if (args.size() == 1) {
            stream = stream.map(x -> prefix + x);
         }

         event.completions = stream.toArray(String[]::new);
      }
   }

   public Stream<String> tabComplete(String msg) {
      try {
         List<ICommandArgument> args = CommandArguments.from(msg, true);
         ArgConsumer argc = new ArgConsumer(this.manager, args);
         return argc.hasAtMost(2) && argc.hasExactly(1)
            ? new TabCompleteHelper().addCommands(this.manager).filterPrefix(argc.getString()).stream()
            : this.manager.tabComplete(msg);
      } catch (CommandNotEnoughArgumentsException ignored) {
         return Stream.empty();
      }
   }
}
