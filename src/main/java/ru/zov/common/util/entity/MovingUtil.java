package ru.zov.common.util.entity;

import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.common.QuickImports;
import ru.zov.common.util.rotation.RotationController;

public final class MovingUtil implements QuickImports {
   public static boolean hasPlayerMovement() {
      return (mc.player.input.playerInput.forward() ? 1.0F : (mc.player.input.playerInput.backward() ? -1.0F : 0.0F)) != 0.0F
            || (mc.player.input.playerInput.left() ? 1.0F : (mc.player.input.playerInput.right() ? -1.0F : 0.0F)) != 0.0F;
   }

   public static double[] calculateDirection(double distance) {
      return calculateDirection(RotationController.INSTANCE.getMoveRotation().getYaw(), distance);
   }

   public static double[] calculateDirection(float yaw, double distance) {
      return calculateDirection(
            mc.player.input.playerInput.forward() ? 1.0F : (mc.player.input.playerInput.backward() ? -1.0F : 0.0F),
            mc.player.input.playerInput.left() ? 1.0F : (mc.player.input.playerInput.right() ? -1.0F : 0.0F),
            yaw, distance);
   }

   public static double[] calculateDirection(float forward, float sideways, float yaw, double distance) {
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sinYaw = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      double cosYaw = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double xMovement = (double)forward * distance * cosYaw + (double)sideways * distance * sinYaw;
      double zMovement = (double)forward * distance * sinYaw - (double)sideways * distance * cosYaw;
      return new double[]{xMovement, zMovement};
   }

   public static double getSpeedSqrt(Entity entity) {
      return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.lastX, entity.lastY, entity.lastZ)));
   }

   public static void setVelocity(double velocity) {
      double[] direction = calculateDirection(velocity);
      ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
   }

   public static void setVelocity(double velocity, double y) {
      double[] direction = calculateDirection(velocity);
      ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).setVelocity(direction[0], y, direction[1]);
   }

   public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
      float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double currentYaw = Math.toRadians((double)MathHelper.wrapDegrees(yaw));
      return Math.toDegrees(MathHelper.wrapDegrees((double)optimalYaw - currentYaw));
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
      boolean forwards = input.forward();
      boolean backwards = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (dgs >= (double)(-90.0F + deadAngle) && dgs <= (double)(90.0F - deadAngle)) {
         forwards = true;
      } else if (dgs < (double)(-90.0F - deadAngle) || dgs > (double)(90.0F + deadAngle)) {
         backwards = true;
      }

      if (dgs >= (double)(0.0F + deadAngle) && dgs <= (double)(180.0F - deadAngle)) {
         right = true;
      } else if (dgs >= (double)(-180.0F + deadAngle) && dgs <= (double)(0.0F - deadAngle)) {
         left = true;
      }

      return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
      return getDirectionalInputForDegrees(input, dgs, 20.0F);
   }

   private MovingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
