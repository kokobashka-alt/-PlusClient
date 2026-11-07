package ru.zov.implement.events.render;

import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.events.Event;

public class CameraPositionEvent implements Event {
   private Vec3d pos;

   public Vec3d getPos() {
      return this.pos;
   }

   public void setPos(Vec3d pos) {
      this.pos = pos;
   }

   public CameraPositionEvent(Vec3d pos) {
      this.pos = pos;
   }
}
