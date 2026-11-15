package ru.zov.implement.features.modules.combat.killaura.neuro;

/**
 * Builds the network input from a {@link RawSample}. This class and tools/bezier_common.py must produce the same
 * numbers for the same sample; {@code FeatureParityTest} checks that on generated data.
 *
 * <p>Scales are fixed constants, not fitted ones, so a sample recorded today means the same thing after a model is
 * retrained. The model file additionally carries its own per-feature mean and scale, applied inside the GRU.
 *
 * <p>State: the previous distance and target velocity, used for closing speed and acceleration. The first sample of
 * a segment has no history; the model gets zeros there and the {@code has_history} flag tells it so.
 */
public final class FeatureVector {
    public static final int COUNT = 13;

    public static final String[] NAMES = {
            "yaw_error",
            "pitch_error",
            "distance",
            "height_delta",
            "target_speed",
            "target_accel",
            "closing_speed",
            "airborne",
            "player_vy",
            "ticks_since_attack",
            "prev_step_yaw",
            "prev_step_pitch",
            "has_history",
    };

    /** Sentinel written into ticks_since_attack when the value was never measured; outside the normal 0..1.6 range. */
    public static final float ATTACK_UNKNOWN_FEATURE = 2.0F;

    private static final float ANGLE_SCALE = 45.0F;
    private static final float DISTANCE_SCALE = 4.0F;
    private static final float HEIGHT_SCALE = 2.0F;
    private static final float TARGET_SPEED_SCALE = 0.3F;   // sprinting is about 0.28 blocks/tick
    private static final float TARGET_ACCEL_SCALE = 0.1F;
    private static final float CLOSING_SCALE = 0.3F;
    private static final float PLAYER_VY_SCALE = 0.4F;
    private static final float ATTACK_SCALE = 25.0F;
    private static final float STEP_SCALE = 20.0F;

    private boolean hasPrev;
    private float prevDistance;
    private float prevTargetVx;
    private float prevTargetVz;

    public void reset() {
        this.hasPrev = false;
        this.prevDistance = 0.0F;
        this.prevTargetVx = 0.0F;
        this.prevTargetVz = 0.0F;
    }

    /**
     * Fills {@code out} with {@link #COUNT} features. Returns false when a required input is not a finite number; in
     * that case the history is left untouched and the caller must fall back for this tick.
     */
    public boolean fill(RawSample s, float[] out) {
        if (out.length < COUNT) {
            throw new IllegalArgumentException("output needs " + COUNT + " slots, got " + out.length);
        }

        float yawError = wrapDegrees(s.targetYaw() - s.curYaw());
        float pitchError = s.targetPitch() - s.curPitch();
        float distance = s.distance();
        float height = s.heightDelta();
        float tvx = s.targetVx();
        float tvz = s.targetVz();
        float pvx = s.playerVx();
        float pvy = s.playerVy();
        float pvz = s.playerVz();
        float prevStepYaw = s.prevStepYaw();
        float prevStepPitch = s.prevStepPitch();

        if (!finite(yawError) || !finite(pitchError) || !finite(distance) || !finite(height)
                || !finite(tvx) || !finite(tvz) || !finite(pvx) || !finite(pvy) || !finite(pvz)
                || !finite(prevStepYaw) || !finite(prevStepPitch)) {
            return false;
        }
        // Negative distance is a broken caller, not a real measurement.
        if (distance < 0.0F) {
            return false;
        }

        float targetSpeed = (float) Math.hypot(tvx, tvz);
        float targetAccel = this.hasPrev ? (float) Math.hypot(tvx - this.prevTargetVx, tvz - this.prevTargetVz) : 0.0F;
        float closing = this.hasPrev ? -(distance - this.prevDistance) : 0.0F;

        // The attack timer is unknown when negative or not finite; that is encoded as 2.0, above the 0..1.6 band.
        float sinceAttack = ATTACK_UNKNOWN_FEATURE;
        if (finite(s.ticksSinceAttack()) && s.ticksSinceAttack() >= 0.0F) {
            sinceAttack = Math.min(s.ticksSinceAttack(), 40.0F) / ATTACK_SCALE;
        }

        out[0] = clamp(yawError / ANGLE_SCALE, -2.0F, 2.0F);
        out[1] = clamp(pitchError / ANGLE_SCALE, -2.0F, 2.0F);
        out[2] = clamp(distance / DISTANCE_SCALE, 0.0F, 6.0F);
        out[3] = clamp(height / HEIGHT_SCALE, -3.0F, 3.0F);
        out[4] = clamp(targetSpeed / TARGET_SPEED_SCALE, 0.0F, 8.0F);
        out[5] = clamp(targetAccel / TARGET_ACCEL_SCALE, 0.0F, 8.0F);
        out[6] = clamp(closing / CLOSING_SCALE, -8.0F, 8.0F);
        out[7] = s.onGround() ? 0.0F : 1.0F;
        out[8] = clamp(pvy / PLAYER_VY_SCALE, -4.0F, 4.0F);
        out[9] = sinceAttack;
        out[10] = clamp(prevStepYaw / STEP_SCALE, -3.0F, 3.0F);
        out[11] = clamp(prevStepPitch / STEP_SCALE, -3.0F, 3.0F);
        out[12] = this.hasPrev ? 1.0F : 0.0F;

        this.prevDistance = distance;
        this.prevTargetVx = tvx;
        this.prevTargetVz = tvz;
        this.hasPrev = true;
        return true;
    }

    private static boolean finite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static float wrapDegrees(float degrees) {
        float r = degrees % 360.0F;
        if (r >= 180.0F) {
            r -= 360.0F;
        }
        if (r < -180.0F) {
            r += 360.0F;
        }
        return r;
    }
}
