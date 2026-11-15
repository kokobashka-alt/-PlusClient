package ru.zov.implement.modules.combat.killaura.neuro;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.neuro.FeatureVector;
import ru.zov.implement.features.modules.combat.killaura.neuro.RawSample;

/**
 * Minecraft-facing side of the feature builder. Converts the live entities into a {@link RawSample} and lets
 * {@link FeatureVector} do the arithmetic, so the numbers here are the same ones the recorder writes and the trainer
 * expects.
 *
 * <p>The attack timer is counted in {@link #tick()} calls, i.e. one per client tick. If your killaura calls the
 * rotation more than once per tick, move {@link #tick()} to your own tick hook or the "ticks since attack" feature
 * will run fast.
 */
public final class BezierFeatures {
    public static final int COUNT = FeatureVector.COUNT;

    private final FeatureVector vector = new FeatureVector();
    private float attackAge = RawSample.ATTACK_UNKNOWN;

    public void reset() {
        this.vector.reset();
    }

    /** Counts one client tick. Call once per tick, before building the sample. */
    public void tick() {
        if (this.attackAge >= 0.0F && this.attackAge < 1000.0F) {
            this.attackAge += 1.0F;
        }
    }

    /** Called by the killaura when it actually attacks. */
    public void onAttack() {
        this.attackAge = 0.0F;
    }

    public RawSample sample(float curYaw, float curPitch, float targetYaw, float targetPitch,
                            ClientPlayerEntity player, LivingEntity target, float prevYaw, float prevPitch) {
        Vec3d eye = player.getEyePos();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        double aimX = targetPos.x;
        double aimY = targetPos.y + target.getHeight() * 0.5;
        double aimZ = targetPos.z;

        float distance = (float) Math.sqrt(sq(aimX - eye.x) + sq(aimY - eye.y) + sq(aimZ - eye.z));
        float heightDelta = (float) (aimY - eye.y);

        Vec3d pv = player.getVelocity();
        Vec3d tv = target.getVelocity();

        return new RawSample(
                curYaw, curPitch, targetYaw, targetPitch,
                distance, heightDelta,
                (float) tv.x, (float) tv.z,
                (float) pv.x, (float) pv.y, (float) pv.z,
                player.isOnGround(), this.attackAge,
                prevYaw, prevPitch);
    }

    /** Returns false when the sample is unusable, in which case the caller must fall back for this tick. */
    public boolean fill(float[] out, RawSample sample) {
        return this.vector.fill(sample, out);
    }

    private static double sq(double v) {
        return v * v;
    }
}
