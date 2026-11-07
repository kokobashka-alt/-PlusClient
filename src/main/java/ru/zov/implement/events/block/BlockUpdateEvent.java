package ru.zov.implement.events.block;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.zov.api.event.events.Event;

public record BlockUpdateEvent(List<BlockUpdateEvent.BlockData> list, BlockUpdateEvent.Type type, ChunkPos chunkPos) implements Event {
   public BlockUpdateEvent(List<BlockData> list, Type type) {
      this(list, type, null);
   }

   public record BlockData(BlockPos pos, BlockState state, int light) {
   }

   public enum Type {
      LOAD,
      UNLOAD,
      UPDATE,
      LIGHT;
   }
}
