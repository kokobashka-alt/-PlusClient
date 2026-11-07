package ru.zov.implement.events.item;

import ru.zov.api.event.events.callables.EventCancellable;

public class UsingItemEvent extends EventCancellable {
   byte type;

   public byte getType() {
      return this.type;
   }

   public void setType(byte type) {
      this.type = type;
   }

   public UsingItemEvent(byte type) {
      this.type = type;
   }
}
