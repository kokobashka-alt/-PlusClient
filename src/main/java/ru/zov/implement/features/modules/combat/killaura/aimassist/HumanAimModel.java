package ru.zov.implement.features.modules.combat.aimassist;

import java.util.random.RandomGenerator;

/**
 * Исправленная легитная модель аим-ассиста.
 * Удовлетворяет требованиям: мгновенная реакция, фокус на центр хитбокса,
 * постоянное удержание цели и естественная человеческая плавность без ИИ-патернов.
 */
public final class HumanAimModel {

    public static final class Settings {
        public float strength = 0.65F;
        public float vertical = 0.40F;
        public float coneDegrees = 15.0F;
    }

    public static final class Input {
        public float dt;
        public float yaw;
        public float pitch;
        public double eyeX;
        public double eyeY;
        public double eyeZ;
        public double feetX;
        public double feetY;
        public double feetZ;
        public float width = 0.6F;
        public float height = 1.8F;
        public float inputYaw;
        public float inputPitch;
        public float quantum = 0.15F;
    }

    public static final class Output {
        public float yaw;
        public float pitch;
    }

    private final RandomGenerator rng;

    // Шум для рандомизации движения, чтобы избежать детекта по идеальным интерполяциям
    private float noiseYaw;
    private float noisePitch;
    private float residualYaw;
    private float residualPitch;

    public HumanAimModel(RandomGenerator rng) {
        this.rng = rng;
    }

    public void acquire() {
        noiseYaw = 0.0F;
        noisePitch = 0.0F;
        residualYaw = 0.0F;
        residualPitch = 0.0F;
    }

    public void clearTarget() {
        residualYaw = 0.0F;
        residualPitch = 0.0F;
    }

    /** Точное попадание в центр хитбокса цели */
    public static double[] aimPoint(Input in) {
        return new double[]{
                in.feetX,
                in.feetY + (in.height * 0.5), // Центр по высоте
                in.feetZ
        };
    }

    public Output step(Input in, Settings settings, Output out) {
        out.yaw = 0.0F;
        out.pitch = 0.0F;

        float dt = clamp(in.dt, 0.001F, 0.05F);
        float sqrtDt = (float) Math.sqrt(dt);

        // Генерация легкого человеческого шума (Орнштейн-Уленбек)
        noiseYaw += -2.0F * noiseYaw * dt + 0.8F * sqrtDt * (float) rng.nextGaussian();
        noisePitch += -2.0F * noisePitch * dt + 0.8F * sqrtDt * (float) rng.nextGaussian();

        double[] p = aimPoint(in);
        double dx = p[0] - in.eyeX;
        double dy = p[1] - in.eyeY;
        double dz = p[2] - in.eyeZ;
        double horizontal = Math.hypot(dx, dz);
        
        float errorYaw = wrap((float) Math.toDegrees(Math.atan2(-dx, dz)) - in.yaw);
        float errorPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal))) - in.pitch;

        float cone = Math.max(1.0F, settings.coneDegrees);
        float offAxis = (float) Math.hypot(errorYaw, errorPitch);
        
        // Если цель вне конуса обзора — не вмешиваемся
        if (offAxis > cone) {
            return out;
        }

        float strength = clamp(settings.strength, 0.0F, 1.0F);
        float vertical = clamp(settings.vertical, 0.0F, 1.0F);

        // Плавный коэффициент в зависимости от удаленности от центра конуса (чем ближе, тем стабильнее)
        float proximityFactor = 1.0F - smoothstep(0.0F, cone, offAxis);
        
        // Моментальный расчет доворота с учетом движения мыши игрока
        float baseSpeedYaw = Math.abs(in.inputYaw) + 0.35F;
        float baseSpeedPitch = Math.abs(in.inputPitch) + 0.35F;

        // Формула доводки (без задержек и мертвых зон для постоянного удержания)
        float pullYaw = errorYaw * (0.18F + 0.45F * strength + 0.15F * noiseYaw) * proximityFactor;
        float pullPitch = errorPitch * (0.18F + 0.45F * vertical + 0.15F * noisePitch) * proximityFactor;

        // Ограничение максимальной скорости поворота для легитимности (чтобы не было резких фликов)
        float maxYawRate = 45.0F * baseSpeedYaw * dt * 60.0F;
        float maxPitchRate = 25.0F * baseSpeedPitch * dt * 60.0F;
        
        pullYaw = clamp(pullYaw, -maxYawRate, maxYawRate);
        pullPitch = clamp(pullPitch, -maxPitchRate, maxPitchRate);

        // Квантование (приведение к шагам сетки мыши)
        float q = Math.max(1.0E-6F, in.quantum);
        out.yaw = quantize(pullYaw, q, true);
        out.pitch = quantize(pullPitch, q, false);

        return out;
    }

    private float quantize(float assist, float q, boolean yawAxis) {
        float residual = clamp((yawAxis ? residualYaw : residualPitch) + assist, -2.0F * q, 2.0F * q);
        int wanted = Math.round(residual / q);
        float emitted = wanted * q;
        residual -= emitted;

        if (yawAxis) {
            residualYaw = residual;
        } else {
            residualPitch = residual;
        }
        return emitted;
    }

    public static float wrap(float angle) {
        float a = angle % 360.0F;
        if (a >= 180.0F) a -= 360.0F;
        if (a < -180.0F) a += 360.0F;
        return a;
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) {
            return x >= edge1 ? 1.0F : 0.0F;
        }
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
