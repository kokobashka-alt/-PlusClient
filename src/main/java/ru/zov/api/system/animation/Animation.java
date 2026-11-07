package ru.zov.api.system.animation;

import net.minecraft.util.math.MathHelper;
import ru.zov.common.util.math.StopWatch;

public abstract class Animation implements AnimationCalculation {
   public final StopWatch counter = new StopWatch();
   protected int ms;
   protected double value;
   protected Direction direction = Direction.FORWARDS;

   public void reset() {
      this.counter.reset();
   }

   public boolean isDone() {
      return this.counter.finished(this.ms);
   }

   public boolean isFinished(Direction direction) {
      return this.isDirection(direction) && this.isDone();
   }

   public Animation setDirection(boolean forward) {
      return this.setDirection(forward ? Direction.FORWARDS : Direction.BACKWARDS);
   }

   public Animation setDirection(Direction direction) {
      if (this.direction != direction) {
         this.direction = direction;
         this.adjustTimer();
      }

      return this;
   }

   public boolean isDirection(Direction direction) {
      return this.direction.equals(direction);
   }

   private void adjustTimer() {
      if (this.isDone()) {
         this.reset();
      } else {
         this.counter.setMs((int)(this.getOutput() * this.ms));
      }
   }

   public Double getOutput() {
      if (this.isDone()) {
         return this.direction.equals(Direction.FORWARDS) ? 1.0 * this.value : 0.0;
      }

      double calc = MathHelper.clamp(this.calculation(this.counter.elapsedLong()), 0.0, 1.0);
      return this.direction.equals(Direction.FORWARDS) ? calc * this.value : (1.0 - calc) * this.value;
   }

   public Animation setMs(int ms) {
      this.ms = ms;
      return this;
   }

   public Animation setValue(double value) {
      this.value = value;
      return this;
   }
}
