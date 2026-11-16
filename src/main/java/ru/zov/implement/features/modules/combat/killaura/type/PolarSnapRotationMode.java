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
 * Polar Snap: while the view is far from the target, each tick covers a fixed fraction of the remaining error,
 * capped at a per-swing snap speed (22 to 40 degrees per tick). The step never overshoots, so the view lands on
 * the target in a few ticks. Inside a 1.2 degree radius it switches to a gentle proportional lock with a tiny
 * jitter, so it does not hunt. The snap speed is re-rolled after every hit.
 *
 * The snap speed and lock gain are hand-picked starting values. They have not been measured against Polar.
 */
public final class PolarSnapRotationMode extends RotationMode {
    private static final float PITCH_LIMIT = 89.0F;
    private static final float LOCK_RADIUS = 1.2F;
    private static final float LOCK_GAIN = 0.35F;
    private static final float LOCK_JITTER = 0.03F;
    private static final float SNAP_MIN = 22.0F;
    private static final float SNAP_MAX = 40.0F;

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
        }

        this.checkHit();

        float errYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float errPitch = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT) - current.getPitch();
        float dist = (float) Math.hypot(errYaw, errPitch);

        float dy;
        float dp;
        if (dist > LOCK_RADIUS) {
            float fraction = Math.min(1.0F, this.snapCap / dist);
            dy = errYaw * fraction;
            dp = errPitch * fraction;
        } else {
            dy = errYaw * LOCK_GAIN + (float) this.rng.nextGaussian() * LOCK_JITTER;
            dp = errPitch * LOCK_GAIN + (float) this.rng.nextGaussian() * LOCK_JITTER * 0.6F;
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
        return SNAP_MIN + (SNAP_MAX - SNAP_MIN) * this.rng.nextFloat();
    }

    /** Snaps a step to whole mouse counts and carries the remainder to the next tick. */
    private float quantize(int axis, float desired, double step) {
        double pending = desired + this.residual[axis];
        long counts = Math.round(pending / step);
        double exact = counts * step;
        this.residual[axis] = (float) (pending - exact);
        if (counts == 0) {
            return 0.0F;
        }

        // The framework truncates delta / step, so land a hair above the exact multiple.
        return (float) (exact + Math.signum(exact) * step * 0.01);
    }
}
