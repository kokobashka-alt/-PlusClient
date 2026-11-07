package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.entity.EntityType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.IBaritoneChatControl;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.datatypes.BlockDataType;
import ru.zov.api.feature.command.datatypes.BlockESPDataType;
import ru.zov.api.feature.command.datatypes.EntityDataType;
import ru.zov.api.feature.command.datatypes.EntityESPDataType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.BoxESPRepository;
import ru.zov.common.QuickImports;
import ru.zov.core.Main;

public class BoxESPCommand extends Command implements QuickImports {
   private final BoxESPRepository repository;

   protected BoxESPCommand(Main main) {
      super("box");
      this.repository = main.getBoxESPRepository();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      switch (arg) {
         case "add":
            this.handleAdd(args);
            break;
         case "remove":
            this.handleRemove(args);
            break;
         case "clear":
            this.handleClear(args);
            break;
         case "list":
            this.handleList(label, args);
      }
   }

   @Native
   private void handleAdd(IArgConsumer args) throws CommandException {
      args.requireMin(2);
      String type = args.getString().toLowerCase();
      String name = args.getString();
      int color = args.has(1) ? args.getArgs().getFirst().getAs(Integer.class) : 0;
      switch (type) {
         case "block":
            BlockDataType.INSTANCE.findBlock(name).ifPresentOrElse(block -> {
               if (!this.repository.blocks.containsKey(block)) {
                  this.repository.blocks.put(block, color);
                  StringBuilder builder = new StringBuilder();
                  builder.append("Добавлен блок: ").append(Formatting.GREEN).append(block.getName().getString()).append(Formatting.GRAY);
                  if (color != 0) {
                     builder.append(", Цвет: ").append(Formatting.GREEN).append(color);
                  }

                  this.logDirect(builder.toString(), Formatting.GRAY);
               } else {
                  this.logDirect(Formatting.GRAY + "Блок \"" + Formatting.RED + block.getName().getString() + Formatting.GRAY + "\" уже добавлен");
               }
            }, () -> this.logDirect(Formatting.RED + "Не удалось найти блок"));
            break;
         case "entity":
            EntityDataType.INSTANCE.findEntity(name).ifPresentOrElse(entity -> {
               if (!this.repository.entities.containsKey(entity)) {
                  this.repository.entities.put((EntityType<?>)entity, color);
                  StringBuilder builder = new StringBuilder();
                  builder.append("Добавлена сущность: ").append(Formatting.GREEN).append(entity.getName().getString()).append(Formatting.GRAY);
                  if (color != 0) {
                     builder.append(", Цвет: ").append(Formatting.GREEN).append(color);
                  }

                  this.logDirect(builder.toString(), Formatting.GRAY);
               } else {
                  this.logDirect(Formatting.GRAY + "Сущность \"" + Formatting.RED + entity.getName().getString() + Formatting.GRAY + "\" уже добавлен");
               }
            }, () -> this.logDirect(Formatting.RED + "Не удалось найти сущность"));
            break;
         default:
            this.logDirect(Formatting.RED + "Не верный тип");
      }
   }

   @Native
   private void handleRemove(IArgConsumer args) throws CommandException {
      args.requireMin(2);
      String type = args.getString().toLowerCase();
      String name = args.getString();
      switch (type) {
         case "block":
            this.repository
               .blocks
               .keySet()
               .stream()
               .filter(block -> block.getName().getString().replace(" ", "_").equalsIgnoreCase(name))
               .findFirst()
               .ifPresentOrElse(block -> {
                  this.repository.blocks.remove(block);
                  this.logDirect("Удален блок: " + Formatting.GREEN + block.getName().getString(), Formatting.GRAY);
               }, () -> this.logDirect(Formatting.RED + "Не удалось найти блок"));
            break;
         case "entity":
            this.repository
               .entities
               .keySet()
               .stream()
               .filter(block -> block.getName().getString().replace(" ", "_").equalsIgnoreCase(name))
               .findFirst()
               .ifPresentOrElse(block -> {
                  this.repository.entities.remove(block);
                  this.logDirect("Удалена сущность: " + Formatting.GREEN + block.getName().getString(), Formatting.GRAY);
               }, () -> this.logDirect(Formatting.RED + "Не удалось найти сущность"));
            break;
         default:
            this.logDirect(Formatting.RED + "Не верный тип");
      }
   }

   @Native
   private void handleList(String label, IArgConsumer args) throws CommandException {
      args.requireMin(1);
      String type = args.getString().toLowerCase();
      if (!args.hasAny() || args.is(Integer.class)) {
         switch (type) {
            case "block":
               Paginator.paginate(
                  args, new Paginator<>(this.repository.blocks.entrySet().stream().toList()), () -> this.logDirect("Список блоков:"), block -> {
                     MutableText text = Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + block.getKey().getName().getString());
                     if (block.getValue() != 0) {
                        text.append(Formatting.GRAY + "Цвет: " + Formatting.RED + block.getValue());
                     }

                     return text;
                  }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
               );
               break;
            case "entity":
               Paginator.paginate(
                  args, new Paginator<>(this.repository.entities.entrySet().stream().toList()), () -> this.logDirect("Список Сущностей:"), entity -> {
                     MutableText text = Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + entity.getKey().getName().getString());
                     if (entity.getValue() != 0) {
                        text.append(Formatting.GRAY + "Цвет: " + Formatting.RED + entity.getValue());
                     }

                     return text;
                  }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label
               );
               break;
            default:
               this.logDirect(Formatting.RED + "Не верный тип");
         }
      }
   }

   @Native
   private void handleClear(IArgConsumer args) throws CommandException {
      args.requireMin(1);
      String type = args.getString().toLowerCase();
      switch (type) {
         case "block":
            this.repository.blocks.clear();
            this.logDirect(Formatting.GREEN + "Все блоки были удалены.");
            break;
         case "entity":
            this.repository.entities.clear();
            this.logDirect(Formatting.GREEN + "Все сущности были удалены.");
            break;
         default:
            this.logDirect(Formatting.RED + "Не верный тип");
      }
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      String arg = args.getString();
      if (args.has(4)) {
         return Stream.empty();
      }

      if (args.has(3)) {
         return new TabCompleteHelper().append("Цвет").sortAlphabetically().stream();
      }

      if (args.has(2)) {
         String nextArg = args.getString().toLowerCase();
         switch (nextArg) {
            case "block":
               if (arg.equalsIgnoreCase("remove")) {
                  return args.tabCompleteDatatype(BlockESPDataType.INSTANCE);
               }

               if (arg.equalsIgnoreCase("add")) {
                  return args.tabCompleteDatatype(BlockDataType.INSTANCE);
               }
               break;
            case "entity":
               if (arg.equalsIgnoreCase("remove")) {
                  return args.tabCompleteDatatype(EntityESPDataType.INSTANCE);
               }

               if (arg.equalsIgnoreCase("add")) {
                  return args.tabCompleteDatatype(EntityDataType.INSTANCE);
               }
         }

         return Stream.empty();
      } else {
         return args.hasAny()
            ? new TabCompleteHelper().append("Block", "Entity").filterPrefix(args.getString()).sortAlphabetically().stream()
            : new TabCompleteHelper().append("add", "remove", "list", "clear").filterPrefix(arg).sortAlphabetically().stream();
      }
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет отображать боксы в мире";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно добавлять/удалять рендер боксов в мире",
         "",
         "Использование:",
         "> box add <Block/Entity> <block/entity> <color> - Добавляет блок/сущность",
         "> box remove <Block/Entity> <block/entity> - Удаляет блок/сущность",
         "> box list <Block/Entity> - Возвращает список блоков/сущностей",
         "> box clear <Block/Entity> - Очищает блоков/сущностей"
      );
   }
}
