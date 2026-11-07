package ru.zov.common.util.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.zov.common.QuickImports;
import ru.zov.common.util.rotation.Angle;

public abstract class AngleSmoothMode implements QuickImports {
   private final String name;

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, null, null);
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d) {
      return this.limitAngleChange(currentAngle, targetAngle, vec3d, null);
   }

   public abstract Angle limitAngleChange(Angle var1, Angle var2, Vec3d var3, Entity var4);

   public String getName() {
      return this.name;
   }

   public AngleSmoothMode(String name) {
      this.name = name;
   }
}
