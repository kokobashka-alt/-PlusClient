package ru.zov.implement.events.player;

import ru.zov.api.event.events.callables.EventCancellable;

public class PushAwayEvent extends EventCancellable {
   double x;
   double z;

   public void setX(double x) {
      this.x = x;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public double getX() {
      return this.x;
   }

   public double getZ() {
      return this.z;
   }

   public PushAwayEvent(double x, double z) {
      this.x = x;
      this.z = z;
   }
}
