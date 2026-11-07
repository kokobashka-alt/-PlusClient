package ru.zov.common.util.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;

public class LinearSmoothMode extends AngleSmoothMode {
   public LinearSmoothMode() {
      super("Linear");
   }

   @Native
   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
      );
   }
}
