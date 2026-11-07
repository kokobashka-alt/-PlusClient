package ru.zov.common.util.rotation;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.zov.common.QuickImports;

public final class AngleUtil implements QuickImports {
   public static Angle fromVec2f(Vec2f vector2f) {
      return new Angle(vector2f.y, vector2f.x);
   }

   public static Angle fromVec3d(Vec3d vector) {
      return new Angle(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   public static Angle calculateDelta(Angle start, Angle end) {
      float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
      float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
      return new Angle(deltaYaw, deltaPitch);
   }

   public static Angle calculateAngle(Vec3d to) {
      return fromVec3d(to.subtract(mc.player.getEyePos()));
   }

   public static Angle pitch(float pitch) {
      return mc.player != null ? new Angle(mc.player.getYaw(), pitch) : Angle.DEFAULT;
   }

   public static Angle cameraAngle() {
      return mc.player != null ? new Angle(mc.player.getYaw(), mc.player.getPitch()) : Angle.DEFAULT;
   }

   private AngleUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
