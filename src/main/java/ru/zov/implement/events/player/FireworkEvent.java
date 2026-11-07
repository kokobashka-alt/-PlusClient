package ru.zov.implement.events.player;

import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.events.Event;

public class FireworkEvent implements Event {
   public Vec3d vector;

   public FireworkEvent(Vec3d vector) {
      this.vector = vector;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }
}
