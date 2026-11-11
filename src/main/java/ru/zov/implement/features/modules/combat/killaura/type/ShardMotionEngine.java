package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.Random;

/**
 * Per-tick yaw/pitch motion generator for the Shard rotation mode. Contains no Minecraft types so it
 * can be exercised without the game.
 *
 * Coordinate system (same as Minecraft): yaw in degrees, 0 = +Z, increasing clockwise seen from
 * above, unwrapped (it may leave -180..180, the engine keeps it on the branch of the caller's yaw);
 * pitch in degrees, -90 = straight up, +90 = straight down. One call to {@link #step} equals one tick.
 *
 * The engine keeps its own ideal state and only compares it with the caller's yaw/pitch to detect
 * external changes (setback, other module). Sensitivity (GCD) quantisation is NOT done here; the
 * client applies it afterwards in {@code Rotation.normalize()}.
 */
public final class ShardMotionEngine {
    public record Input(float fromYaw, float fromPitch, float targetYaw, float targetPitch,
                        int targetId, boolean engaged, boolean attackImminent) {
    }

    public record Output(float yaw, float pitch) {
    }

    private static final float RESYNC_DEGREES = 6.0F;
    private static final int LONG_IDLE_TICKS = 10;
    /** Share of the worst-case acceleration-to-braking swing assumed to happen before braking bites. */
    private static final float LEAD_FACTOR = 0.9F;
    private static final float RELEASE_SPEED_SCALE = 0.6F;
    private static final float ATTENTION_LIMIT = 0.15F;

    /** One rotation axis with jerk-limited velocity tracking. */
    private static final class Axis {
        float pos;
        float vel;
        float acc;

        void reset(float position) {
            pos = position;
            vel = 0.0F;
            acc = 0.0F;
        }

        void advance(float err, float targetVel, float peak, float accLimit, float jerkLimit,
                     float omega, float zeta, boolean freeze) {
            float desired = 0.0F;
            if (!freeze) {
                float a = Math.abs(err);
                float brakeAcc = accLimit * clamp(0.35F + 0.45F * zeta, 0.5F, 0.9F);
                float rampLead = brakeAcc * LEAD_FACTOR * (accLimit + brakeAcc) / jerkLimit;
                float braking = (float) Math.sqrt(rampLead * rampLead + 2.0F * brakeAcc * a) - rampLead;
                float approach = Math.signum(err) * Math.min(omega * a, braking);
                desired = clamp(approach + targetVel, -peak, peak);
            }
            float gain = clamp(zeta * omega * 1.5F, 0.10F, 0.95F);
            float wanted = (desired - vel) * gain;
            acc += clamp(wanted - acc, -jerkLimit, jerkLimit);
            acc = clamp(acc, -accLimit, accLimit);
            vel = clamp(vel + acc, -peak, peak);
            if (Math.abs(err) < 0.01F && Math.abs(vel) < 0.05F) {
                vel *= 0.5F;
                acc = 0.0F;
            }
            pos += vel;
        }
    }

    private final Random rng;
    private final Axis yawAxis = new Axis();
    private final Axis pitchAxis = new Axis();

    private boolean initialized;
    private float tremorYaw;
    private float tremorPitch;
    private float driftYaw;
    private float driftPitch;
    private float attention;
    private float settle;
    private float arcSign = 1.0F;
    private int reactionTicks;
    private int pauseTicks;
    private int ticksSincePause = 100;
    private int idleTicks = 1000;
    private int lastTargetId = Integer.MIN_VALUE;
    private boolean overshootArmed = true;
    private int overshootTicks;
    private float overshootYaw;
    private float overshootPitch;
    private float lastErrorDegrees;
    private float lastTargetYaw;
    private float lastTargetPitch;
    private boolean haveLastTarget;
    private float targetVelYaw;
    private float targetVelPitch;

    public ShardMotionEngine(long seed) {
        this.rng = new Random(seed);
    }

    public void reset() {
        initialized = false;
        yawAxis.reset(0.0F);
        pitchAxis.reset(0.0F);
        tremorYaw = tremorPitch = 0.0F;
        driftYaw = driftPitch = 0.0F;
        settle = 0.0F;
        reactionTicks = 0;
        pauseTicks = 0;
        ticksSincePause = 100;
        idleTicks = 1000;
        lastTargetId = Integer.MIN_VALUE;
        overshootArmed = true;
        overshootTicks = 0;
        overshootYaw = overshootPitch = 0.0F;
        haveLastTarget = false;
        targetVelYaw = targetVelPitch = 0.0F;
    }

    /** Angular distance between the last emitted rotation and the unperturbed target, in degrees. */
    public float lastErrorDegrees() {
        return lastErrorDegrees;
    }

    public Output step(Input in, ShardProfile p) {
        if (!isFinite(in.fromYaw, in.fromPitch, in.targetYaw, in.targetPitch)) {
            return new Output(in.fromYaw, in.fromPitch);
        }
        syncWithCaller(in);
        updateAttention();

        boolean engaged = in.engaged;
        if (engaged) {
            if (in.targetId != lastTargetId || idleTicks > 0) {
                onAcquire(p, in.targetId != lastTargetId, idleTicks > LONG_IDLE_TICKS);
            }
            lastTargetId = in.targetId;
            idleTicks = 0;
        } else {
            idleTicks++;
        }

        float settleGoal = engaged && in.attackImminent ? 1.0F : 0.0F;
        settle += (settleGoal - settle) * (settleGoal > settle ? 0.45F : 0.20F);
        float scale = 1.0F - settle * (1.0F - p.attackTighten());

        updateDrift(p, engaged);
        updateTremor(p);
        updateTargetVelocity(in, p);

        if (engaged && in.attackImminent) {
            pauseTicks = 0;
        }
        float targetYaw = in.targetYaw;
        float targetPitch = in.targetPitch;
        if (engaged) {
            targetYaw += driftYaw * scale + overshootYaw;
            targetPitch += driftPitch * scale + overshootPitch;
        }

        float errYaw = wrap(targetYaw - yawAxis.pos);
        float errPitch = targetPitch + arcSign * p.arcGain() * yawAxis.vel - pitchAxis.pos;

        if (engaged) {
            rollOvershoot(p, errYaw, errPitch);
            rollPause(p, errYaw, errPitch, in.attackImminent);
        }
        tickOvershoot();

        boolean freeze = engaged && (reactionTicks > 0 || pauseTicks > 0);
        if (reactionTicks > 0) {
            reactionTicks--;
        }
        if (pauseTicks > 0) {
            pauseTicks--;
        }
        ticksSincePause++;

        float speedScale = (1.0F + attention) * (engaged ? 1.0F : RELEASE_SPEED_SCALE);
        float yawPeak = p.peakYawSpeed() * speedScale;
        float pitchRatio = p.peakPitchSpeed() / p.peakYawSpeed();
        float pitchPeak = p.peakPitchSpeed() * speedScale;
        float pitchOmega = p.omega() * (0.6F + 0.4F * p.pitchGain());

        yawAxis.advance(errYaw, targetVelYaw * p.pursuitGain(), yawPeak, p.accLimit(), p.jerkLimit(), p.omega(), p.zeta(), freeze);
        pitchAxis.advance(errPitch, targetVelPitch * p.pursuitGain(), pitchPeak, p.accLimit() * pitchRatio, p.jerkLimit() * pitchRatio,
                pitchOmega, p.zeta(), freeze);
        pitchAxis.pos = clamp(pitchAxis.pos, -90.0F, 90.0F);

        float outYaw = yawAxis.pos + tremorYaw * scale;
        float outPitch = clamp(pitchAxis.pos + tremorPitch * scale, -90.0F, 90.0F);
        lastErrorDegrees = (float) Math.hypot(wrap(in.targetYaw - outYaw), in.targetPitch - outPitch);
        if (!isFinite(outYaw, outPitch)) {
            reset();
            return new Output(in.fromYaw, in.fromPitch);
        }
        return new Output(outYaw, outPitch);
    }

    private void syncWithCaller(Input in) {
        float yawGap = wrap(in.fromYaw - yawAxis.pos);
        float pitchGap = in.fromPitch - pitchAxis.pos;
        if (!initialized || Math.abs(yawGap) > RESYNC_DEGREES || Math.abs(pitchGap) > RESYNC_DEGREES) {
            yawAxis.reset(in.fromYaw);
            pitchAxis.reset(in.fromPitch);
            initialized = true;
            return;
        }
        yawAxis.pos = in.fromYaw - yawGap;
    }

    /**
     * Smoothed angular velocity of the target as seen in the caller's target angles. Pitch is clamped
     * and filtered harder because the aim-point selection can hop between height samples.
     */
    private void updateTargetVelocity(Input in, ShardProfile p) {
        if (!in.engaged || !haveLastTarget || reactionTicks > 0) {
            targetVelYaw = 0.0F;
            targetVelPitch = 0.0F;
        } else {
            float yawLimit = p.peakYawSpeed() * 0.5F;
            float pitchLimit = Math.min(1.5F, p.peakPitchSpeed() * 0.5F);
            float rawYaw = clamp(wrap(in.targetYaw - lastTargetYaw), -yawLimit, yawLimit);
            float rawPitch = clamp(in.targetPitch - lastTargetPitch, -pitchLimit, pitchLimit);
            targetVelYaw += (rawYaw - targetVelYaw) * 0.40F;
            targetVelPitch += (rawPitch - targetVelPitch) * 0.25F;
        }
        lastTargetYaw = in.targetYaw;
        lastTargetPitch = in.targetPitch;
        haveLastTarget = in.engaged;
    }

    private void onAcquire(ShardProfile p, boolean newTarget, boolean longIdle) {
        float lo;
        float hi;
        if (longIdle) {
            lo = p.reactionMinTicks();
            hi = p.reactionMaxTicks();
        } else if (newTarget) {
            lo = p.reactionMinTicks() * 0.6F;
            hi = p.reactionMaxTicks() * 0.6F;
        } else {
            lo = 1.0F;
            hi = 2.0F;
        }
        reactionTicks = Math.max(1, Math.round(lo + rng.nextFloat() * Math.max(0.0F, hi - lo)));
        arcSign = rng.nextBoolean() ? 1.0F : -1.0F;
        driftYaw = (float) rng.nextGaussian() * p.driftSigmaYaw();
        driftPitch = (float) rng.nextGaussian() * p.driftSigmaPitch();
        overshootTicks = 0;
        overshootYaw = overshootPitch = 0.0F;
        overshootArmed = true;
        pauseTicks = 0;
    }

    private void updateAttention() {
        attention += -attention * 0.01F + (float) rng.nextGaussian() * 0.006F;
        attention = clamp(attention, -ATTENTION_LIMIT, ATTENTION_LIMIT);
    }

    private void updateDrift(ShardProfile p, boolean engaged) {
        if (!engaged) {
            driftYaw *= 0.8F;
            driftPitch *= 0.8F;
            return;
        }
        float k = p.driftRevert();
        float noise = (float) Math.sqrt(2.0F * k);
        driftYaw += -k * driftYaw + (float) rng.nextGaussian() * p.driftSigmaYaw() * noise;
        driftPitch += -k * driftPitch + (float) rng.nextGaussian() * p.driftSigmaPitch() * noise;
    }

    private void updateTremor(ShardProfile p) {
        float c = p.tremorCorr();
        float s = (float) Math.sqrt(1.0F - c * c) * p.tremorSigma();
        tremorYaw = c * tremorYaw + (float) rng.nextGaussian() * s;
        tremorPitch = c * tremorPitch + (float) rng.nextGaussian() * s * 0.7F;
    }

    private void rollOvershoot(ShardProfile p, float errYaw, float errPitch) {
        float distance = Math.abs(errYaw);
        if (distance < p.overshootMinDistance() * 0.4F) {
            overshootArmed = true;
        }
        if (reactionTicks > 0 || overshootTicks > 0 || !overshootArmed
                || distance < p.overshootMinDistance()) {
            return;
        }
        overshootArmed = false;
        if (rng.nextFloat() >= p.overshootChance()) {
            return;
        }
        float depth = p.overshootFraction() * (0.6F + 0.8F * rng.nextFloat());
        overshootYaw = Math.signum(errYaw) * distance * depth;
        overshootPitch = Math.signum(errPitch) * Math.abs(errPitch) * depth * 0.5F;
        overshootTicks = 6 + rng.nextInt(5);
    }

    private void tickOvershoot() {
        if (overshootTicks <= 0) {
            return;
        }
        overshootTicks--;
        if (overshootTicks <= 3) {
            overshootYaw *= 0.55F;
            overshootPitch *= 0.55F;
        }
        if (overshootTicks == 0) {
            overshootYaw = overshootPitch = 0.0F;
        }
    }

    private void rollPause(ShardProfile p, float errYaw, float errPitch, boolean attackImminent) {
        if (pauseTicks > 0 || reactionTicks > 0 || attackImminent || ticksSincePause < 8) {
            return;
        }
        if (Math.hypot(errYaw, errPitch) > 2.5 || rng.nextFloat() >= p.pauseChance()) {
            return;
        }
        float span = p.pauseMaxTicks() - p.pauseMinTicks();
        pauseTicks = Math.max(1, Math.round(p.pauseMinTicks() + rng.nextFloat() * span));
        ticksSincePause = 0;
    }

    static float wrap(float degrees) {
        float d = degrees % 360.0F;
        if (d >= 180.0F) {
            d -= 360.0F;
        } else if (d < -180.0F) {
            d += 360.0F;
        }
        return d;
    }

    private static boolean isFinite(float... values) {
        for (float v : values) {
            if (!Float.isFinite(v)) {
                return false;
            }
        }
        return true;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
