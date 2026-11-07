package ru.zov.api.feature.command.datatypes;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;

public enum BlockESPDataType implements IDatatypeFor<Block> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> blocks = this.getBlocks().stream().map(b -> b.getName().getString().replace(" ", "_"));
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(blocks).filterPrefix(context).sortAlphabetically().stream();
   }

   public Block get(IDatatypeContext datatypeContext) throws CommandException {
      String text = datatypeContext.getConsumer().getString();
      return this.getBlocks().stream().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst().orElse(null);
   }

   private List<? extends Block> getBlocks() {
      return main.getBoxESPRepository().blocks.keySet().stream().toList();
   }
}
