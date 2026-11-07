package ru.zov.implement.features.modules.combat.killaura.type;

import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * MLS (Multi-Linear Step) — двойной шаг линейной интерполяции за тик.
 *
 * Первый шаг — «основной» (к цели), второй — «догоняющий» (доводит остаток).
 * Визуально — плавная наводка с лёгким ускорением на финише.
 */
public final class MlsRotationMode implements RotationMode {

    private final float step1;
    private final float step2;

    public MlsRotationMode() {
        this(0.55f, 0.65f);
    }

    public MlsRotationMode(float step1, float step2) {
        this.step1 = clamp01(step1);
        this.step2 = clamp01(step2);
    }

    @Override
    public Rotation calculate(Rotation from, Rotation to) {
        if (from == null || to == null) return to;

        // --- Шаг 1: from → промежуточный ---
        float dYaw1 = wrap(to.getYaw() - from.getYaw()) * step1;
        float dPitch1 = (to.getPitch() - from.getPitch()) * step1;

        float midYaw = from.getYaw() + dYaw1;
        float midPitch = from.getPitch() + dPitch1;

        // --- Шаг 2: промежуточный → цель ---
        float dYaw2 = wrap(to.getYaw() - midYaw) * step2;
        float dPitch2 = (to.getPitch() - midPitch) * step2;

        float newYaw = midYaw + dYaw2;
        float newPitch = clampPitch(midPitch + dPitch2);

        return new Rotation(newYaw, newPitch);
    }

    @Override
    public float randomValue() {
        return 0f;
    }

    @Override
    public void reset() {
        // MLS не хранит состояние — сбрасывать нечего.
    }

    // --- утилиты ---
    private static float wrap(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private static float clamp01(float v) {
        return v < 0.05f ? 0.05f : (v > 1f ? 1f : v);
    }

    private static float clampPitch(float p) {
        return p < -90f ? -90f : (p > 90f ? 90f : p);
    }
}
