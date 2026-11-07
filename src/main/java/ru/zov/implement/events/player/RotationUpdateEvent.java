package ru.zov.implement.events.player;

import ru.zov.api.event.events.Event;

public record RotationUpdateEvent(byte type) implements Event {
   public boolean isPre() {
      return this.type == 0;
   }

   public boolean isPost() {
      return this.type == 0;
   }
}
