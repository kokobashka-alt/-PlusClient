package ru.zov.implement.features.modules.combat.killaura.neuro;

/**
 * One tick of input, already converted to plain numbers. Everything the feature builder needs is here, so the
 * feature code has no Minecraft dependency and can be exercised offline.
 *
 * <p>Velocities are per-tick deltas in blocks, angles are degrees, {@code ticksSinceAttack} is in client ticks and
 * {@link #ATTACK_UNKNOWN} means "not measured" (which is not the same as zero).
 */
public record RawSample(
        float curYaw,
        float curPitch,
        float targetYaw,
        float targetPitch,
        float distance,
        float heightDelta,
        float targetVx,
        float targetVz,
        float playerVx,
        float playerVy,
        float playerVz,
        boolean onGround,
        float ticksSinceAttack,
        float prevStepYaw,
        float prevStepPitch) {

    /** Sentinel for "the last attack time is not known"; the feature builder maps it to its own sentinel. */
    public static final float ATTACK_UNKNOWN = -1.0F;
}
