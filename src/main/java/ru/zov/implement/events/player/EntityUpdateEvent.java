package ru.zov.implement.events.player;

import net.minecraft.entity.Entity;
import ru.zov.api.event.events.Event;

public record EntityUpdateEvent(Entity entity, EntityUpdateEvent.Type type) implements Event {
   public enum Type {
      ADD,
      REMOVE;
   }
}
