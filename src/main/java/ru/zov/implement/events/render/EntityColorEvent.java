package ru.zov.implement.events.render;

import ru.zov.api.event.events.callables.EventCancellable;

public class EntityColorEvent extends EventCancellable {
   private int color;

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public EntityColorEvent(int color) {
      this.color = color;
   }
}
