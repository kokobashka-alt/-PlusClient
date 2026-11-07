package ru.zov.common.util.rotation;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.common.util.math.MathUtil;

public class Angle {
   public static Angle DEFAULT = new Angle(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Angle adjustSensitivity() {
      double gcd = MathUtil.computeGcd();
      Angle previousAngle = RotationController.INSTANCE.getServerAngle();
      float adjustedYaw = this.adjustAxis(this.yaw, previousAngle.yaw, gcd);
      float adjustedPitch = this.adjustAxis(this.pitch, previousAngle.pitch, gcd);
      return new Angle(adjustedYaw, MathHelper.clamp(adjustedPitch, -90.0F, 90.0F));
   }

   public Angle random(float f) {
      return this.add(MathUtil.getRandom(-f, f), MathUtil.getRandom(-f, f));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd) {
      float delta = axisValue - previousValue;
      return previousValue + (float)Math.round(delta / gcd) * (float)gcd;
   }

   public final Vec3d toVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float g = -this.yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public Angle add(Angle angle) {
      return this.add(angle.getYaw(), angle.getPitch());
   }

   public Angle add(float yaw, float pitch) {
      return new Angle(this.yaw + yaw, MathHelper.clamp(this.pitch + pitch, -90.0F, 90.0F));
   }

   public Angle addYaw(float yaw) {
      return new Angle(this.yaw + yaw, this.pitch);
   }

   public Angle addPitch(float pitch) {
      return new Angle(this.yaw, MathHelper.clamp(this.pitch + pitch, -90.0F, 90.0F));
   }

   public Angle of(Angle angle) {
      return new Angle(angle.getYaw(), angle.getPitch());
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Override
   public String toString() {
      return "Angle(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   public Angle(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public record VecRotation(Angle angle, Vec3d vec) {
      public static Angle.VecRotation of(Angle angle) {
         return new Angle.VecRotation(angle, angle.toVector());
      }
   }
}
