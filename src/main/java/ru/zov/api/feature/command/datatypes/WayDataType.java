package ru.zov.api.feature.command.datatypes;

import java.util.List;
import java.util.stream.Stream;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.WayRepository;

public enum WayDataType implements IDatatypeFor<WayRepository.Way> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> ways = this.getWay().stream().map(WayRepository.Way::name);
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
   }

   public WayRepository.Way get(IDatatypeContext datatypeContext) throws CommandException {
      String text = datatypeContext.getConsumer().getString();
      return this.getWay().stream().filter(s -> s.name().equalsIgnoreCase(text)).findFirst().orElse(null);
   }

   private List<? extends WayRepository.Way> getWay() {
      return main.getWayRepository().wayList;
   }
}
