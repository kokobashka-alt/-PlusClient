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
 * Humanized rotation for Aura: target selection and attacks stay with Aura, this mode only decides how the
 * view moves toward the aim point each tick.
 *
 * Movement is a damped second-order controller (stiffness, damping, speed cap) driven by the angular error to
 * a reaction-delayed copy of the target rotation, plus a slow in-hitbox aim drift (Ornstein-Uhlenbeck),
 * filtered hand tremor, a short disturbance after every attack and occasional micro-holds. The controller
 * parameters come from one of five archetypes (settle, smooth, snappy, drift, bursty); a new archetype with
 * freshly rolled parameters is chosen on every new target and every 40 to 140 ticks, and the live parameters
 * blend toward the new ones so that patterns change without discontinuities. Output deltas are quantized to the
 * mouse sensitivity grid with the sub-step remainder carried over, like integer mouse counts.
 *
 * Untested against any anticheat: whether a given server flags it depends on checks that are not known here.
 */
public class HumanRotationMode extends RotationMode {
    private static final int HISTORY = 8;
    private static final float PITCH_LIMIT = 89.0F;
    private static final float BLEND_PER_TICK = 0.12F;
    private static final int ARCHETYPES = 5;

    private static final class Params {
        float omega;
        float zeta;
        float maxSpeed;
        float tremor;
        float drift;
        float pitchGain;
        float reaction;
        float kick;
        float holdChance;

        void copyFrom(Params other) {
            omega = other.omega;
            zeta = other.zeta;
            maxSpeed = other.maxSpeed;
            tremor = other.tremor;
            drift = other.drift;
            pitchGain = other.pitchGain;
            reaction = other.reaction;
            kick = other.kick;
            holdChance = other.holdChance;
        }

        void blendToward(Params goal, float t) {
            omega += (goal.omega - omega) * t;
            zeta += (goal.zeta - zeta) * t;
            maxSpeed += (goal.maxSpeed - maxSpeed) * t;
            tremor += (goal.tremor - tremor) * t;
            drift += (goal.drift - drift) * t;
            pitchGain += (goal.pitchGain - pitchGain) * t;
            reaction += (goal.reaction - reaction) * t;
            kick += (goal.kick - kick) * t;
            holdChance += (goal.holdChance - holdChance) * t;
        }
    }

    private final Random rng = new Random(System.nanoTime());
    private final Params live = new Params();
    private final Params goal = new Params();
    private final float[] historyYaw = new float[HISTORY];
    private final float[] historyPitch = new float[HISTORY];
    private final float[] residual = new float[2];

    private Entity trackedTarget;
    private int archetype = -1;
    private int ticksToSwitch;
    private int historyHead;
    private int historyCount;
    private int holdTicks;
    private int lastHitCounter;
    private float velocityYaw;
    private float velocityPitch;
    private float tremorYaw;
    private float tremorPitch;
    private float driftYaw;
    private float driftPitch;

    public HumanRotationMode() {
        super("Human");
    }

    private float range(float min, float max) {
        return min + rng.nextFloat() * (max - min);
    }

    private void rollGoal(int kind) {
        switch (kind) {
            case 0 -> fill(0.28F, 0.42F, 0.72F, 0.90F, 16.0F, 26.0F, 0.03F, 0.07F, 0.15F, 0.30F, 0.60F, 0.85F, 1.0F, 3.0F, 0.15F, 0.35F, 0.00F, 0.02F);
            case 1 -> fill(0.18F, 0.30F, 0.95F, 1.20F, 10.0F, 18.0F, 0.02F, 0.05F, 0.10F, 0.25F, 0.70F, 0.95F, 2.0F, 4.0F, 0.10F, 0.25F, 0.00F, 0.00F);
            case 2 -> fill(0.45F, 0.60F, 0.70F, 0.85F, 24.0F, 40.0F, 0.04F, 0.09F, 0.10F, 0.20F, 0.50F, 0.75F, 1.0F, 2.0F, 0.25F, 0.50F, 0.00F, 0.01F);
            case 3 -> fill(0.20F, 0.32F, 0.80F, 1.00F, 12.0F, 20.0F, 0.06F, 0.12F, 0.35F, 0.60F, 0.45F, 0.70F, 2.0F, 4.0F, 0.20F, 0.40F, 0.02F, 0.04F);
            default -> fill(0.30F, 0.50F, 0.75F, 0.95F, 14.0F, 28.0F, 0.03F, 0.08F, 0.15F, 0.35F, 0.55F, 0.80F, 1.0F, 3.0F, 0.20F, 0.45F, 0.08F, 0.14F);
        }
    }

    private void fill(float oMin, float oMax, float zMin, float zMax, float sMin, float sMax, float tMin, float tMax,
                      float dMin, float dMax, float pMin, float pMax, float rMin, float rMax, float kMin, float kMax,
                      float hMin, float hMax) {
        goal.omega = range(oMin, oMax);
        goal.zeta = range(zMin, zMax);
        goal.maxSpeed = range(sMin, sMax);
        goal.tremor = range(tMin, tMax);
        goal.drift = range(dMin, dMax);
        goal.pitchGain = range(pMin, pMax);
        goal.reaction = range(rMin, rMax);
        goal.kick = range(kMin, kMax);
        goal.holdChance = range(hMin, hMax);
    }

    private void pickArchetype(boolean snapLive) {
        int next;
        do {
            next = rng.nextInt(ARCHETYPES);
        } while (next == archetype && ARCHETYPES > 1);
        archetype = next;
        rollGoal(next);
        ticksToSwitch = 40 + rng.nextInt(100);
        if (snapLive) {
            live.copyFrom(goal);
        }
    }

    private void acquire(Entity entity, Aura aura) {
        trackedTarget = entity;
        lastHitCounter = aura.hitCounter();
        historyCount = 0;
        historyHead = 0;
        velocityYaw = 0.0F;
        velocityPitch = 0.0F;
        residual[0] = 0.0F;
        residual[1] = 0.0F;
        driftYaw = 0.0F;
        driftPitch = 0.0F;
        pickArchetype(true);
        holdTicks = Math.round(live.reaction) + rng.nextInt(2);
    }

    private void pushHistory(Rotation target) {
        historyHead = (historyHead + 1) % HISTORY;
        historyYaw[historyHead] = target.getYaw();
        historyPitch[historyHead] = target.getPitch();
        historyCount = Math.min(historyCount + 1, HISTORY);
    }

    private int delayedIndex() {
        int back = Math.min(Math.max(Math.round(live.reaction), 0), historyCount - 1);
        return ((historyHead - back) % HISTORY + HISTORY) % HISTORY;
    }

    private static float controller(float error, float velocity, float omega, float zeta, float maxSpeed) {
        float acceleration = omega * omega * error - 2.0F * zeta * omega * velocity;
        float next = velocity + acceleration;
        float limit = maxSpeed * MathHelper.clamp(0.25F + Math.abs(error) / 25.0F, 0.25F, 1.0F);
        return MathHelper.clamp(next, -limit, limit);
    }

    /** Snaps a movement to whole mouse counts, carrying the remainder to the next tick. */
    private float quantize(int axis, float desired, double step) {
        double pending = desired + residual[axis];
        long counts = Math.round(pending / step);
        double applied = counts * step;
        residual[axis] = (float) (pending - applied);
        if (counts == 0) {
            return 0.0F;
        }
        // The framework truncates delta / step; a hair above the exact multiple keeps the count intact.
        return (float) (applied + Math.signum(applied) * step * 0.01);
    }

    private Rotation apply(Rotation current, float moveYaw, float movePitch, double step) {
        float dy = quantize(0, moveYaw, step);
        float dp = quantize(1, movePitch, step);
        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + dy),
                MathHelper.clamp(current.getPitch() + dp, -PITCH_LIMIT, PITCH_LIMIT));
    }

    private Rotation returnToCamera(Rotation current, Rotation target, double step) {
        trackedTarget = null;
        archetype = -1;
        holdTicks = 0;
        driftYaw = 0.0F;
        driftPitch = 0.0F;
        tremorYaw = 0.0F;
        tremorPitch = 0.0F;
        float yawError = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchError = MathHelper.clamp(target.getPitch(), -PITCH_LIMIT, PITCH_LIMIT) - current.getPitch();
        velocityYaw = controller(yawError, velocityYaw, 0.45F, 1.0F, 25.0F);
        velocityPitch = controller(pitchError, velocityPitch, 0.45F, 1.0F, 14.0F);
        return apply(current, velocityYaw, velocityPitch, step);
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        Aura aura = (Aura) AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
        double step = MathUtil.computeGcd();
        if (player == null || aura == null || !(step > 1.0E-4)) {
            return current;
        }
        if (entity == null) {
            return returnToCamera(current, target, step);
        }
        if (trackedTarget != entity) {
            acquire(entity, aura);
        }

        if (aura.hitCounter() != lastHitCounter) {
            lastHitCounter = aura.hitCounter();
            velocityYaw = velocityYaw * 0.4F + (float) rng.nextGaussian() * live.kick;
            velocityPitch = velocityPitch * 0.4F + (float) rng.nextGaussian() * live.kick * 0.5F;
        }

        pushHistory(target);
        if (--ticksToSwitch <= 0) {
            pickArchetype(false);
        }
        live.blendToward(goal, BLEND_PER_TICK);

        int index = delayedIndex();
        float distance = Math.max(player.distanceTo(entity), 0.5F);
        float maxDriftYaw = (float) Math.toDegrees(Math.atan2(entity.getWidth() * 0.35F, distance)) * live.drift;
        float maxDriftPitch = (float) Math.toDegrees(Math.atan2(entity.getHeight() * 0.25F, distance)) * live.drift;
        driftYaw = MathHelper.clamp(driftYaw * 0.95F + (float) rng.nextGaussian() * maxDriftYaw * 0.12F, -maxDriftYaw, maxDriftYaw);
        driftPitch = MathHelper.clamp(driftPitch * 0.95F + (float) rng.nextGaussian() * maxDriftPitch * 0.12F, -maxDriftPitch, maxDriftPitch);

        float yawError = MathHelper.wrapDegrees(historyYaw[index] + driftYaw - current.getYaw());
        float pitchError = MathHelper.clamp(historyPitch[index] + driftPitch, -PITCH_LIMIT, PITCH_LIMIT) - current.getPitch();

        if (holdTicks > 0) {
            holdTicks--;
            velocityYaw *= 0.5F;
            velocityPitch *= 0.5F;
        } else if (rng.nextFloat() < live.holdChance) {
            holdTicks = 1 + rng.nextInt(3);
            velocityYaw *= 0.5F;
            velocityPitch *= 0.5F;
        } else {
            velocityYaw = controller(yawError, velocityYaw, live.omega, live.zeta, live.maxSpeed);
            velocityPitch = controller(pitchError, velocityPitch, live.omega * live.pitchGain, live.zeta, live.maxSpeed * 0.55F);
        }

        tremorYaw = tremorYaw * 0.8F + (float) rng.nextGaussian() * live.tremor;
        tremorPitch = tremorPitch * 0.8F + (float) rng.nextGaussian() * live.tremor * 0.6F;
        return apply(current, velocityYaw + tremorYaw, velocityPitch + tremorPitch, step);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.0D, 0.0D, 0.0D);
    }
}
