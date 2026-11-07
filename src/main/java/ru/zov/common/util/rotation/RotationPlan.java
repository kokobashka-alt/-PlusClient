package ru.zov.common.util.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.zov.common.QuickImports;
import ru.zov.common.util.rotation.angle.AngleSmoothMode;

public record RotationPlan(
   Angle angle,
   Vec3d vec3d,
   Entity entity,
   AngleSmoothMode angleSmooth,
   int ticksUntilReset,
   float resetThreshold,
   boolean moveCorrection,
   boolean freeCorrection
) implements QuickImports {
   public Angle nextRotation(Angle fromAngle, boolean isResetting) {
      return isResetting
         ? this.angleSmooth.limitAngleChange(fromAngle, AngleUtil.fromVec2f(mc.player.getRotationClient()))
         : this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
   }
}
