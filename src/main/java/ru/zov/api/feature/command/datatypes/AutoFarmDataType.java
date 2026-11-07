package ru.zov.api.feature.command.datatypes;

import java.util.stream.Stream;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;

public enum AutoFarmDataType implements IDatatypeFor<String> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(this.getPoints()).filterPrefix(context).sortAlphabetically().stream();
   }

   public String get(IDatatypeContext datatypeContext) throws CommandException {
      String text = datatypeContext.getConsumer().getString();
      return this.getPoints().filter(s -> s.equalsIgnoreCase(text)).findFirst().orElse(null);
   }

   private Stream<String> getPoints() {
      return main.getAutoFarmRepository().points.stream().map(pos -> pos.getX() + " " + pos.getY() + " " + pos.getZ());
   }
}
