package ru.zov.implement.features.modules.combat.killaura.type;

import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.math.RotationMath;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Deterministic, stateful rotation with bounded velocity, acceleration and jerk.
 *
 * The controller accelerates from rest, starts braking early enough to approach
 * the requested angle smoothly and keeps sub-GCD movement as a residual instead
 * of losing it during mouse-sensitivity normalization.
 */
public final class SpookyTimeRotationMode extends RotationMode {
    private static final MotionProfile TRACKING_PROFILE = new MotionProfile(
            72.0F,
            46.0F,
            14.0F,
            9.0F,
            6.0F,
            4.0F,
            0.70F,
            0.72F,
            0.65F
    );
    private static final MotionProfile RETURN_PROFILE = new MotionProfile(
            100.0F,
            70.0F,
            24.0F,
            16.0F,
            10.0F,
            7.0F,
            0.95F,
            0.85F,
            0.85F
    );
    private static final float FINISH_EPSILON = 0.05F;
    private static final float TARGET_DISCONTINUITY_ANGLE = 45.0F;
    private static final float CURRENT_DISCONTINUITY_ANGLE = 5.0F;
    private static final int MAX_BRAKING_SIMULATION_TICKS = 80;
    private static final float NORMALIZATION_GUARD = 1.001F;

    private int trackedEntityId = Integer.MIN_VALUE;
    private boolean returningToPlayerRotation;
    private boolean trackingInitialized;
    private float yawVelocity;
    private float pitchVelocity;
    private float yawAcceleration;
    private float pitchAcceleration;
    private float yawResidual;
    private float pitchResidual;
    private Rotation lastTarget;
    private Rotation lastOutput;

    public SpookyTimeRotationMode() {
        super("SpookyTime");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d targetVector, Entity entity) {
        if (!isValid(current)) {
            reset();
            current = isValid(target) ? sanitize(target, Rotation.ZERO) : Rotation.ZERO;
        }
        if (!isValid(target)) {
            Rotation safeCurrent = sanitize(current, Rotation.ZERO);
            reset();
            return safeCurrent;
        }

        int entityId = entity == null ? Integer.MIN_VALUE : entity.getId();
        boolean returning = entity == null;
        boolean contextChanged = !this.trackingInitialized
                || entityId != this.trackedEntityId
                || returning != this.returningToPlayerRotation;
        boolean targetDiscontinuity = this.lastTarget != null
                && angularDistance(this.lastTarget, target) > TARGET_DISCONTINUITY_ANGLE;
        boolean currentDiscontinuity = this.lastOutput != null
                && angularDistance(this.lastOutput, current) > CURRENT_DISCONTINUITY_ANGLE;
        if (contextChanged || targetDiscontinuity || currentDiscontinuity) {
            resetKinematics();
            this.trackedEntityId = entityId;
            this.returningToPlayerRotation = returning;
            this.trackingInitialized = true;
        }
        this.lastTarget = sanitize(target, current);

        Rotation difference = RotationMath.INSTANCE.calculateRotationDifference(target, current);
        float yawError = difference.getYaw();
        float pitchError = difference.getPitch();
        if (!Float.isFinite(yawError) || !Float.isFinite(pitchError)) {
            Rotation safeCurrent = sanitize(current, Rotation.ZERO);
            reset();
            return safeCurrent;
        }

        float mouseGcd = safeMouseGcd();
        float tolerance = Math.max(FINISH_EPSILON, mouseGcd * 0.5F);
        if (Math.abs(yawError) <= tolerance && Math.abs(pitchError) <= tolerance) {
            resetKinematics();
            this.lastOutput = sanitize(current, Rotation.ZERO);
            return this.lastOutput;
        }

        MotionProfile profile = returning ? RETURN_PROFILE : TRACKING_PROFILE;
        AxisMotion yawMotion = advanceAxis(
                yawError,
                this.yawVelocity,
                this.yawAcceleration,
                this.yawResidual,
                profile.maxYawVelocity(),
                profile.maxYawAcceleration(),
                profile.maxYawJerk(),
                profile.positionGain(),
                profile.velocityGain(),
                profile.maxApproachRatio(),
                mouseGcd,
                tolerance
        );
        AxisMotion pitchMotion = advanceAxis(
                pitchError,
                this.pitchVelocity,
                this.pitchAcceleration,
                this.pitchResidual,
                profile.maxPitchVelocity(),
                profile.maxPitchAcceleration(),
                profile.maxPitchJerk(),
                profile.positionGain(),
                profile.velocityGain(),
                profile.maxApproachRatio(),
                mouseGcd,
                tolerance
        );

        this.yawVelocity = yawMotion.velocity();
        this.pitchVelocity = pitchMotion.velocity();
        this.yawAcceleration = yawMotion.acceleration();
        this.pitchAcceleration = pitchMotion.acceleration();
        this.yawResidual = yawMotion.residual();
        this.pitchResidual = pitchMotion.residual();

        Rotation output = new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + yawMotion.step()),
                MathHelper.clamp(current.getPitch() + pitchMotion.step(), -89.0F, 89.0F)
        );
        this.lastOutput = sanitize(output, current);
        return this.lastOutput;
    }

    public void reset() {
        this.trackedEntityId = Integer.MIN_VALUE;
        this.returningToPlayerRotation = false;
        this.trackingInitialized = false;
        this.lastTarget = null;
        this.lastOutput = null;
        resetKinematics();
    }

    private void resetKinematics() {
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
        this.yawAcceleration = 0.0F;
        this.pitchAcceleration = 0.0F;
        this.yawResidual = 0.0F;
        this.pitchResidual = 0.0F;
    }

    private static AxisMotion advanceAxis(
            float error,
            float velocity,
            float acceleration,
            float residual,
            float maxVelocity,
            float maxAcceleration,
            float maxJerk,
            float positionGain,
            float velocityGain,
            float maxApproachRatio,
            float mouseGcd,
            float tolerance
    ) {
        float absoluteError = Math.abs(error);
        if (!Float.isFinite(absoluteError) || absoluteError <= tolerance) {
            return AxisMotion.ZERO;
        }

        float direction = Math.signum(error);
        if (!Float.isFinite(velocity) || velocity * direction < 0.0F) {
            velocity = 0.0F;
            acceleration = 0.0F;
        }
        if (!Float.isFinite(acceleration)) {
            acceleration = 0.0F;
        }
        if (!Float.isFinite(residual) || residual * direction < 0.0F) {
            residual = 0.0F;
        }

        float brakingDistance = simulateBrakingDistance(
                velocity * direction,
                acceleration * direction,
                maxAcceleration,
                maxJerk
        );
        float availableDistance = Math.max(0.0F, absoluteError - brakingDistance - tolerance);
        float desiredVelocity = direction * Math.min(maxVelocity, availableDistance * positionGain);
        float desiredAcceleration = MathHelper.clamp(
                (desiredVelocity - velocity) * velocityGain,
                -maxAcceleration,
                maxAcceleration
        );
        float nextAcceleration = moveTowards(acceleration, desiredAcceleration, maxJerk);
        float nextVelocity = MathHelper.clamp(velocity + nextAcceleration, -maxVelocity, maxVelocity);

        float continuousStep;
        if (!Float.isFinite(nextVelocity) || nextVelocity * direction <= 0.0F) {
            continuousStep = 0.0F;
        } else {
            continuousStep = direction * Math.min(
                    Math.abs(nextVelocity),
                    absoluteError * maxApproachRatio
            );
        }

        float nextResidual = residual + continuousStep;
        int availableQuanta = (int) (Math.abs(nextResidual) / mouseGcd);
        float quantizedStep = 0.0F;
        if (availableQuanta > 0) {
            int usefulQuanta = Math.max(1, (int) Math.floor((absoluteError + tolerance) / mouseGcd));
            int emittedQuanta = Math.min(availableQuanta, usefulQuanta);
            float emitted = Math.copySign(emittedQuanta * mouseGcd, nextResidual);
            nextResidual -= emitted;
            quantizedStep = emitted + Math.copySign(mouseGcd * (NORMALIZATION_GUARD - 1.0F), emitted);
        }

        return new AxisMotion(nextVelocity, nextAcceleration, nextResidual, quantizedStep);
    }

    private static float simulateBrakingDistance(
            float velocity,
            float acceleration,
            float maxAcceleration,
            float maxJerk
    ) {
        if (!Float.isFinite(velocity) || velocity <= 0.0F) {
            return 0.0F;
        }
        float simulatedVelocity = velocity;
        float simulatedAcceleration = Float.isFinite(acceleration) ? acceleration : 0.0F;
        float distance = 0.0F;
        for (int tick = 0; tick < MAX_BRAKING_SIMULATION_TICKS && simulatedVelocity > 0.0F; tick++) {
            simulatedAcceleration = moveTowards(simulatedAcceleration, -maxAcceleration, maxJerk);
            simulatedVelocity = Math.max(0.0F, simulatedVelocity + simulatedAcceleration);
            distance += simulatedVelocity;
            if (!Float.isFinite(distance)) {
                return Float.MAX_VALUE;
            }
        }
        return distance;
    }

    private static float safeMouseGcd() {
        try {
            double gcd = MathUtil.computeGcd();
            if (Double.isFinite(gcd) && gcd > 0.0D) {
                return (float) gcd;
            }
        } catch (RuntimeException ignored) {
        }
        return FINISH_EPSILON;
    }

    private static float moveTowards(float current, float target, float maximumDelta) {
        if (current < target) {
            return Math.min(current + maximumDelta, target);
        }
        return Math.max(current - maximumDelta, target);
    }

    private static float angularDistance(Rotation first, Rotation second) {
        Rotation difference = RotationMath.INSTANCE.calculateRotationDifference(second, first);
        return (float) Math.hypot(difference.getYaw(), difference.getPitch());
    }

    private static boolean isValid(Rotation rotation) {
        return rotation != null
                && Float.isFinite(rotation.getYaw())
                && Float.isFinite(rotation.getPitch());
    }

    private static Rotation sanitize(Rotation rotation, Rotation fallback) {
        if (!isValid(rotation)) {
            return fallback;
        }
        return new Rotation(
                MathHelper.wrapDegrees(rotation.getYaw()),
                MathHelper.clamp(rotation.getPitch(), -89.0F, 89.0F)
        );
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private record MotionProfile(
            float maxYawVelocity,
            float maxPitchVelocity,
            float maxYawAcceleration,
            float maxPitchAcceleration,
            float maxYawJerk,
            float maxPitchJerk,
            float positionGain,
            float velocityGain,
            float maxApproachRatio
    ) {
    }

    private record AxisMotion(float velocity, float acceleration, float residual, float step) {
        private static final AxisMotion ZERO = new AxisMotion(0.0F, 0.0F, 0.0F, 0.0F);
    }
}
