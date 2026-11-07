package ru.zov.api.feature.command.datatypes;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.file.ClientFile;
import ru.zov.api.file.impl.ModuleFile;

public enum ConfigDataType implements IDatatypeFor<ClientFile> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      Stream<String> friends = this.getList().stream().map(ClientFile::getName);
      String context = ctx.getConsumer().getString();
      return new TabCompleteHelper().append(friends).filterPrefix(context).sortAlphabetically().stream();
   }

   public ClientFile get(IDatatypeContext datatypeContext) throws CommandException {
      String username = datatypeContext.getConsumer().getString();
      return this.getList().stream().filter(s -> s.getName().equalsIgnoreCase(username)).findFirst().orElse(null);
   }

   public List<? extends ModuleFile> getList() {
      return main.getFileRepository()
         .getClientFiles()
         .stream()
         .filter(clientFile -> clientFile instanceof ModuleFile)
         .map(clientFile -> (ModuleFile)clientFile)
         .collect(Collectors.toList());
   }
}
