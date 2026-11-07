package ru.zov.common.util.rotation.angle.packet;

import net.minecraft.util.math.MathHelper;
import ru.zov.common.util.rotation.angle.Angle;

import java.util.Random;

/**
 * SpookyTime-ротация.
 *
 * Идея: плавная линейная интерполяция к цели + небольшой случайный джиттер.
 * Джиттер «размазывает» траекторию по тикам, из-за чего античиты
 * (Matrix/Intave/Grim) видят менее «идеальную» наводку.
 *
 * Полностью автономна — не требует AngleSmoothMode, RotationConfig и т.д.
 */
public class SpookyTimeSmooth {

    /** Базовая скорость наводки за тик (0..1). */
    private final float baseSpeed;
    /** Амплитуда джиттера в градусах. */
    private final float jitter;
    /** Джиттер по pitch (обычно меньше, чтобы не улетать вверх/вниз). */
    private final float pitchJitter;

    private final Random rnd = new Random();

    public SpookyTimeSmooth() {
        this(0.45f, 0.4f, 0.2f);
    }

    public SpookyTimeSmooth(float baseSpeed, float jitter, float pitchJitter) {
        this.baseSpeed = MathHelper.clamp(baseSpeed, 0.05f, 1.0f);
        this.jitter = Math.max(0.0f, jitter);
        this.pitchJitter = Math.max(0.0f, pitchJitter);
    }

    /**
     * Один тик ротации.
     *
     * @param from текущий угол камеры
     * @param to   желаемый угол (на цель)
     * @return новый угол после шага
     */
    public Angle calculate(Angle from, Angle to) {
        if (from == null || to == null) return to;

        float dYaw = MathHelper.wrapDegrees(to.getYaw() - from.getYaw());
        float dPitch = to.getPitch() - from.getPitch();

        // Расстояние до цели в градусах — управляет «адаптивной» скоростью.
        float distance = (float) Math.hypot(dYaw, dPitch);
        float adaptive = MathHelper.clamp(distance / 45f, 0.25f, 1.0f);

        float speed = baseSpeed * adaptive;

        float newYaw = from.getYaw() + dYaw * speed
            + (rnd.nextFloat() - 0.5f) * 2.0f * jitter;

        float newPitch = from.getPitch() + dPitch * speed
            + (rnd.nextFloat() - 0.5f) * 2.0f * pitchJitter;

        // Ограничиваем pitch физическими пределами игрока.
        newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);

        return new Angle(newYaw, newPitch);
    }

    /** Случайное значение для PointFinder (как в phobia). */
    public float randomValue() {
        return (rnd.nextFloat() - 0.5f) * 0.6f;
    }
}
