package ru.zov.implement.events.block;

import net.minecraft.block.entity.BlockEntity;
import ru.zov.api.event.events.Event;

public record BlockEntityProgressEvent(BlockEntity blockEntity, BlockEntityProgressEvent.Type type) implements Event {
   public enum Type {
      ADD,
      UPDATE,
      REMOVE;
   }
}
