package ru.zov.common.util.math;

public class StopWatch {
   private long startTime;

   public StopWatch() {
      this.reset();
   }

   public boolean finished() {
      return this.elapsedLong() > 0L;
   }

   public boolean finished(double delay) {
      return System.currentTimeMillis() - delay >= this.startTime;
   }

   public boolean every(double delay) {
      boolean finished = this.finished(delay);
      if (finished) {
         this.reset();
      }

      return finished;
   }

   public void reset() {
      this.startTime = System.currentTimeMillis();
   }

   public int elapsedInt() {
      return Math.toIntExact(Math.min(this.elapsedLong(), 999999999L));
   }

   public long elapsedLong() {
      return System.currentTimeMillis() - this.startTime;
   }

   public StopWatch setMs(int ms) {
      this.startTime = System.currentTimeMillis() - ms;
      return this;
   }

   public StopWatch setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
      return this;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public long getStartTime() {
      return this.startTime;
   }
}
