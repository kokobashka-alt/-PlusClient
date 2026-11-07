package ru.zov.api.event.events.callables;

import ru.zov.api.event.events.Event;
import ru.zov.api.event.events.Typed;

public abstract class EventTyped implements Event, Typed {
   private final byte type;

   protected EventTyped(byte eventType) {
      this.type = eventType;
   }

   @Override
   public byte getType() {
      return this.type;
   }
}
