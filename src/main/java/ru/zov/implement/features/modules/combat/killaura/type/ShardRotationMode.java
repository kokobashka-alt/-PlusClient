package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;

/**
 * Shard: a damped tracker with a soft (tanh) speed cap, organic curve arcs,
 * micro-pauses, dynamic hit kicks, and exact GCD quantization matching ru.zov framework.
 */
public final class ShardRotationMode extends RotationMode {
    private static final float PITCH_LIMIT = 89.0F;
    private static final float NOISE_AMP = 0.16F;
    private static final float PAUSE_CHANCE = 0.04F;
    private static final float HIT_KICK = 0.5F;

    private final Random rng = new Random();
    private final float[] residual = new float[2];
    private Entity tracked;
    private int lastHitCounter = Integer.MIN_VALUE;
    private int rerollIn;
    private int pauseTicks;
    private float startDist = 1.0F;
    private float bowSide = 1.0F;
    private float bowAmp;
    private float omega;
    private float zeta;
    private float maxSpeed;
    private float velYaw;
    private float velPitch;
    private float noiseYaw;
    private float noisePitch;

    public ShardRotationMode() {
        super("Shard");
    }

    public void reset() {
        this.tracked = null;
        this.lastHitCounter = Integer.MIN_VALUE;
        this.rerollIn = 0;
        this.pauseTicks = 0;
        this.startDist = 1.0F;
        this.velYaw = 0.0F;
        this.velPitch = 0.0F;
        this.noiseYaw = 0.0F;
        this.noisePitch = 0.0F;
        this.residual[0] = 0.0F;
        this.residual[1] = 0.0F;
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double gcd = MathUtil.computeGcd();
        if (player == null || current == null || target == null || !(gcd > 1.0E-4)) {
            return current;
        }

        if (entity == null) {
            this.reset();
            return this.toCamera(current, target, gcd);
        }

        if (this.tracked != entity) {
            this.startSegment(current, target, entity);
        }

        this.checkHit();
        if (--this.rerollIn <= 0) {
            this.rollParams();
        }

        float errYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float errPitch = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT) - current.getPitch();
        float dist = (float) Math.hypot(errYaw, errPitch);

        float done = MathHelper.clamp(1.0F - dist / Math.max(this.startDist, 1.0E-3F), 0.0F, 1.0F);
        float bow = this.bowAmp * (float) Math.sin(Math.PI * done) * this.bowSide;
        float aimYawErr = MathHelper.wrapDegrees(errYaw + bow);
        float aimPitchErr = errPitch - bow * 0.25F;

        float near = MathHelper.clamp(dist / 4.0F, 0.15F, 1.0F);
        this.noiseYaw = this.noiseYaw * 0.86F + (float) this.rng.nextGaussian() * NOISE_AMP * near;
        this.noisePitch = this.noisePitch * 0.86F + (float) this.rng.nextGaussian() * NOISE_AMP * near * 0.6F;

        if (this.pauseTicks > 0) {
            this.pauseTicks--;
            this.velYaw *= 0.45F;
            this.velPitch *= 0.45F;
        } else if (this.rng.nextFloat() < PAUSE_CHANCE) {
            this.pauseTicks = 1 + this.rng.nextInt(2);
            this.velYaw *= 0.45F;
            this.velPitch *= 0.45F;
        } else {
            this.velYaw = limited(aimYawErr, this.velYaw, this.omega, this.zeta, this.maxSpeed);
            this.velPitch = limited(aimPitchErr, this.velPitch, this.omega, this.zeta, this.maxSpeed * 0.7F);
        }

        float dy = this.quantize(0, this.velYaw + this.noiseYaw, gcd);
        float dp = this.quantize(1, this.velPitch + this.noisePitch, gcd);
        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + dy),
                MathHelper.clamp(current.getPitch() + dp, -PITCH_LIMIT, PITCH_LIMIT));
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private void startSegment(Rotation current, Rotation target, Entity entity) {
        this.tracked = entity;
        this.pauseTicks = 0;
        this.noiseYaw = 0.0F;
        this.noisePitch = 0.0F;
        float errYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float errPitch = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT) - current.getPitch();
        this.startDist = Math.max((float) Math.hypot(errYaw, errPitch), 1.0F);
        this.bowSide = this.rng.nextBoolean() ? 1.0F : -1.0F;
        this.bowAmp = MathHelper.clamp(Math.abs(errYaw) * (0.05F + 0.07F * this.rng.nextFloat()), 0.4F, 5.0F);
        this.rollParams();
    }

    private void rollParams() {
        this.omega = 0.27F + 0.09F * this.rng.nextFloat();
        this.zeta = 0.95F + 0.2F * this.rng.nextFloat();
        this.maxSpeed = 7.0F + 4.0F * this.rng.nextFloat();
        this.rerollIn = 60 + this.rng.nextInt(101);
    }

    private void checkHit() {
        Aura aura = (Aura) AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
        if (aura == null) {
            return;
        }

        int counter = aura.hitCounter();
        if (this.lastHitCounter != Integer.MIN_VALUE && counter != this.lastHitCounter) {
            this.velYaw = this.velYaw * 0.5F + (float) this.rng.nextGaussian() * HIT_KICK;
            this.velPitch = this.velPitch * 0.5F + (float) this.rng.nextGaussian() * HIT_KICK * 0.5F;
            this.pauseTicks = 0;
        }

        this.lastHitCounter = counter;
    }

    private Rotation toCamera(Rotation current, Rotation target, double gcd) {
        float errYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float errPitch = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT) - current.getPitch();
        this.velYaw = limited(errYaw, this.velYaw, 0.4F, 1.0F, 12.0F);
        this.velPitch = limited(errPitch, this.velPitch, 0.4F, 1.0F, 8.0F);
        float dy = this.quantize(0, this.velYaw, gcd);
        float dp = this.quantize(1, this.velPitch, gcd);
        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + dy),
                MathHelper.clamp(current.getPitch() + dp, -PITCH_LIMIT, PITCH_LIMIT));
    }

    private static float limited(float error, float vel, float omega, float zeta, float maxSpeed) {
        float acc = omega * omega * error - 2.0F * zeta * omega * vel;
        float next = vel + acc;
        return maxSpeed * (float) Math.tanh(next / maxSpeed);
    }

    private float quantize(int axis, float desired, double step) {
        double pending = desired + this.residual[axis];
        long counts = Math.round(pending / step);
        double exact = counts * step;
        this.residual[axis] = (float) (pending - exact);
        if (counts == 0) {
            return 0.0F;
        }
        return (float) (exact + Math.signum(exact) * step * 0.01);
    }
}
