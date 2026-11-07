package ru.zov.api.feature.command.manager;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Pair;
import ru.zov.api.feature.command.ICommand;
import ru.zov.api.feature.command.argument.ICommandArgument;
import ru.zov.api.feature.command.registry.Registry;

public interface ICommandManager {
   Registry<ICommand> getRegistry();

   ICommand getCommand(String var1);

   boolean execute(String var1);

   boolean execute(Pair<String, List<ICommandArgument>> var1);

   Stream<String> tabComplete(Pair<String, List<ICommandArgument>> var1);

   Stream<String> tabComplete(String var1);
}
