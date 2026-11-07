package ru.zov.api.feature.command.datatypes;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.entity.EntityType;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;

public enum EntityESPDataType implements IDatatypeFor<EntityType<?>> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      Stream<String> ways = this.getEntities().map(s -> s.getName().getString().replace(" ", "_"));
      String context = ctx.getConsumer().getString();
      return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
   }

   public EntityType<?> get(IDatatypeContext datatypeContext) throws CommandException {
      return this.findEntity(datatypeContext.getConsumer().getString()).orElse(null);
   }

   public Optional<EntityType<?>> findEntity(String text) {
      return this.getEntities().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst();
   }

   private Stream<EntityType<?>> getEntities() {
      return main.getBoxESPRepository().entities.keySet().stream();
   }
}
