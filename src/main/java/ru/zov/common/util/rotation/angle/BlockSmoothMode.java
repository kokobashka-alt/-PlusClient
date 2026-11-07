package ru.zov.common.util.rotation.angle;

import java.security.SecureRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;

public class BlockSmoothMode extends AngleSmoothMode {
   public BlockSmoothMode() {
      super("Block");
   }

   @Native
   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      float speed = entity != null ? 1.0F : (new SecureRandom().nextBoolean() ? 0.4F : 0.2F);
      float lineYaw = Math.abs(yawDelta / rotationDifference) * 200.0F;
      float linePitch = Math.abs(pitchDelta / rotationDifference) * 200.0F;
      float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
      float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
      Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
      moveAngle.setYaw(MathHelper.lerp(Math.clamp(MathUtil.getRandom(speed, speed + 0.2F), 0.0F, 1.0F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
      moveAngle.setPitch(
         MathHelper.lerp(Math.clamp(MathUtil.getRandom(speed, speed + 0.2F), 0.0F, 1.0F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch)
      );
      return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
   }
}
