package ru.zov.implement.events.player;

import net.minecraft.util.hit.HitResult;
import ru.zov.api.event.events.Event;

public class CrosshairTargetEvent implements Event {
   private HitResult result;

   public HitResult getResult() {
      return this.result;
   }

   public void setResult(HitResult result) {
      this.result = result;
   }

   public CrosshairTargetEvent(HitResult result) {
      this.result = result;
   }
}
