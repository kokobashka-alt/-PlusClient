package ru.zov.api.feature.command.datatypes;

import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.scoreboard.Team;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;

public enum TabPlayerDataType implements IDatatypeFor<Team> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      return new TabCompleteHelper()
         .append(this.getTeam().stream().map(Team::getPlayerList).map(Object::toString).map(s -> s.replaceAll("[\\[\\]]", "")))
         .filterPrefix(ctx.getConsumer().getString())
         .sortAlphabetically()
         .stream();
   }

   public Team get(IDatatypeContext datatypeContext) throws CommandException {
      String username = datatypeContext.getConsumer().getString();
      return this.getTeam().stream().filter(s -> s.getName().equalsIgnoreCase(username)).findFirst().orElse(null);
   }

   public Collection<Team> getTeam() {
      return mc.world.getScoreboard().getTeams();
   }
}
