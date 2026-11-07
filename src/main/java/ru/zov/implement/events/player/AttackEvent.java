package ru.zov.implement.events.player;

import net.minecraft.entity.Entity;
import ru.zov.api.event.events.callables.EventCancellable;

public class AttackEvent extends EventCancellable {
   private Entity entity;

   public Entity getEntity() {
      return this.entity;
   }

   public AttackEvent(Entity entity) {
      this.entity = entity;
   }
}
