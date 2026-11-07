package ru.zov.implement.events.block;

import ru.zov.api.event.events.callables.EventCancellable;

public class PushEvent extends EventCancellable {
   private PushEvent.Type type;

   public PushEvent.Type getType() {
      return this.type;
   }

   public PushEvent(PushEvent.Type type) {
      this.type = type;
   }

   public enum Type {
      BLOCK,
      WATER;
   }
}
