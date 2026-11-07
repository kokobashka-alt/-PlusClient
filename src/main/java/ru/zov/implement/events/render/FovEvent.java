package ru.zov.implement.events.render;

import ru.zov.api.event.events.callables.EventCancellable;

public class FovEvent extends EventCancellable {
   private int fov;

   public int getFov() {
      return this.fov;
   }

   public void setFov(int fov) {
      this.fov = fov;
   }

   public FovEvent(int fov) {
      this.fov = fov;
   }
}
