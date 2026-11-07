package ru.zov.implement.features.commands.defaults;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.datatypes.AutoFarmDataType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.AutoFarmRepository;
import ru.zov.core.Main;

public class AutoFarmCommand extends Command {
   private final AutoFarmRepository repository;

   public AutoFarmCommand(Main main) {
      super("autofarm");
      this.repository = main.getAutoFarmRepository();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (arg) {
         case "start":
            this.repository.workWatch.reset();
            this.repository.start = true;
            this.repository.count = 0;
            break;
         case "stop":
            this.repository.start = false;
            break;
         case "add":
            args.requireMin(3);
            BlockPos pos = BlockPos.ofFloored(
               args.getArgs().get(0).getAs(Integer.class).intValue(),
               args.getArgs().get(1).getAs(Integer.class).intValue(),
               args.getArgs().get(2).getAs(Integer.class).intValue()
            );
            if (this.repository.points.contains(pos)) {
               this.logDirect(String.format("Поинт уже добавлен: %d %d %d", pos.getX(), pos.getY(), pos.getZ()), Formatting.RED);
               return;
            }

            this.repository.points.add(pos);
            this.logDirect(String.format("Добавлен поинт: %d %d %d", pos.getX(), pos.getY(), pos.getZ()), Formatting.GRAY);
            break;
         case "remove":
            args.requireMin(3);
            BlockPos removePos = BlockPos.ofFloored(
               args.getArgs().get(0).getAs(Integer.class).intValue(),
               args.getArgs().get(1).getAs(Integer.class).intValue(),
               args.getArgs().get(2).getAs(Integer.class).intValue()
            );
            if (!this.repository.points.contains(removePos)) {
               this.logDirect(
                  String.format("Поинт не найден: %d %d %d", removePos.getX(), removePos.getY(), removePos.getZ()), Formatting.RED
               );
               return;
            }

            this.repository.points.remove(removePos);
            this.logDirect(
               String.format("Удален поинт: %d %d %d", removePos.getX(), removePos.getY(), removePos.getZ()), Formatting.GRAY
            );
            break;
         case "list":
            args.requireMax(1);
            Paginator.paginate(
               args,
               new Paginator<>(this.repository.points),
               () -> this.logDirect("Список меток:"),
               posx -> Text.literal(Formatting.GRAY + "Позиция: " + Formatting.RED + this.repository.points.indexOf(posx))
                  .append(
                     Text.literal(Formatting.GRAY + " Координаты: " + Formatting.WHITE + " (" + posx.getX() + ", " + posx.getY() + ", " + posx.getZ() + ")")
                  )
            );
            break;
         case "clear":
            this.repository.points.clear();
            this.logDirect("Поинты успешно удалены", Formatting.GRAY);
      }
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!arg.equalsIgnoreCase("remove")) {
            if (arg.equalsIgnoreCase("add")) {
               String string = args.has(4) ? "" : (args.has(3) ? "z" : (args.has(2) ? "y" : "x"));
               return new TabCompleteHelper().sortAlphabetically().prepend(string).stream();
            }

            return new TabCompleteHelper().sortAlphabetically().prepend("start", "stop", "add", "remove", "list", "clear").filterPrefix(arg).stream();
         }

         if (args.hasExactlyOne()) {
            return args.tabCompleteDatatype(AutoFarmDataType.INSTANCE);
         }
      }

      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет автоматически фармить нарост на HolyWorld";
   }

   @Override
   public List<String> getLongDesc() {
      return List.of();
   }
}
