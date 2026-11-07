package ru.zov.api.system.animation.implement;

import ru.zov.api.system.animation.Animation;

public class DecelerateAnimation extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return 1.0 - (x - 1.0) * (x - 1.0);
   }
}
