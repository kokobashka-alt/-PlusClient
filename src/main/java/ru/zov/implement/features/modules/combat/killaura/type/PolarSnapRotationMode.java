package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.Random;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;

/**
 * Polar-aware snap rotation.
 *
 * Design notes vs. Polar:
 *  - Every tunable is re-rolled per tick, not per hit / per target. Polar's cloud ML
 *    looks at tick-to-tick sequences, so class-level constants that persist across
 *    ticks are the main source of "static/consistent patterns" flags.
 *  - Snap step decays non-linearly with remaining error and is multiplied by a
 *    per-tick uniform noise, so the angular velocity profile is never a clean ramp.
 *  - Lock phase intentionally maintains a non-zero floor error (MIN_LOCK_ERR). A
 *    crosshair that converges to within one GCD unit is trivially detected.
 *  - Pause probability itself is jittered per tick (0.01 .. 0.09), so the
 *    distribution of skipped ticks has no fixed mean.
 *  - Residual carry uses a bias of 1e-4 * step — just enough to survive the
 *    framework's truncation, too small to accumulate into a detectable drift.
 */
public final class PolarSnapRotationMode extends RotationMode {
    private static final float PITCH_LIMIT   = 89.0F;
    private static final float LOCK_RADIUS   = 1.3F;   // tighter than before; snap dominates
    private static final float MIN_LOCK_ERR  = 0.025F; // never let error collapse to 0
    private static final float STEP_NOISE    = 0.07F;  // +-7 % on the per-tick step
    private static final float WOBBLE        = 0.012F; // perpendicular, degrees
    private static final float QUANT_BIAS    = 1.0E-4F;

    private final Random rng = new Random();
    private final float[] residual = new float[2];
    private Entity tracked;
    private int lastHitCounter = Integer.MIN_VALUE;

    // Rolling state used only to decorrelate from the previous tick — not as a fixed parameter.
    private float lastStepMag;

    public PolarSnapRotationMode() {
        super("Polar Snap");
    }

    public void reset() {
        this.tracked = null;
        this.lastHitCounter = Integer.MIN_VALUE;
        this.residual[0] = 0.0F;
        this.residual[1] = 0.0F;
        this.lastStepMag = 0.0F;
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        double gcd = MathUtil.computeGcd();
        if (player == null || current == null || target == null || !(gcd > 1.0E-4)) {
            return current;
        }

        if (this.tracked != entity) {
            this.tracked = entity;
            this.residual[0] = 0.0F;
            this.residual[1] = 0.0F;
            this.lastStepMag = 0.0F;
        }
        this.checkHit();

        float clampedPitch = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT);
        float errYaw   = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float errPitch = clampedPitch - current.getPitch();
        float dist     = (float) Math.hypot(errYaw, errPitch);

        if (dist < 1.0E-3F) {
            return current;
        }

        // ---- Per-tick randomization block ----
        float snapCap    = 18.0F + this.rng.nextFloat() * 30.0F;       // 18 .. 48
        float pauseChance = 0.01F + this.rng.nextFloat() * 0.08F;      // 1 % .. 9 %, re-rolled
        float lockGain   = 0.22F + this.rng.nextFloat() * 0.30F;       // 0.22 .. 0.52
        float lockJitter = 0.018F + this.rng.nextFloat() * 0.030F;     // 0.018 .. 0.048

        // Human pause — the *probability itself* changes every tick.
        if (dist > LOCK_RADIUS && this.rng.nextFloat() < pauseChance) {
            return current;
        }

        float dy;
        float dp;
        if (dist > LOCK_RADIUS) {
            // --- Snap: non-linear decay + per-tick magnitude noise ---
            float step;
            if (dist <= snapCap) {
                // Arrival: 0.90 .. 1.08 of remaining error. Never a clean landing.
                step = dist * (0.90F + this.rng.nextFloat() * 0.18F);
            } else {
                // Mid-snap: base cap pulled toward the previous step to avoid a fixed ceiling,
                // then multiplied by +-STEP_NOISE.
                float base = snapCap * 0.65F + this.lastStepMag * 0.35F;
                float noisy = base * (1.0F - STEP_NOISE + this.rng.nextFloat() * 2.0F * STEP_NOISE);
                step = Math.min(noisy, dist);
            }
            step = Math.max(step, 0.5F); // don't produce sub-visible micro-steps mid-snap
            this.lastStepMag = step;

            float inv = step / dist;
            float wobble = (this.rng.nextFloat() * 2.0F - 1.0F) * WOBBLE;
            dy = errYaw   * inv + wobble;
            dp = errPitch * inv - wobble;
        } else {
            // --- Lock: proportional, but with a non-zero error floor ---
            float signY = Math.signum(errYaw);
            float signP = Math.signum(errPitch);
            float desiredY = errYaw * lockGain;
            float desiredP = errPitch * lockGain;

            // If the proportional step would take us inside MIN_LOCK_ERR, clamp it
            // so the residual error stays non-zero.
            if (Math.abs(errYaw - desiredY) < MIN_LOCK_ERR) {
                desiredY = errYaw - signY * MIN_LOCK_ERR;
            }
            if (Math.abs(errPitch - desiredP) < MIN_LOCK_ERR) {
                desiredP = errPitch - signP * MIN_LOCK_ERR;
            }

            dy = desiredY + (this.rng.nextFloat() * 2.0F - 1.0F) * lockJitter;
            dp = desiredP + (this.rng.nextFloat() * 2.0F - 1.0F) * lockJitter * 0.6F;
        }

        float qy = this.quantize(0, dy, gcd);
        float qp = this.quantize(1, dp, gcd);
        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + qy),
                MathHelper.clamp(current.getPitch() + qp, -PITCH_LIMIT, PITCH_LIMIT));
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private void checkHit() {
        Aura aura = (Aura) AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
        if (aura == null) {
            return;
        }
        int counter = aura.hitCounter();
        // Hit counter no longer re-rolls snapCap — per-tick randomization already covers it.
        // We only track it so a future Polar-specific tweak (e.g. post-hit flinch) has a hook.
        this.lastHitCounter = counter;
    }

    private float quantize(int axis, float desired, double step) {
        double pending = desired + this.residual[axis];
        long counts = Math.round(pending / step);
        double exact = counts * step;
        this.residual[axis] = (float) (pending - exact);
        if (counts == 0) {
            return 0.0F;
        }
        return (float) (exact + Math.signum(exact) * step * QUANT_BIAS);
    }
}
