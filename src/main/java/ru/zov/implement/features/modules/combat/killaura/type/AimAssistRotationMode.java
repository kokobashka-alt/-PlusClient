package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * Aim assist built on top of the existing Bezier rotation. It keeps the model/fallback speed character, but puts a
 * human motion profile in front of the mouse grid:
 *
 * <ul>
 *   <li>reaction delay after a new or re-acquired target;</li>
 *   <li>acceleration from standstill and deceleration near the target (Fitts law + stopping distance);</li>
 *   <li>a hard "do not cross the target in one tick" clamp, so it cannot snap or oscillate;</li>
 *   <li>tiny tremor while moving and rare one-tick pauses;</li>
 *   <li>an FOV gate: it only smooths an aim that is already close to the crosshair.</li>
 * </ul>
 *
 * <p>Direction is always the shortest path to the target; the wrapped rotation decides how fast the step is allowed
 * to be. That is deliberate: a random or under-trained model must not drag the crosshair away while assist is on.
 * Recording/training and model reloading are inherited unchanged.
 *
 * <p>This is a movement shaper, not an anticheat bypass. Validate it on the server you actually play.
 */
public class AimAssistRotationMode extends BezierRotationMode {
    /** All speeds are degrees per tick (20 ticks per second), accelerations are degrees per tick squared. */
    public static final class Config {
        public float maxYawSpeed = 14.0F;
        public float maxPitchSpeed = 10.0F;
        public float yawAcceleration = 1.8F;
        public float pitchAcceleration = 1.4F;
        public float yawDeceleration = 3.6F;
        public float pitchDeceleration = 2.8F;

        /** Desired speed grows with the remaining error: at 10 deg this gives about 4.5 deg/tick. */
        public float fittsGain = 0.45F;
        /** Smallest speed the profile is willing to generate while far from the target. */
        public float minApproachSpeed = 0.35F;
        /** Outside this angle from the crosshair the assist does not touch the rotation. */
        public float maxFovDegrees = 45.0F;

        public float stopErrorDegrees = 0.06F;
        public float stopSpeed = 0.04F;

        public float jitterBaseDegrees = 0.025F;
        public float jitterSpeedDegrees = 0.05F;
        public float pauseChance = 0.004F;

        public int reactionMinTicks = 1;
        public int reactionMaxTicks = 3;
        public long seed = 20261115L;

        private void validate() {
            this.maxYawSpeed = clamp(this.maxYawSpeed, 0.5F, 90.0F);
            this.maxPitchSpeed = clamp(this.maxPitchSpeed, 0.5F, 90.0F);
            this.yawAcceleration = clamp(this.yawAcceleration, 0.05F, 30.0F);
            this.pitchAcceleration = clamp(this.pitchAcceleration, 0.05F, 30.0F);
            this.yawDeceleration = clamp(this.yawDeceleration, 0.05F, 30.0F);
            this.pitchDeceleration = clamp(this.pitchDeceleration, 0.05F, 30.0F);
            this.fittsGain = clamp(this.fittsGain, 0.01F, 5.0F);
            this.minApproachSpeed = clamp(this.minApproachSpeed, 0.0F, 5.0F);
            this.maxFovDegrees = clamp(this.maxFovDegrees, 1.0F, 180.0F);
            this.stopErrorDegrees = clamp(this.stopErrorDegrees, 0.0F, 5.0F);
            this.stopSpeed = clamp(this.stopSpeed, 0.0F, 5.0F);
            this.jitterBaseDegrees = clamp(this.jitterBaseDegrees, 0.0F, 2.0F);
            this.jitterSpeedDegrees = clamp(this.jitterSpeedDegrees, 0.0F, 2.0F);
            this.pauseChance = clamp(this.pauseChance, 0.0F, 1.0F);
            this.reactionMinTicks = Math.max(0, this.reactionMinTicks);
            this.reactionMaxTicks = Math.max(this.reactionMinTicks, this.reactionMaxTicks);
        }

        private static float clamp(float value, float min, float max) {
            return value < min ? min : (value > max ? max : value);
        }
    }

    private final Config config;
    private final Random random;

    private float yawSpeed;
    private float pitchSpeed;
    private Entity lastTarget;
    private int reactionTicks;

    public AimAssistRotationMode() {
        this(new Config());
    }

    public AimAssistRotationMode(Config config) {
        super("AimAssist");
        this.config = config == null ? new Config() : config;
        this.config.validate();
        this.random = new Random(this.config.seed);
    }

    /** Live config; changes are picked up on the next tick. */
    public Config config() {
        return this.config;
    }

    /**
     * Forget acceleration/reaction state. Call it when the module is disabled or the target is lost, so the next
     * acquisition starts with the same reaction delay as a fresh target.
     */
    public void resetMotion() {
        this.resetProfile();
    }

    @Override
    protected void shapeStep(Rotation current, Rotation target, Entity entity, float[] step) {
        this.config.validate();
        float rawYaw = step[0];
        float rawPitch = step[1];

        // Return-to-camera is not an assist target; leave the fallback exactly as it was.
        if (entity == null) {
            this.resetProfile();
            return;
        }

        if (entity != this.lastTarget) {
            this.lastTarget = entity;
            this.yawSpeed = 0.0F;
            this.pitchSpeed = 0.0F;
            int min = this.config.reactionMinTicks;
            int max = this.config.reactionMaxTicks;
            this.reactionTicks = max > min ? min + this.random.nextInt(max - min + 1) : min;
        }

        float yawError = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchError = target.getPitch() - current.getPitch();

        // A flick across the screen must stay a flick: assist only smooths aim that is already near.
        if (Math.abs(yawError) > this.config.maxFovDegrees
                || Math.abs(pitchError) > this.config.maxFovDegrees) {
            this.resetProfile();
            step[0] = 0.0F;
            step[1] = 0.0F;
            return;
        }

        // Humans do not start moving on the same tick they acquire a target.
        if (this.reactionTicks > 0) {
            this.reactionTicks--;
            step[0] = 0.0F;
            step[1] = 0.0F;
            return;
        }

        this.yawSpeed = this.advance(this.yawSpeed, yawError, rawYaw,
                this.config.maxYawSpeed, this.config.yawAcceleration, this.config.yawDeceleration,
                this.config.fittsGain, this.config.minApproachSpeed,
                this.config.stopErrorDegrees, this.config.stopSpeed);
        this.pitchSpeed = this.advance(this.pitchSpeed, pitchError, rawPitch,
                this.config.maxPitchSpeed, this.config.pitchAcceleration, this.config.pitchDeceleration,
                this.config.fittsGain, this.config.minApproachSpeed,
                this.config.stopErrorDegrees, this.config.stopSpeed);

        float outYaw = this.yawSpeed;
        float outPitch = this.pitchSpeed;
        boolean moving = Math.abs(outYaw) > this.config.stopSpeed || Math.abs(outPitch) > this.config.stopSpeed;
        if (moving) {
            // Tremor scales with speed: almost nothing during a slow correction, a little more during a flick.
            float yawSigma = this.config.jitterBaseDegrees + this.config.jitterSpeedDegrees
                    * Math.min(1.0F, Math.abs(this.yawSpeed) / this.config.maxYawSpeed);
            float pitchSigma = this.config.jitterBaseDegrees + this.config.jitterSpeedDegrees
                    * Math.min(1.0F, Math.abs(this.pitchSpeed) / this.config.maxPitchSpeed);
            outYaw += (float) this.random.nextGaussian() * yawSigma;
            outPitch += (float) this.random.nextGaussian() * pitchSigma;
        }

        // Tremor must not fight the approach, and no single tick may cross the target.
        if (outYaw != 0.0F && yawError != 0.0F && Math.signum(outYaw) != Math.signum(yawError)) {
            outYaw = 0.0F;
        }
        if (outPitch != 0.0F && pitchError != 0.0F && Math.signum(outPitch) != Math.signum(pitchError)) {
            outPitch = 0.0F;
        }
        outYaw = limitToRemaining(outYaw, yawError);
        outPitch = limitToRemaining(outPitch, pitchError);
        outYaw = MathHelper.clamp(outYaw, -this.config.maxYawSpeed, this.config.maxYawSpeed);
        outPitch = MathHelper.clamp(outPitch, -this.config.maxPitchSpeed, this.config.maxPitchSpeed);

        // A short hold, mostly during slow corrections. It is rare on purpose: at 20 tps even 1% is a pause every
        // five seconds.
        if (this.config.pauseChance > 0.0F
                && Math.abs(this.yawSpeed) < this.config.maxYawSpeed * 0.6F
                && Math.abs(this.pitchSpeed) < this.config.maxPitchSpeed * 0.6F
                && this.random.nextFloat() < this.config.pauseChance) {
            this.yawSpeed = 0.0F;
            this.pitchSpeed = 0.0F;
            outYaw = 0.0F;
            outPitch = 0.0F;
        }

        step[0] = outYaw;
        step[1] = outPitch;
    }

    /**
     * One axis of the motion profile. Returns the new velocity, in degrees per tick.
     */
    private float advance(float speed, float error, float rawStep, float maxSpeed, float acceleration,
                          float deceleration, float fittsGain, float minApproach, float stopError, float stopSpeed) {
        float absError = Math.abs(error);
        if (absError <= stopError) {
            return moveToward(speed, 0.0F, deceleration);
        }

        // Fitts law, plus the largest speed from which we can still stop in the remaining error over discrete ticks:
        // v + v^2 / (2a) <= error, so v <= sqrt(a^2 + 2a*error) - a. The smallest cap wins.
        float cap = Math.min(maxSpeed, fittsGain * absError + minApproach);
        float discreteStop = (float) (Math.sqrt(deceleration * deceleration + 2.0 * deceleration * absError)
                - deceleration);
        cap = Math.min(cap, discreteStop);
        cap = Math.min(cap, absError * 0.8F);
        cap = Math.max(cap, 0.0F);

        float rawSpeed = Float.isFinite(rawStep) ? Math.abs(rawStep) : 0.0F;
        // Use the wrapped rotation for the speed character. The small floor keeps an untrained/random model from
        // freezing the assist; the cap still makes the approach decelerate like a hand.
        float desiredSpeed = Math.min(cap, Math.max(rawSpeed, fittsGain * absError * 0.4F));
        if (absError <= stopError * 4.0F && desiredSpeed < stopSpeed) {
            desiredSpeed = 0.0F;
        }
        float desired = Math.copySign(desiredSpeed, error);

        if (Math.abs(speed) < 1.0E-5F && desiredSpeed < 1.0E-5F) {
            return 0.0F;
        }

        boolean speedingUp = Math.abs(desired) > Math.abs(speed)
                && (Math.abs(speed) < 1.0E-5F || Math.signum(desired) == Math.signum(speed));
        float limit = speedingUp ? acceleration : deceleration;
        float next = speed + MathHelper.clamp(desired - speed, -limit, limit);

        // If the target crossed us while we were still carrying speed, do not spend a tick moving further away.
        if (next * error < 0.0F) {
            return 0.0F;
        }

        // If the target moved closer since the previous tick, brake hard enough not to step past it. This is the
        // only non-acceleration-limited clamp, and it is at most a fraction of a degree.
        float hardCap = absError * 0.9F;
        if (Math.abs(next) > hardCap) {
            next = Math.copySign(Math.max(hardCap, 0.0F), next);
        }
        return next;
    }

    private void resetProfile() {
        this.yawSpeed = 0.0F;
        this.pitchSpeed = 0.0F;
        this.reactionTicks = 0;
        this.lastTarget = null;
    }

    private static float limitToRemaining(float step, float error) {
        float absError = Math.abs(error);
        if (absError < 1.0E-4F) {
            return 0.0F;
        }
        float limit = absError * 0.8F;
        return Math.abs(step) > limit ? Math.copySign(limit, step) : step;
    }

    private static float moveToward(float value, float target, float maxDelta) {
        float delta = target - value;
        return Math.abs(delta) <= maxDelta ? target : value + Math.copySign(maxDelta, delta);
    }
}
