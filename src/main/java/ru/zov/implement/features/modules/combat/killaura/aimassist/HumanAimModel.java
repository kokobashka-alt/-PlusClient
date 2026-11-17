package ru.zov.implement.features.modules.combat.aimassist;

import java.util.random.RandomGenerator;

/**
 * Формульная модель «человеческого» аим-ассиста. Не зависит от Minecraft: на вход кадр с положением глаз, цели и
 * собственным движением мыши игрока, на выход добавка к повороту в градусах (кратная шагу мыши).
 *
 * <p>Принципы (все числа выбраны автором порта, под конкретный античит не калибровались):
 * <ul>
 *   <li>добавка всегда доля от собственного движения мыши игрока и только в сторону цели — без ввода её нет;</li>
 *   <li>точка прицеливания не центр хитбокса: у каждой цели своя точка в верхней части туловища, она медленно плавает;</li>
 *   <li>внутри «полосы попадания» вокруг точки ассист равен нулю, поэтому прицел не залипает;</li>
 *   <li>реакция 120..380 мс после входа цели в конус, затем фазы «работа / отдых», случайные паузы, плавный набор;</li>
 *   <li>усиление модулируется медленным шумом (процесс Орнштейна—Уленбека), а не белым шумом по кадрам;</li>
 *   <li>питч — слабая добавка с жёстким потолком в градусах в секунду;</li>
 *   <li>результат выдаётся целыми шагами мыши (накопитель остатка), чтобы поворот оставался на сетке чувствительности.</li>
 * </ul>
 */
public final class HumanAimModel {
    public enum Phase { IDLE, ARMING, ACTIVE, RESTING }

    public static final class Settings {
        /** Сила 0..1. */
        public float strength = 0.55F;
        /** Сила по вертикали 0..1. */
        public float vertical = 0.20F;
        /** Конус работы, градусы. */
        public float coneDegrees = 12.0F;
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
        /** Собственное движение мыши игрока за кадр, градусы (знак как у yaw/pitch). */
        public float inputYaw;
        public float inputPitch;
        /** Градусов на один отсчёт мыши. */
        public float quantum = 0.15F;
    }

    public static final class Output {
        public float yaw;
        public float pitch;
    }

    // --- константы модели ---
    private static final float GAIN_YAW = 0.50F;
    private static final float GAIN_PITCH = 0.50F;
    private static final float MAX_RATIO_YAW = 0.38F;
    private static final float MAX_RATIO_PITCH = 0.22F;
    private static final float MAX_YAW_RATE = 70.0F;
    private static final float MAX_PITCH_RATE = 6.0F;
    private static final float ACT_LOW = 3.0F;
    private static final float ACT_HIGH = 18.0F;
    private static final float FAST_LOW = 70.0F;
    private static final float FAST_HIGH = 260.0F;
    private static final float LAPSE_RATE = 0.22F;
    private static final float RISE_TAU = 0.12F;
    private static final float FALL_TAU = 0.08F;
    private static final float MIN_V = 0.54F;
    private static final float MAX_V = 0.88F;
    private static final float MAX_U = 0.30F;

    private final RandomGenerator rng;

    // персона цели
    private boolean hasTarget;
    private float baseU;
    private float baseV;
    private float driftU;
    private float driftV;
    private float bandYaw;
    private float bandPitch;
    private float reaction;
    private float holdMax;

    // фазы
    private Phase phase = Phase.IDLE;
    private float phaseTime;
    private float restDuration;
    private float lapseLeft;
    private float engage;

    // шум усиления и остатки
    private float gainLogYaw;
    private float gainLogPitch;
    private float residualYaw;
    private float residualPitch;
    private float budgetYaw;
    private float budgetPitch;

    public HumanAimModel(RandomGenerator rng) {
        this.rng = rng;
    }

    public Phase phase() {
        return phase;
    }

    public float engage() {
        return engage;
    }

    public float baseVerticalFraction() {
        return baseV;
    }

    public float baseLateralFraction() {
        return baseU;
    }

    /** Новая цель: выбираем персону (точка, полоса попадания, реакция, длительность работы). */
    public void acquire() {
        hasTarget = true;
        baseV = clamp(0.70F + 0.08F * (float) rng.nextGaussian(), MIN_V, MAX_V);
        baseU = clamp(0.13F * (float) rng.nextGaussian(), -MAX_U, MAX_U);
        driftU = 0.0F;
        driftV = 0.0F;
        bandYaw = 0.42F + 0.26F * (float) rng.nextDouble();
        bandPitch = 0.38F + 0.24F * (float) rng.nextDouble();
        reaction = clamp(0.19F * (float) Math.exp(0.28 * rng.nextGaussian()), 0.12F, 0.38F);
        holdMax = 0.9F + 1.3F * (float) rng.nextDouble();
        phase = Phase.IDLE;
        phaseTime = 0.0F;
        lapseLeft = 0.0F;
        engage = 0.0F;
        gainLogYaw = 0.0F;
        gainLogPitch = 0.0F;
        residualYaw = 0.0F;
        residualPitch = 0.0F;
        budgetYaw = 0.0F;
        budgetPitch = 0.0F;
    }

    public void clearTarget() {
        hasTarget = false;
        phase = Phase.IDLE;
        phaseTime = 0.0F;
        lapseLeft = 0.0F;
        engage = 0.0F;
        residualYaw = 0.0F;
        residualPitch = 0.0F;
        budgetYaw = 0.0F;
        budgetPitch = 0.0F;
    }

    /** Нейтральная точка для сравнения кандидатов до выбора персоны: верх туловища. */
    public static double[] neutralPoint(double feetX, double feetY, double feetZ, float height) {
        return new double[]{feetX, feetY + 0.70 * height, feetZ};
    }

    /** Текущая точка прицеливания цели (с учётом медленного дрейфа). */
    public double[] aimPoint(Input in) {
        float u = clamp(baseU + driftU, -MAX_U, MAX_U);
        float v = clamp(baseV + driftV, MIN_V, MAX_V);
        double dirX = in.feetX - in.eyeX;
        double dirZ = in.feetZ - in.eyeZ;
        double len = Math.hypot(dirX, dirZ);
        double perpX = len < 1.0E-6 ? 1.0 : -dirZ / len;
        double perpZ = len < 1.0E-6 ? 0.0 : dirX / len;
        return new double[]{in.feetX + perpX * u * in.width, in.feetY + v * in.height, in.feetZ + perpZ * u * in.width};
    }

    public Output step(Input in, Settings settings, Output out) {
        out.yaw = 0.0F;
        out.pitch = 0.0F;
        if (!hasTarget) {
            return out;
        }

        float dt = clamp(in.dt, 0.001F, 0.05F);
        float sqrtDt = (float) Math.sqrt(dt);

        driftU = clamp(driftU - 0.9F * driftU * dt + 0.045F * sqrtDt * (float) rng.nextGaussian(), -0.12F, 0.12F);
        driftV = clamp(driftV - 0.9F * driftV * dt + 0.045F * sqrtDt * (float) rng.nextGaussian(), -0.12F, 0.12F);
        gainLogYaw += -1.4F * gainLogYaw * dt + 0.5F * sqrtDt * (float) rng.nextGaussian();
        gainLogPitch += -1.4F * gainLogPitch * dt + 0.5F * sqrtDt * (float) rng.nextGaussian();
        float gainYaw = (float) Math.exp(clamp(gainLogYaw, -0.5F, 0.4F));
        float gainPitch = (float) Math.exp(clamp(gainLogPitch, -0.5F, 0.4F));

        double[] p = aimPoint(in);
        double dx = p[0] - in.eyeX;
        double dy = p[1] - in.eyeY;
        double dz = p[2] - in.eyeZ;
        double horizontal = Math.hypot(dx, dz);
        double distance = Math.max(0.3, Math.sqrt(dx * dx + dy * dy + dz * dz));
        float errorYaw = wrap((float) Math.toDegrees(Math.atan2(-dx, dz)) - in.yaw);
        float errorPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal))) - in.pitch;
        float cone = Math.max(1.0F, settings.coneDegrees);
        float offAxis = (float) Math.hypot(errorYaw, errorPitch);
        boolean inCone = offAxis <= cone;

        updatePhase(dt, inCone);

        float engageTarget = phase == Phase.ACTIVE && lapseLeft <= 0.0F ? 1.0F : 0.0F;
        float tau = engageTarget > engage ? RISE_TAU : FALL_TAU;
        engage += (engageTarget - engage) * (1.0F - (float) Math.exp(-dt / tau));
        if (engage < 0.001F) {
            residualYaw = 0.0F;
            residualPitch = 0.0F;
            budgetYaw = 0.0F;
            budgetPitch = 0.0F;
            return out;
        }

        // полоса попадания вокруг точки; не выходит за хитбокс
        float u = clamp(baseU + driftU, -MAX_U, MAX_U);
        float v = clamp(baseV + driftV, MIN_V, MAX_V);
        float halfW = Math.max(0.08F * in.width, Math.min(0.5F * in.width * bandYaw, 0.5F * in.width - Math.abs(u) * in.width));
        float halfH = Math.max(0.05F * in.height, Math.min(0.5F * in.height * bandPitch, 0.8F * Math.min(v, 1.0F - v) * in.height));
        float bandYawDeg = (float) Math.toDegrees(Math.atan2(halfW, distance));
        float bandPitchDeg = (float) Math.toDegrees(Math.atan2(halfH, distance));

        float coneWeight = 1.0F - smoothstep(0.55F * cone, cone, offAxis);
        float ix = in.inputYaw;
        float iy = in.inputPitch;
        float rateYaw = Math.abs(ix) / dt;
        float fast = 1.0F - smoothstep(FAST_LOW, FAST_HIGH, rateYaw);
        float strength = clamp(settings.strength, 0.0F, 1.0F);
        float vertical = clamp(settings.vertical, 0.0F, 1.0F);

        float assistYaw = 0.0F;
        float outsideYaw = deadKnee(errorYaw, bandYawDeg);
        if (ix * outsideYaw > 0.0F) {
            float act = smoothstep(ACT_LOW, ACT_HIGH, rateYaw);
            float h0 = Math.max(0.8F, 0.9F * bandYawDeg);
            float shape = Math.abs(outsideYaw) / (Math.abs(outsideYaw) + h0);
            float remaining = Math.max(0.0F, Math.abs(outsideYaw) - Math.abs(ix));
            float pull = GAIN_YAW * strength * engage * coneWeight * fast * act * shape * gainYaw * Math.abs(ix);
            pull = Math.min(pull, Math.min(MAX_RATIO_YAW * Math.abs(ix), Math.min(MAX_YAW_RATE * dt, 0.6F * remaining)));
            assistYaw = Math.copySign(pull, outsideYaw);
        }

        float assistPitch = 0.0F;
        float outsidePitch = deadKnee(errorPitch, bandPitchDeg);
        float activity = Math.max(Math.abs(iy), 0.25F * Math.abs(ix));
        boolean pitchDirectionOk = Math.abs(iy) < 1.0E-4F || iy * outsidePitch > 0.0F;
        if (outsidePitch != 0.0F && pitchDirectionOk) {
            float act = smoothstep(ACT_LOW, ACT_HIGH, activity / dt);
            float h0 = Math.max(0.6F, 0.9F * bandPitchDeg);
            float shape = Math.abs(outsidePitch) / (Math.abs(outsidePitch) + h0);
            float pull = GAIN_PITCH * vertical * engage * coneWeight * fast * act * shape * gainPitch * activity;
            pull = Math.min(pull, Math.min(MAX_RATIO_PITCH * activity, Math.min(MAX_PITCH_RATE * dt, 0.6F * Math.abs(outsidePitch))));
            assistPitch = Math.copySign(pull, outsidePitch);
        }

        float q = Math.max(1.0E-6F, in.quantum);
        out.yaw = quantize(assistYaw, ix, q, true);
        out.pitch = quantize(assistPitch, Math.max(Math.abs(iy), Math.abs(ix)), q, false);
        return out;
    }

    private void updatePhase(float dt, boolean inCone) {
        if (!inCone) {
            phase = Phase.IDLE;
            phaseTime = 0.0F;
            lapseLeft = 0.0F;
            return;
        }

        switch (phase) {
            case IDLE -> {
                phase = Phase.ARMING;
                phaseTime = 0.0F;
            }
            case ARMING -> {
                phaseTime += dt;
                if (phaseTime >= reaction) {
                    phase = Phase.ACTIVE;
                    phaseTime = 0.0F;
                }
            }
            case ACTIVE -> {
                phaseTime += dt;
                if (lapseLeft > 0.0F) {
                    lapseLeft -= dt;
                } else if (rng.nextDouble() < LAPSE_RATE * dt) {
                    lapseLeft = 0.15F + 0.30F * (float) rng.nextDouble();
                }

                if (phaseTime >= holdMax) {
                    phase = Phase.RESTING;
                    phaseTime = 0.0F;
                    restDuration = 0.35F + 0.55F * (float) rng.nextDouble();
                }
            }
            case RESTING -> {
                phaseTime += dt;
                if (phaseTime >= restDuration) {
                    phase = Phase.ACTIVE;
                    phaseTime = 0.0F;
                    holdMax = 0.9F + 1.3F * (float) rng.nextDouble();
                }
            }
        }
    }

    /**
     * Накопитель остатка и бюджет шагов. Добавка выдаётся целыми шагами мыши. Бюджет пополняется только собственным
     * движением игрока (40% от его шагов, максимум 3), поэтому в долгосрочном среднем добавка не больше 40% ввода и
     * без ввода её нет; за кадр выдаётся не больше max(1, 40% шагов кадра). Остаток держится в пределах двух шагов.
     */
    private float quantize(float assist, float ownMovement, float q, boolean yawAxis) {
        float ownCounts = Math.abs(ownMovement) / q;
        float budget = Math.min(3.0F, (yawAxis ? budgetYaw : budgetPitch) + 0.4F * ownCounts);
        float residual = clamp((yawAxis ? residualYaw : residualPitch) + assist, -2.0F * q, 2.0F * q);
        int wanted = Math.round(residual / q);
        int allowed = Math.min((int) Math.floor(budget), Math.max(1, (int) Math.ceil(0.4F * ownCounts)));
        int counts = (int) Math.copySign(Math.min(Math.abs(wanted), allowed), wanted);
        float emitted = counts * q;
        residual -= emitted;
        budget -= Math.abs(counts);
        if (yawAxis) {
            residualYaw = residual;
            budgetYaw = budget;
        } else {
            residualPitch = residual;
            budgetPitch = budget;
        }
        return emitted;
    }

    // --- чистые функции ---

    public static float wrap(float angle) {
        float a = angle % 360.0F;
        if (a >= 180.0F) {
            a -= 360.0F;
        }
        if (a < -180.0F) {
            a += 360.0F;
        }
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

    /** Мёртвая зона радиуса r с плавным коленом: внутри 0, дальше квадратичный подъём и затем линейный рост. */
    public static float deadKnee(float x, float r) {
        float a = Math.abs(x);
        float radius = Math.max(0.05F, r);
        float knee = 0.5F * radius;
        float o;
        if (a <= radius) {
            o = 0.0F;
        } else if (a < radius + knee) {
            float d = a - radius;
            o = d * d / (2.0F * knee);
        } else {
            o = a - radius - 0.5F * knee;
        }
        return Math.copySign(o, x);
    }
}
