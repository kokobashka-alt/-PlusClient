package ru.zov.implement.features.modules.combat.killaura.type;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.math.RotationMath;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class GrimRotationMode extends RotationMode {
    private static final float TARGET_YAW_SPEED = 85.0f;
    private static final float TARGET_PITCH_SPEED = 55.0f;
    private static final float RESET_YAW_SPEED = 110.0f;
    private static final float RESET_PITCH_SPEED = 70.0f;

    public GrimRotationMode() {
        super("SnapRotation");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        Rotation difference = RotationMath.INSTANCE.calculateRotationDifference(target, current);
        float yawDifference = difference.getYaw();
        float pitchDifference = difference.getPitch();

        if (!Float.isFinite(yawDifference) || !Float.isFinite(pitchDifference)) {
            return new Rotation(MathHelper.wrapDegrees(target.getYaw()), MathHelper.clamp(target.getPitch(), -89.0f, 89.0f));
        }

        if (Math.abs(yawDifference) < 0.001f && Math.abs(pitchDifference) < 0.001f) {
            return new Rotation(MathHelper.wrapDegrees(target.getYaw()), MathHelper.clamp(target.getPitch(), -89.0f, 89.0f));
        }

        Aura aura = (Aura) AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
        boolean attackReady = entity != null && aura != null && aura.canAttack(Mc.INSTANCE.getPlayer(), 1);
        boolean resetting = entity == null;

        float yawStep;
        float pitchStep;
        if (attackReady) {
            yawStep = yawDifference;
            pitchStep = pitchDifference;
        } else {
            float progress = resetting ? 0.72f : 0.62f;
            float yawSpeed = resetting ? RESET_YAW_SPEED : TARGET_YAW_SPEED;
            float pitchSpeed = resetting ? RESET_PITCH_SPEED : TARGET_PITCH_SPEED;
            yawStep = MathHelper.clamp(yawDifference, -yawSpeed, yawSpeed) * progress;
            pitchStep = MathHelper.clamp(pitchDifference, -pitchSpeed, pitchSpeed) * progress;

            if (Math.abs(yawDifference) < 1.0f) {
                yawStep = yawDifference;
            }
            if (Math.abs(pitchDifference) < 1.0f) {
                pitchStep = pitchDifference;
            }
        }

        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + yawStep),
                MathHelper.clamp(current.getPitch() + pitchStep, -89.0f, 89.0f)
        );
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.0d, 0.0d, 0.0d);
    }
}
