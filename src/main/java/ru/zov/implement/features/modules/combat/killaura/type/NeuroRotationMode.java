package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.util.Random;

/**
 * Neuro-имитация (без ML-модели).
 *
 * Фазы наводки:
 *   FAR  — цель далеко по углу, быстрый рывок
 *   NEAR — цель близко, плавное торможение
 *   LOCK — удержание с микро-дрожанием (имитация руки)
 *
 * Точка расширения: если появится NeuroController с весами —
 * переопределите predict(...), оставив process(...) как обёртку.
 */
public final class NeuroRotationMode extends RotationMode {

    private final Random rnd = new Random();
    private Phase phase = Phase.FAR;

    private enum Phase { FAR, NEAR, LOCK }

    private static final float NEAR_THRESHOLD = 20f;
    private static final float LOCK_THRESHOLD = 3f;

    public NeuroRotationMode() {
        this("Neuro");
    }

    public NeuroRotationMode(String name) {
        super(name);
    }

    @Override
    public Rotation process(Rotation from, Rotation to, Vec3d vec, Entity entity) {
        if (from == null || to == null) return to;

        float dYaw = wrap(to.getYaw() - from.getYaw());
        float dPitch = to.getPitch() - from.getPitch();
        float dist = (float) Math.hypot(dYaw, dPitch);

        if (dist > NEAR_THRESHOLD)      phase = Phase.FAR;
        else if (dist > LOCK_THRESHOLD) phase = Phase.NEAR;
        else                            phase = Phase.LOCK;

        float speed = switch (phase) {
            case FAR  -> 0.75f + rnd.nextFloat() * 0.15f;
            case NEAR -> 0.35f + rnd.nextFloat() * 0.10f;
            case LOCK -> 0.12f + rnd.nextFloat() * 0.06f;
        };

        float newYaw = from.getYaw() + dYaw * speed;
        float newPitch = from.getPitch() + dPitch * speed;

        float jitter = phase == Phase.LOCK ? 0.06f : 0.15f;
        newYaw   += (rnd.nextFloat() - 0.5f) * jitter;
        newPitch += (rnd.nextFloat() - 0.5f) * jitter * 0.7f;

        return new Rotation(newYaw, clampPitch(newPitch));
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(
            (rnd.nextDouble() - 0.5) * 0.4,
            0.0,
            (rnd.nextDouble() - 0.5) * 0.4
        );
    }

    /** Сброс фазы в FAR (можно звать из Aura при смене цели). */
    public void reset() {
        phase = Phase.FAR;
    }

    private static float wrap(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private static float clampPitch(float p) {
        return p < -90f ? -90f : (p > 90f ? 90f : p);
    }
}
