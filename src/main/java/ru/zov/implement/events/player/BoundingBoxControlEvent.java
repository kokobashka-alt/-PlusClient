package ru.zov.implement.events.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import ru.zov.api.event.events.callables.EventCancellable;

public class BoundingBoxControlEvent extends EventCancellable {
   public Box box;
   public Entity entity;

   public Box getBox() {
      return this.box;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public void setBox(Box box) {
      this.box = box;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public BoundingBoxControlEvent(Box box, Entity entity) {
      this.box = box;
      this.entity = entity;
   }
}
