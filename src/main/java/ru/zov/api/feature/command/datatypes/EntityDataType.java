package ru.zov.api.feature.command.datatypes;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;

public enum EntityDataType implements IDatatypeFor<EntityType<?>> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
      Stream<String> ways = this.streamEntities().map(s -> s.getName().getString().replace(" ", "_"));
      String context = ctx.getConsumer().getString();
      return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
   }

   public EntityType<?> get(IDatatypeContext datatypeContext) throws CommandException {
      return this.findEntity(datatypeContext.getConsumer().getString()).orElse(null);
   }

   public Optional<EntityType<?>> findEntity(String text) {
      return this.streamEntities().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst();
   }

   public Stream<EntityType<?>> streamEntities() {
      return Registries.ENTITY_TYPE.stream().filter(type -> !main.getBoxESPRepository().entities.containsKey(type));
   }
}
