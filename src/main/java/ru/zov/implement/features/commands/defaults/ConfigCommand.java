package ru.zov.implement.features.commands.defaults;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import ru.zov.api.file.FileController;
import ru.zov.api.file.FileRepository;
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
      String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
      args.requireMax(1);
      if (arg.contains("load")) {
         String name = args.getString();
         if (new File(this.clientInfoProvider.configsDir(), name + ".json").exists()) {
            try {
               FileRepository fileRepository = new FileRepository();
               fileRepository.setup(main);
               FileController fileController = new FileController(
                  fileRepository.getClientFiles(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir()
               );
               fileController.loadFile(name + ".json");
               this.logDirect(String.format("Конфигурация %s загружена!", name));
            } catch (FileProcessingException e) {
               this.logDirect(String.format("Ошибка при загрузке конфига! Детали: %s", e.getCause().getMessage()), Formatting.RED);
            }
         } else {
            this.logDirect(String.format("Конфигурация %s не найдена!", name));
         }
      }

      if (arg.contains("save")) {
         String name = args.getString();

         try {
            this.fileController.saveFile(name + ".json");
            this.logDirect(String.format("Конфигурация %s сохранена!", name));
         } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.printf("error %s%n", e.getCause().getMessage());
            this.logDirect(String.format("Ошибка при сохранении конфига! Детали: %s", e.getCause().getMessage()), Formatting.RED);
         }
      }

      if (arg.contains("list")) {
         Paginator.paginate(args, new Paginator<>(this.getConfigs()), () -> this.logDirect("Список конфигов:"), config -> {
            MutableText namesComponent = Text.literal(config);
            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
            return namesComponent;
         }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
      }

      if (arg.contains("dir")) {
         try {
            Runtime.getRuntime().exec("explorer " + this.clientInfoProvider.configsDir().getAbsolutePath());
         } catch (IOException e) {
            this.logDirect("Папка с конфигурациями не найдена!" + e.getMessage());
         }
      }
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
      List<String> configs = new ArrayList<>();
      File[] configFiles = main.getClientInfoProvider().configsDir().listFiles();
      if (configFiles != null) {
         for (File configFile : configFiles) {
            if (configFile.isFile() && configFile.getName().endsWith(".json")) {
               String configName = configFile.getName().replace(".json", "");
               configs.add(configName);
            }
         }
      }

      return configs;
   }
}
