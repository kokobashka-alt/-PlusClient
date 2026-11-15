package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.util.Random;

/**
 * Fallback aim used when no model is loaded: a damped, slightly jittery pursuit. It is not a replacement for the
 * neural path, only something sane to do while the model is missing or a tick is unusable.
 *
 * <p>Gains are uncalibrated. They are chosen so the view converges in roughly six to ten ticks at 60 degrees of
 * error, which looks human enough for testing; tune them on your own sensitivity.
 */
public final class HumanStep {
    private static final float YAW_GAIN = 0.38F;
    private static final float PITCH_GAIN = 0.42F;
    private static final float YAW_JITTER = 0.10F;
    private static final float PITCH_JITTER = 0.06F;
    private static final float MAX_STEP = 25.0F;

    private final Random rng = new Random();
    private int pause;

    public void reset() {
        this.pause = 0;
    }

    /** Writes the desired yaw and pitch step in degrees into {@code out}. */
    public void desired(float curYaw, float curPitch, float targetYaw, float targetPitch, float[] out) {
        float yawError = FeatureVector.wrapDegrees(targetYaw - curYaw);
        float pitchError = targetPitch - curPitch;

        float dy;
        if (this.pause > 0) {
            this.pause--;
            dy = 0.0F;
        } else {
            dy = yawError * YAW_GAIN;
            // A human now and then holds the mouse still for a tick; keeps the fallback from looking like a lerp.
            if (Math.abs(yawError) > 2.0F && this.rng.nextFloat() < 0.06F) {
                this.pause = 1 + this.rng.nextInt(2);
                dy = 0.0F;
            }
        }

        float dp = pitchError * PITCH_GAIN;
        out[0] = clamp(dy + (float) this.rng.nextGaussian() * YAW_JITTER, -MAX_STEP, MAX_STEP);
        out[1] = clamp(dp + (float) this.rng.nextGaussian() * PITCH_JITTER, -MAX_STEP, MAX_STEP);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
