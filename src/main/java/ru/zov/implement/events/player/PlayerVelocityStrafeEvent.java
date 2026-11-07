package ru.zov.implement.events.player;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.events.Event;
import ru.zov.common.util.rotation.Angle;

public class PlayerVelocityStrafeEvent implements Event {
   private final Vec3d movementInput;
   private final float speed;
   private final float yaw;
   private Vec3d velocity;

   public void setVelocity(Angle angle) {
      if (angle != null) {
         float yaw = angle.getYaw();
         double d = this.movementInput.lengthSquared();
         if (d < 1.0E-7) {
            this.velocity = Vec3d.ZERO;
         } else {
            Vec3d vec3d = (d > 1.0 ? this.movementInput.normalize() : this.movementInput).multiply(this.speed);
            float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
            float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
            this.velocity = new Vec3d(vec3d.getX() * g - vec3d.getZ() * f, vec3d.getY(), vec3d.getZ() * g + vec3d.getX() * f);
         }
      }
   }

   public Vec3d getMovementInput() {
      return this.movementInput;
   }

   public float getSpeed() {
      return this.speed;
   }

   public float getYaw() {
      return this.yaw;
   }

   public Vec3d getVelocity() {
      return this.velocity;
   }

   public PlayerVelocityStrafeEvent(Vec3d movementInput, float speed, float yaw, Vec3d velocity) {
      this.movementInput = movementInput;
      this.speed = speed;
      this.yaw = yaw;
      this.velocity = velocity;
   }
}
