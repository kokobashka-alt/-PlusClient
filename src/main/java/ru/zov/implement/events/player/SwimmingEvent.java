package ru.zov.implement.events.player;

import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.events.callables.EventCancellable;

public class SwimmingEvent extends EventCancellable {
   Vec3d vector;

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public SwimmingEvent(Vec3d vector) {
      this.vector = vector;
   }
}
