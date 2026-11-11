package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.List;
import java.util.Random;

/**
 * One "hand style" for the Shard rotation mode. All angles are degrees, all times are client ticks
 * (1 tick = 50 ms), speeds are degrees per tick.
 *
 * Every numeric value in {@link #CATALOG} is an UNCALIBRATED starting point chosen from general
 * human-aiming ranges. None of them was measured against the Shard anticheat.
 *
 * The compact constructor clamps everything into a safe range, so {@link #lerp} and {@link #mutate}
 * can never produce an unstable or non-finite profile.
 */
public record ShardProfile(
        String name,
        float reactionMinTicks,
        float reactionMaxTicks,
        float peakYawSpeed,
        float peakPitchSpeed,
        float omega,
        float zeta,
        float accLimit,
        float jerkLimit,
        float pitchGain,
        float tremorSigma,
        float tremorCorr,
        float driftSigmaYaw,
        float driftSigmaPitch,
        float driftRevert,
        float overshootChance,
        float overshootFraction,
        float overshootMinDistance,
        float pauseChance,
        float pauseMinTicks,
        float pauseMaxTicks,
        float arcGain,
        float attackTighten,
        float pursuitGain) {

    public ShardProfile {
        name = name == null ? "unnamed" : name;
        reactionMinTicks = clamp(reactionMinTicks, 1.0F, 20.0F);
        reactionMaxTicks = clamp(Math.max(reactionMaxTicks, reactionMinTicks), 1.0F, 24.0F);
        peakYawSpeed = clamp(peakYawSpeed, 4.0F, 60.0F);
        peakPitchSpeed = clamp(peakPitchSpeed, 2.0F, 30.0F);
        omega = clamp(omega, 0.08F, 0.85F);
        zeta = clamp(zeta, 0.35F, 1.5F);
        accLimit = clamp(accLimit, 0.8F, 14.0F);
        jerkLimit = clamp(jerkLimit, 0.2F, 5.0F);
        pitchGain = clamp(pitchGain, 0.3F, 1.0F);
        tremorSigma = clamp(tremorSigma, 0.0F, 0.2F);
        tremorCorr = clamp(tremorCorr, 0.0F, 0.95F);
        driftSigmaYaw = clamp(driftSigmaYaw, 0.0F, 2.0F);
        driftSigmaPitch = clamp(driftSigmaPitch, 0.0F, 1.5F);
        driftRevert = clamp(driftRevert, 0.01F, 0.4F);
        overshootChance = clamp(overshootChance, 0.0F, 0.9F);
        overshootFraction = clamp(overshootFraction, 0.0F, 0.3F);
        overshootMinDistance = clamp(overshootMinDistance, 4.0F, 60.0F);
        pauseChance = clamp(pauseChance, 0.0F, 0.2F);
        pauseMinTicks = clamp(pauseMinTicks, 1.0F, 12.0F);
        pauseMaxTicks = clamp(Math.max(pauseMaxTicks, pauseMinTicks), 1.0F, 16.0F);
        arcGain = clamp(arcGain, 0.0F, 0.3F);
        attackTighten = clamp(attackTighten, 0.1F, 1.0F);
        pursuitGain = clamp(pursuitGain, 0.4F, 1.0F);
    }

    public static final ShardProfile SMOOTH_TRACKER = new ShardProfile("SmoothTracker",
            4, 7, 22, 7, 0.28F, 1.00F, 4.0F, 1.10F, 0.75F,
            0.05F, 0.60F, 0.55F, 0.35F, 0.06F,
            0.10F, 0.06F, 18, 0.015F, 2, 4, 0.05F, 0.45F, 0.88F);

    public static final ShardProfile FLICKER = new ShardProfile("Flicker",
            3, 5, 38, 11, 0.42F, 0.85F, 7.0F, 2.20F, 0.80F,
            0.07F, 0.50F, 0.45F, 0.30F, 0.09F,
            0.22F, 0.08F, 14, 0.020F, 2, 3, 0.04F, 0.35F, 0.92F);

    public static final ShardProfile LAZY_DRIFT = new ShardProfile("LazyDrift",
            6, 10, 16, 5, 0.20F, 1.05F, 2.6F, 0.70F, 0.60F,
            0.09F, 0.75F, 1.00F, 0.70F, 0.04F,
            0.06F, 0.05F, 20, 0.045F, 3, 6, 0.08F, 0.55F, 0.75F);

    public static final ShardProfile OVERSHOOTER = new ShardProfile("Overshooter",
            4, 6, 30, 9, 0.36F, 0.62F, 6.0F, 1.80F, 0.70F,
            0.06F, 0.55F, 0.60F, 0.40F, 0.07F,
            0.50F, 0.14F, 10, 0.020F, 2, 4, 0.06F, 0.40F, 0.85F);

    public static final ShardProfile SURGICAL = new ShardProfile("Surgical",
            5, 8, 34, 10, 0.50F, 0.95F, 8.0F, 2.60F, 0.90F,
            0.03F, 0.40F, 0.22F, 0.16F, 0.12F,
            0.08F, 0.04F, 16, 0.008F, 2, 3, 0.02F, 0.30F, 0.95F);

    public static final ShardProfile ARC_MOVER = new ShardProfile("ArcMover",
            4, 7, 26, 8, 0.33F, 0.90F, 5.0F, 1.40F, 0.85F,
            0.06F, 0.65F, 0.70F, 0.50F, 0.06F,
            0.15F, 0.09F, 15, 0.025F, 2, 4, 0.16F, 0.40F, 0.88F);

    public static final List<ShardProfile> CATALOG = List.of(
            SMOOTH_TRACKER, FLICKER, LAZY_DRIFT, OVERSHOOTER, SURGICAL, ARC_MOVER);

    /** Component-wise blend. {@code t} is clamped to 0..1; the name follows whichever side is closer. */
    public static ShardProfile lerp(ShardProfile a, ShardProfile b, float t) {
        float k = clamp(t, 0.0F, 1.0F);
        String label = k < 0.5F ? a.name : b.name;
        return new ShardProfile(label,
                mix(a.reactionMinTicks, b.reactionMinTicks, k),
                mix(a.reactionMaxTicks, b.reactionMaxTicks, k),
                mix(a.peakYawSpeed, b.peakYawSpeed, k),
                mix(a.peakPitchSpeed, b.peakPitchSpeed, k),
                mix(a.omega, b.omega, k),
                mix(a.zeta, b.zeta, k),
                mix(a.accLimit, b.accLimit, k),
                mix(a.jerkLimit, b.jerkLimit, k),
                mix(a.pitchGain, b.pitchGain, k),
                mix(a.tremorSigma, b.tremorSigma, k),
                mix(a.tremorCorr, b.tremorCorr, k),
                mix(a.driftSigmaYaw, b.driftSigmaYaw, k),
                mix(a.driftSigmaPitch, b.driftSigmaPitch, k),
                mix(a.driftRevert, b.driftRevert, k),
                mix(a.overshootChance, b.overshootChance, k),
                mix(a.overshootFraction, b.overshootFraction, k),
                mix(a.overshootMinDistance, b.overshootMinDistance, k),
                mix(a.pauseChance, b.pauseChance, k),
                mix(a.pauseMinTicks, b.pauseMinTicks, k),
                mix(a.pauseMaxTicks, b.pauseMaxTicks, k),
                mix(a.arcGain, b.arcGain, k),
                mix(a.attackTighten, b.attackTighten, k),
                mix(a.pursuitGain, b.pursuitGain, k));
    }

    /**
     * A per-activation variant: every numeric field is scaled by a random factor in
     * {@code 1 +/- amount}, so the same catalog entry never produces the exact same numbers twice.
     */
    public ShardProfile mutate(Random rng, float amount) {
        return new ShardProfile(name,
                jitter(rng, reactionMinTicks, amount), jitter(rng, reactionMaxTicks, amount),
                jitter(rng, peakYawSpeed, amount), jitter(rng, peakPitchSpeed, amount),
                jitter(rng, omega, amount), jitter(rng, zeta, amount),
                jitter(rng, accLimit, amount), jitter(rng, jerkLimit, amount),
                jitter(rng, pitchGain, amount),
                jitter(rng, tremorSigma, amount), jitter(rng, tremorCorr, amount * 0.5F),
                jitter(rng, driftSigmaYaw, amount), jitter(rng, driftSigmaPitch, amount),
                jitter(rng, driftRevert, amount),
                jitter(rng, overshootChance, amount), jitter(rng, overshootFraction, amount),
                jitter(rng, overshootMinDistance, amount),
                jitter(rng, pauseChance, amount),
                jitter(rng, pauseMinTicks, amount), jitter(rng, pauseMaxTicks, amount),
                jitter(rng, arcGain, amount), jitter(rng, attackTighten, amount),
                jitter(rng, pursuitGain, amount * 0.5F));
    }

    private static float jitter(Random rng, float value, float amount) {
        return value * (1.0F + amount * (rng.nextFloat() * 2.0F - 1.0F));
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float lo, float hi) {
        if (!Float.isFinite(v)) {
            return lo;
        }
        return v < lo ? lo : Math.min(v, hi);
    }
}
