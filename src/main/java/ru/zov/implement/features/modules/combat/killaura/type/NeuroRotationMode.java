package ru.zov.implement.features.modules.combat.killaura.type;

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
 * Точка расширения: если у вас появится NeuroController с весами —
 * переопределите predict(...), оставив calculate(...) как обёртку.
 */
public final class NeuroRotationMode implements RotationMode {

    private final Random rnd = new Random();
    private Phase phase = Phase.FAR;

    private enum Phase { FAR, NEAR, LOCK }

    private static final float NEAR_THRESHOLD = 20f; // градусы
    private static final float LOCK_THRESHOLD = 3f;  // градусы

    @Override
    public Rotation calculate(Rotation from, Rotation to) {
        if (from == null || to == null) return to;

        float dYaw = wrap(to.getYaw() - from.getYaw());
        float dPitch = to.getPitch() - from.getPitch();
        float dist = (float) Math.hypot(dYaw, dPitch);

        // --- Определяем фазу ---
        if (dist > NEAR_THRESHOLD) {
            phase = Phase.FAR;
        } else if (dist > LOCK_THRESHOLD) {
            phase = Phase.NEAR;
        } else {
            phase = Phase.LOCK;
        }

        // --- Скорость зависит от фазы ---
        float speed = switch (phase) {
            case FAR  -> 0.75f + rnd.nextFloat() * 0.15f; // рывок
            case NEAR -> 0.35f + rnd.nextFloat() * 0.10f; // торможение
            case LOCK -> 0.12f + rnd.nextFloat() * 0.06f; // удержание
        };

        float newYaw = from.getYaw() + dYaw * speed;
        float newPitch = from.getPitch() + dPitch * speed;

        // --- «Человеческий» джиттер ---
        float jitter = phase == Phase.LOCK ? 0.06f : 0.15f;
        newYaw += (rnd.nextFloat() - 0.5f) * jitter;
        newPitch += (rnd.nextFloat() - 0.5f) * jitter * 0.7f;

        newPitch = clampPitch(newPitch);

        return new Rotation(newYaw, newPitch);
    }

    /**
     * Точка расширения для настоящей ML-модели.
     * Пока просто делегирует в calculate.
     */
    protected Rotation predict(Rotation from, Rotation to) {
        return calculate(from, to);
    }

    @Override
    public float randomValue() {
        return (rnd.nextFloat() - 0.5f) * 0.4f;
    }

    @Override
    public void reset() {
        phase = Phase.FAR;
    }

    // --- утилиты ---
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
