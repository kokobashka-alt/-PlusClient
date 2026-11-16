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
 * Polar Snap: discrete flick towards the target along the polar error vector, followed by a loose
 * proportional lock. Each tick the view moves at most snapCap degrees (re-rolled per hit) in one
 * angular step. The arrival step deliberately undershoots or slightly overshoots so the crosshair
 * never lands exactly on the target — real mouse input never does.
 *
 * Anti-flag:
 *  - Step is quantized to whole mouse counts with an honest residual carry (no 1% bias).
 *  - Jitter is uniform, not Gaussian (mouse noise is not Gaussian).
 *  - Step magnitude varies +-4% per tick so the angular velocity profile is not a constant ramp.
 *  - Occasional skipped ticks mimic human pause between flicks.
 *  - A tiny perpendicular wobble keeps the path from being a perfectly straight line.
 *  - Lock gain is re-jittered every tick.
 */
public final class PolarSnapRotationMode extends RotationMode {
    private static final float PITCH_LIMIT  = 89.0F;
    private static final float LOCK_RADIUS  = 1.4F;
    private static final float LOCK_GAIN    = 0.35F;
    private static final float LOCK_JITTER  = 0.035F;
    private static final float SNAP_MIN     = 20.0F;
    private static final float SNAP_MAX     = 42.0F;
    private static final float STEP_VARIANCE = 0.04F;  // +-4% per-tick step noise
    private static final float PAUSE_CHANCE  = 0.05F;  // ~1 in 20 ticks skipped
    private static final float WOBBLE        = 0.015F;
    private static final float QUANT_BIAS    = 1.0E-4F; // tiny, survives framework truncation

    private final Random rng = new Random();
    private final float[] residual = new float[2];
    private Entity tracked;
    private int lastHitCounter = Integer.MIN_VALUE;
    private float snapCap = SNAP_MAX;

    public PolarSnapRotationMode() {
        super("Polar Snap");
    }

    public void reset() {
        this.tracked = null;
        this.lastHitCounter = Integer.MIN_VALUE;
        this.snapCap = rollSnap();
        this.residual[0] = 0.0F;
        this.residual[1] = 0.0F;
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
            this.snapCap = rollSnap();
            this.residual[0] = 0.0F;
            this.residual[1] = 0.0F;
        }
        this.checkHit();

        float clampedPitch = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT);
        float errYaw   = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float errPitch = clampedPitch - current.getPitch();
        float dist     = (float) Math.hypot(errYaw, errPitch);

        if (dist < 1.0E-3F) {
            return current;
        }

        // Human pause: mouse occasionally doesn't emit movement this tick.
        if (dist > LOCK_RADIUS && this.rng.nextFloat() < PAUSE_CHANCE) {
            return current;
        }

        float dy;
        float dp;
        if (dist > LOCK_RADIUS) {
            // Discrete snap: fixed angular step along the error vector.
            float step;
            if (dist <= this.snapCap) {
                // Arrival — undershoot or slight overshoot, never a perfect landing.
                step = dist * (0.94F + this.rng.nextFloat() * 0.08F);
            } else {
                // Mid-snap — full cap with +-STEP_VARIANCE per-tick variation, never overshoot target.
                float cap = this.snapCap * (1.0F - STEP_VARIANCE + this.rng.nextFloat() * 2.0F * STEP_VARIANCE);
                step = Math.min(cap, dist);
            }
            float inv = step / dist;
            float wobble = (this.rng.nextFloat() * 2.0F - 1.0F) * WOBBLE;
            dy = errYaw * inv + wobble;
            dp = errPitch * inv - wobble;
        } else {
            // Lock: proportional with uniform jitter (not Gaussian).
            float gain = LOCK_GAIN * (0.85F + this.rng.nextFloat() * 0.3F);
            dy = errYaw   * gain + (this.rng.nextFloat() * 2.0F - 1.0F) * LOCK_JITTER;
            dp = errPitch * gain + (this.rng.nextFloat() * 2.0F - 1.0F) * LOCK_JITTER * 0.6F;
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
        if (this.lastHitCounter != Integer.MIN_VALUE && counter != this.lastHitCounter) {
            this.snapCap = rollSnap();
        }
        this.lastHitCounter = counter;
    }

    private float rollSnap() {
        // Sum of two uniforms: mild triangular bias toward the middle, avoids extreme values.
        float a = this.rng.nextFloat();
        float b = this.rng.nextFloat();
        return SNAP_MIN + (SNAP_MAX - SNAP_MIN) * (a + b) * 0.5F;
    }

    /**
     * Rounds the desired step to whole mouse counts and carries the honest remainder.
     * The bias is tiny (QUANT_BIAS * step, i.e. 0.01% of a step) — just enough to survive the
     * framework's integer truncation, small enough not to accumulate into a detectable drift.
     */
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
