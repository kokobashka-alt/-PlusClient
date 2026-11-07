package ru.zov.api.feature.command.datatypes;

import java.util.List;
import java.util.stream.Stream;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.MacroRepository;

public enum MacroDataType implements IDatatypeFor<MacroRepository.Macro> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> macros = this.getMacro().stream().map(MacroRepository.Macro::name);
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(macros).filterPrefix(context).sortAlphabetically().stream();
   }

   public MacroRepository.Macro get(IDatatypeContext datatypeContext) throws CommandException {
      String username = datatypeContext.getConsumer().getString();
      return this.getMacro().stream().filter(s -> s.name().equalsIgnoreCase(username)).findFirst().orElse(null);
   }

   private List<? extends MacroRepository.Macro> getMacro() {
      return main.getMacroRepository().macroList;
   }
}
