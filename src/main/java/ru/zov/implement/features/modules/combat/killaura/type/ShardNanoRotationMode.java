package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * Shard 2 (Nano) — плавная наводка с ограничением ускорения и «тормозом»
 * по расстоянию до цели. Внутри — та же математика, что в оригинальном
 * ShardNanoRotation (Axis/velocity/acceleration + brakingSpeed).
 *
 * Один вызов process(...) = один тик шага.
 * Скорость задаётся снаружи через setSpeed(...) (Aura прокидывает из настройки).
 */
public final class ShardNanoRotationMode extends RotationMode {

    private static final class Axis {
        private float velocity;
        private float acceleration;

        float step(float error, float maxSpeed) {
            if (Math.abs(error) < 0.01f) {
                reset();
                return 0f;
            }
            float accelerationLimit = maxSpeed * 0.35f;
            float brakingSpeed = (float) Math.sqrt(2f * accelerationLimit * Math.abs(error));
            float desiredVelocity = Math.copySign(
                    Math.min(maxSpeed, Math.min(brakingSpeed, Math.abs(error) * 0.65f)),
                    error
            );
            float desiredAcceleration = MathHelper.clamp(desiredVelocity - velocity,
                    -accelerationLimit, accelerationLimit);
            acceleration += MathHelper.clamp(desiredAcceleration - acceleration,
                    -accelerationLimit * 0.5f, accelerationLimit * 0.5f);
            velocity = MathHelper.clamp(velocity + acceleration, -maxSpeed, maxSpeed);
            if (Math.signum(velocity) == Math.signum(error) && Math.abs(velocity) > Math.abs(error)) {
                velocity = error;
                acceleration = 0f;
            }
            return velocity;
        }

        void reset() {
            velocity = 0f;
            acceleration = 0f;
        }
    }

    private final Axis yaw = new Axis();
    private final Axis pitch = new Axis();

    /** Скорость наводки, 4..70 (как в оригинале). Дефолт 20. */
    private float speed = 20f;

    public ShardNanoRotationMode() {
        super("Shard 2 (Nano)");
    }

    public void setSpeed(float speed) {
        this.speed = MathHelper.clamp(speed, 4f, 70f);
    }

    public void reset() {
        yaw.reset();
        pitch.reset();
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec, Entity entity) {
        if (current == null || target == null) {
            reset();
            return current;
        }

        float currentYaw = current.getYaw();
        float currentPitch = current.getPitch();
        float targetYaw = target.getYaw();
        float targetPitch = target.getPitch();

        if (!Float.isFinite(currentYaw) || !Float.isFinite(currentPitch)
                || !Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)
                || !Float.isFinite(speed)) {
            reset();
            float safeYaw = Float.isFinite(currentYaw) ? currentYaw : 0f;
            float safePitch = Float.isFinite(currentPitch) ? MathHelper.clamp(currentPitch, -90f, 90f) : 0f;
            return new Rotation(safeYaw, safePitch);
        }

        float clampedSpeed = MathHelper.clamp(speed, 4f, 70f);
        float yawError = wrap(targetYaw - currentYaw);
        float pitchError = MathHelper.clamp(targetPitch, -90f, 90f) - currentPitch;

        float newYaw = currentYaw + yaw.step(yawError, clampedSpeed);
        float newPitch = MathHelper.clamp(
                currentPitch + pitch.step(pitchError, clampedSpeed * 0.8f),
                -90f, 90f
        );

        return new Rotation(newYaw, newPitch);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private static float wrap(float angle) {
        float wrapped = angle % 360f;
        if (wrapped >= 180f) wrapped -= 360f;
        if (wrapped < -180f) wrapped += 360f;
        return wrapped;
    }
}
