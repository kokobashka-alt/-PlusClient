package ru.zov.implement.features.commands.defaults;

import java.io.File;
import java.io.IOException;
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
import ru.zov.api.feature.command.datatypes.ConfigFileDataType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.Paginator;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.file.ConfigFiles;
import ru.zov.api.file.FileController;
import ru.zov.api.file.exception.FileProcessingException;
import ru.zov.core.Main;
import ru.zov.core.client.ClientInfoProvider;

public class ConfigCommand extends Command {
   private final FileController fileController;
   private final ClientInfoProvider clientInfoProvider;

   protected ConfigCommand(Main main) {
      super("config", "cfg");
      this.fileController = main.getFileController();
      this.clientInfoProvider = main.getClientInfoProvider();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      String sub = args.hasAny() ? args.getString().toLowerCase(Locale.ROOT) : "list";
      switch (sub) {
         case "load" -> {
            args.requireMax(1);
            this.load(args.getString());
         }
         case "save" -> {
            args.requireMax(1);
            this.save(args.getString());
         }
         case "list" -> Paginator.paginate(args, new Paginator<>(this.getConfigs()), () -> this.logDirect("Список конфигов:"), config -> {
            MutableText namesComponent = Text.literal(config);
            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
            return namesComponent;
         }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
         case "dir" -> this.openDirectory();
         default -> this.logDirect("Неизвестная подкоманда. Доступно: load, save, list, dir", Formatting.RED);
      }
   }

   private void load(String rawName) {
      String name = ConfigFiles.normalizeName(rawName);
      if (name == null) {
         this.logDirect("Недопустимое имя конфига!", Formatting.RED);
         return;
      }

      File file = ConfigFiles.file(this.clientInfoProvider.configsDir(), name);
      if (!file.isFile()) {
         this.logDirect(String.format("Конфигурация %s не найдена!", name));
         return;
      }

      try {
         this.fileController.loadFile(file.getName());
         this.logDirect(String.format("Конфигурация %s загружена!", name));
      } catch (FileProcessingException | RuntimeException e) {
         this.logDirect(String.format("Ошибка при загрузке конфига! Детали: %s", describe(e)), Formatting.RED);
      }
   }

   private void save(String rawName) {
      String name = ConfigFiles.normalizeName(rawName);
      if (name == null) {
         this.logDirect("Недопустимое имя конфига!", Formatting.RED);
         return;
      }

      try {
         this.fileController.saveFile(ConfigFiles.fileName(name));
         this.logDirect(String.format("Конфигурация %s сохранена!", name));
      } catch (FileProcessingException | RuntimeException e) {
         this.logDirect(String.format("Ошибка при сохранении конфига! Детали: %s", describe(e)), Formatting.RED);
      }
   }

   private void openDirectory() {
      File directory = this.clientInfoProvider.configsDir();
      directory.mkdirs();
      String path = directory.getAbsolutePath();
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

      try {
         ProcessBuilder builder = os.contains("win")
            ? new ProcessBuilder("explorer", path)
            : (os.contains("mac") ? new ProcessBuilder("open", path) : new ProcessBuilder("xdg-open", path));
         builder.start();
      } catch (IOException e) {
         this.logDirect("Не удалось открыть папку с конфигурациями: " + e.getMessage(), Formatting.RED);
      }
   }

   private static String describe(Exception e) {
      String message = e.getMessage();
      Throwable cause = e.getCause();
      if (cause != null && cause.getMessage() != null) {
         message = message == null ? cause.getMessage() : message + ": " + cause.getMessage();
      }

      return message == null ? e.getClass().getSimpleName() : message;
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      if (args.hasAny()) {
         String arg = args.getString();
         if (!args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("load", "save", "list", "dir").filterPrefix(arg).stream();
         }

         if (arg.equalsIgnoreCase("load")) {
            return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
         }

         if (arg.equalsIgnoreCase("save")) {
            return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
         }
      }

      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Позволяет взаимодействовать с конфигами в чите";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно загружать/сохранять конфиги",
         "",
         "Использование:",
         "> config load <name> - Загружает конфиг.",
         "> config save <name> - Сохраняет конфиг.",
         "> config list - Возвращает список конфигов",
         "> config dir - Открывает папку с конфигами."
      );
   }

   public List<String> getConfigs() {
      return ConfigFiles.list(this.clientInfoProvider.configsDir());
   }
}
