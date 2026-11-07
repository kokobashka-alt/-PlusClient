package ru.zov.implement.features.commands.defaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.ICommand;
import ru.zov.core.Main;

public final class DefaultCommands {
   @Native
   public static List<ICommand> createAll() {
      Main main = Main.getInstance();
      List<ICommand> commands = new ArrayList<>(
         Arrays.asList(
            new AutoFarmCommand(main),
            new BoxESPCommand(main),
            new ConfigCommand(main),
            new MacroCommand(main),
            new HelpCommand(main),
            new BindCommand(main),
            new WayCommand(main),
            new RCTCommand(main),
            new IRCCommand(main),
            new FriendCommand(),
            new PrefixCommand(),
            new DebugCommand()
         )
      );
      return Collections.unmodifiableList(commands);
   }
}
