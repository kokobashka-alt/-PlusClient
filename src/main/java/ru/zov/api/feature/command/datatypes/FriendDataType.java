package ru.zov.api.feature.command.datatypes;

import java.util.stream.Stream;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.api.repository.FriendUtils;

public enum FriendDataType implements IDatatypeFor<String> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(FriendUtils.getFriends().stream()).filterPrefix(context).sortAlphabetically().stream();
   }

   public String get(IDatatypeContext datatypeContext) throws CommandException {
      String username = datatypeContext.getConsumer().getString();
      return FriendUtils.getFriends().stream().filter(s -> s.equalsIgnoreCase(username)).findFirst().orElse(null);
   }
}
