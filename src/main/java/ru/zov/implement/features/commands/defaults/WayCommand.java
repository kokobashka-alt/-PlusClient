package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.IBaritoneChatControl;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.datatypes.WayDataType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.WayRepository;
import ru.zov.common.QuickImports;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.core.Main;

public class WayCommand extends Command implements QuickImports {
   private final WayRepository wayRepository;

   protected WayCommand(Main main) {
      super("way");
      this.wayRepository = main.getWayRepository();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (arg) {
         case "add":
            this.handleAddWay(args);
            break;
         case "remove":
            this.handleRemoveWay(args);
            break;
         case "clear":
            this.handleClearWays(args);
            break;
         case "list":
            this.handleListWays(label, args);
      }
   }

   @Native
   private void handleAddWay(IArgConsumer args) throws CommandException {
      args.requireMin(4);
      String name = args.getString();
      int x = args.getArgs().get(0).getAs(Integer.class);
      int y = args.getArgs().get(1).getAs(Integer.class);
      int z = args.getArgs().get(2).getAs(Integer.class);
      if (this.wayRepository.hasWay(name)) {
         this.logDirect("Метка с таким именем уже есть в списке!", Formatting.RED);
      } else {
         this.logDirect("Добавлена метка " + name + ", Координаты: (" + x + ", " + y + ", " + z + "), Сервер: " + ServerUtil.server, Formatting.GRAY);
         this.wayRepository.addWay(name, new BlockPos(x, y, z), ServerUtil.server);
      }
   }

   @Native
   private void handleRemoveWay(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      String name = args.getString();
      if (this.wayRepository.hasWay(name)) {
         this.wayRepository.deleteWay(name);
         this.logDirect(Formatting.GREEN + "Метка " + Formatting.RED + name + Formatting.GREEN + " была успешна удалена!");
      } else {
         this.logDirect("Метка с названием '" + name + "' не найдена!");
      }
   }

   @Native
   private void handleListWays(String label, IArgConsumer args) throws CommandException {
      args.requireMax(1);
      Paginator.paginate(
         args,
         new Paginator<>(this.wayRepository.wayList),
         () -> this.logDirect("Список меток:"),
         way -> Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + way.name())
            .append(
               Text.literal(
                     Formatting.GRAY + " Координаты: " + Formatting.WHITE + " (" + way.pos().getX() + ", " + way.pos().getY() + ", " + way.pos().getZ() + ")"
                  )
                  .append(Text.literal(Formatting.GRAY + " Сервер: " + Formatting.WHITE + way.server()))
            ),
         IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
      );
   }

   @Native
   private void handleClearWays(IArgConsumer args) throws CommandException {
      args.requireMax(1);
      this.wayRepository.clearList();
      this.logDirect(Formatting.GREEN + "Все метки были удалены.");
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!arg.equalsIgnoreCase("remove")) {
            if (arg.equalsIgnoreCase("add")) {
               String string = args.has(5) ? "" : (args.has(4) ? "z" : (args.has(3) ? "y" : (args.has(2) ? "x" : "Название")));
               return new TabCompleteHelper().sortAlphabetically().prepend(string).stream();
            }

            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(arg).stream();
         }

         if (args.hasExactlyOne()) {
            return args.tabCompleteDatatype(WayDataType.INSTANCE);
         }
      }

      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет ставить метки в мире";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно добавлять/удалять метки в мире",
         "",
         "Использование:",
         "> way add <name> <x> <y> <z> - Добавляет метку",
         "> way remove <name> - Удаляет метку",
         "> way list - Возвращает список меток",
         "> way clear - Очищает список меток."
      );
   }
}
