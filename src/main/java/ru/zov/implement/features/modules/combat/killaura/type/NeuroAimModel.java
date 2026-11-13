package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class NeuroAimModel {

    // === Входные параметры ===
    public record Input(float yaw, float pitch,
                        float aimYaw, float aimPitch,
                        float targetHeadYaw, float targetHeadPitch,
                        float targetChestYaw, float targetChestPitch,
                        float targetFeetYaw, float targetFeetPitch,
                        boolean engaged, boolean newTarget, boolean hit) {}

    public record Output(float yaw, float pitch) {}

    // === Константы ===
    private static final int HISTORY = 4;
    private static final float PEAK_STEP = 24.0F;
    private static final float MAX_STEP = 40.0F;
    private static final float BALLISTIC_MIN = 4.0F;
    private static final float RETARGET_MIN = 8.0F;
    private static final float TRACK_CONTINUOUS = 2.0F;
    private static final float YAW_DEADBAND = 0.35F;
    private static final float PITCH_DEADBAND = 0.6F;
    private static final float RETURN_FINISH = 0.4F;
    private static final float PITCH_NOISE_RATIO = 1.25F;

    // === Состояние ===
    private final Random rnd;
    private final List<Sub> subs = new ArrayList<>(4);
    private final float[] histYaw = new float[HISTORY];
    private final float[] histPitch = new float[HISTORY];
    private int histCount;
    private int waitTicks;
    private int holdTicks;
    private int sinceStart = 99;
    private boolean wasEngaged;

    // Мультипоинт
    private int pointIndex;             // 0=head, 1=chest, 2=feet
    private int pointSwitchCooldown;    // тиков до возможности сменить
    private float wanderYaw, wanderPitch;   // медленный дрейф вокруг точки
    private float wanderVelYaw, wanderVelPitch;
    private float microJitter;          // шум текущего тика

    // «Персональные» параметры
    private float driftYaw, driftPitch;
    private float speedScale = 1.0F;
    private float noiseScale = 1.0F;
    private int latency = 1;
    private float predictionGain = 0.5F;

    // Фиксация после удара
    private int focusHoldTicks;

    private static final class Sub {
        final float ay;
        final float ap;
        final int duration;
        int age;

        Sub(float ay, float ap, int duration) {
            this.ay = ay;
            this.ap = ap;
            this.duration = duration;
        }
    }

    public NeuroAimModel(long seed) {
        this.rnd = new Random(seed);
    }

    public void reset() {
        subs.clear();
        histCount = 0;
        waitTicks = 0;
        holdTicks = 0;
        sinceStart = 99;
        wasEngaged = false;
        driftYaw = 0f;
        driftPitch = 0f;
        wanderYaw = 0f;
        wanderPitch = 0f;
        wanderVelYaw = 0f;
        wanderVelPitch = 0f;
        pointIndex = 0;
        pointSwitchCooldown = 0;
        focusHoldTicks = 0;
    }

    public Output step(Input in) {
        if (!finite(in.yaw()) || !finite(in.pitch())
                || !finite(in.aimYaw()) || !finite(in.aimPitch())) {
            return new Output(in.yaw(), in.pitch());
        }

        boolean engaged = in.engaged();
        if (engaged != wasEngaged || in.newTarget()) {
            beginPhase(in, engaged);
        }
        wasEngaged = engaged;
        push(in.aimYaw(), in.aimPitch());
        sinceStart++;

        if (waitTicks > 0) waitTicks--;
        if (holdTicks > 0) holdTicks--;
        if (pointSwitchCooldown > 0) pointSwitchCooldown--;
        if (focusHoldTicks > 0) focusHoldTicks--;

        // Выбор точки прицеливания (мультипоинт)
        updatePoint(in, engaged);

        // Смешанная «накопленная» точка = выбранная точка + wander + microjitter
        float aimYaw = pickPointYaw(in);
        float aimPitch = pickPointPitch(in);

        if (in.hit() && engaged) onHit();
        if (waitTicks == 0) plan(in, engaged, aimYaw, aimPitch);

        float dy = 0f, dp = 0f;
        Iterator<Sub> it = subs.iterator();
        while (it.hasNext()) {
            Sub s = it.next();
            float from = smooth((float) s.age / s.duration);
            s.age++;
            float to = smooth((float) s.age / s.duration);
            dy += s.ay * (to - from);
            dp += s.ap * (to - from);
            if (s.age >= s.duration) it.remove();
        }

        // Персональный дрейф + wander
        if (engaged) {
            float oldY = driftYaw, oldP = driftPitch;
            driftYaw = driftYaw * 0.85f + gauss(0.025f * noiseScale);
            driftPitch = driftPitch * 0.85f + gauss(0.02f * noiseScale);
            wanderYaw += wanderVelYaw;
            wanderPitch += wanderVelPitch;
            wanderVelYaw = wanderVelYaw * 0.92f + gauss(0.006f * noiseScale);
            wanderVelPitch = wanderVelPitch * 0.92f + gauss(0.005f * noiseScale);
            wanderYaw = clamp(wanderYaw, -1.5f, 1.5f);
            wanderPitch = clamp(wanderPitch, -1.0f, 1.0f);

            // Микро-дрожь в текущем тике
            microJitter = gauss(0.012f * noiseScale);

            dy += (driftYaw - oldY) + wanderVelYaw + microJitter;
            dp += (driftPitch - oldP) + wanderVelPitch + microJitter * 0.8f;

            // Фиксация после удара: гасим шум
            if (focusHoldTicks > 0) {
                dy *= 0.55f;
                dp *= 0.55f;
            }
        }

        dy = clamp(dy, -MAX_STEP, MAX_STEP);
        dp = clamp(dp, -MAX_STEP, MAX_STEP);
        return new Output(in.yaw() + dy, clamp(in.pitch() + dp, -90f, 90f));
    }

    // ===== Мультипоинт =====
    private void updatePoint(Input in, boolean engaged) {
        if (!engaged) { pointIndex = 0; return; }
        if (pointSwitchCooldown > 0) return;

        // Смена точки: при получении урона — в голову, иначе случайно среди head/chest/feet
        // «Умный» выбор: если ошибка большая (dist > 20) — всегда head, чтобы быстрее навестись
        float distHead = (float) Math.hypot(wrap(in.targetHeadYaw() - in.yaw()),
                in.targetHeadPitch() - in.pitch());

        if (distHead > 20f) {
            pointIndex = 0;
        } else if (in.hit()) {
            // после удара остаёмся на текущей точке
        } else if (rnd.nextFloat() < 0.06f) {
            // с вероятностью 6% на тик меняем точку
            pointIndex = rnd.nextInt(3);
        }
        pointSwitchCooldown = 8 + rnd.nextInt(12);
    }

    private float pickPointYaw(Input in) {
        return switch (pointIndex) {
            case 1 -> in.targetChestYaw();
            case 2 -> in.targetFeetYaw();
            default -> in.targetHeadYaw();
        };
    }

    private float pickPointPitch(Input in) {
        return switch (pointIndex) {
            case 1 -> in.targetChestPitch();
            case 2 -> in.targetFeetPitch();
            default -> in.targetHeadPitch();
        };
    }

    // ===== Реакция и планирование =====
    private void beginPhase(Input in, boolean engaged) {
        float err = (float) Math.hypot(wrap(in.aimYaw() - in.yaw()),
                in.aimPitch() - in.pitch());
        histCount = 0;
        holdTicks = 0;
        pointSwitchCooldown = 0;
        if (engaged) {
            rerollPersonality();
            waitTicks = err < 2.5f ? 0 : reaction(2.5f);
        } else {
            waitTicks = reaction(3.0f);
        }
    }

    private void rerollPersonality() {
        speedScale = (float) Math.exp(0.6 * Math.log(speedScale) + 0.4 * 0.10 * rnd.nextGaussian());
        noiseScale = (float) Math.exp(0.6 * Math.log(noiseScale) + 0.4 * 0.25 * rnd.nextGaussian());
        speedScale = clamp(speedScale, 0.8f, 1.25f);
        noiseScale = clamp(noiseScale, 0.6f, 1.8f);
        latency = rnd.nextFloat() < 0.7f ? 1 : 2;
        predictionGain = 0.6f + 0.3f * rnd.nextFloat();
    }

    private int reaction(float medianTicks) {
        float v = medianTicks * (float) Math.exp(0.35 * rnd.nextGaussian());
        return Math.max(1, Math.min(7, Math.round(v)));
    }

    private void onHit() {
        if (rnd.nextFloat() < 0.45f) {
            subs.add(new Sub(gauss(0.25f * noiseScale), gauss(0.2f * noiseScale), 2));
        }
        focusHoldTicks = 2 + rnd.nextInt(2); // 2–3 тика фиксации
        holdTicks = Math.max(holdTicks, 1 + rnd.nextInt(2));
    }

    private void push(float aimYaw, float aimPitch) {
        if (histCount == 0) {
            for (int i = 0; i < HISTORY; i++) {
                histYaw[i] = aimYaw;
                histPitch[i] = aimPitch;
            }
            histCount = HISTORY;
            return;
        }
        for (int i = HISTORY - 1; i > 0; i--) {
            histYaw[i] = histYaw[i - 1];
            histPitch[i] = histPitch[i - 1];
        }
        histYaw[0] = aimYaw;
        histPitch[0] = aimPitch;
    }

    private void plan(Input in, boolean engaged, float aimYaw, float aimPitch) {
        float remY = 0f, remP = 0f;
        for (Sub s : subs) {
            float rest = 1f - smooth((float) s.age / s.duration);
            remY += s.ay * rest;
            remP += s.ap * rest;
        }

        int lag = engaged ? Math.min(latency, histCount - 1) : 0;
        float velY = 0f, velP = 0f;
        if (engaged) {
            velY = clamp(wrap(histYaw[0] - histYaw[1]), -8f, 8f);
            velP = clamp(histPitch[0] - histPitch[1], -4f, 4f);
        }

        float horizon = lag + 2f;
        float errY = wrap(histYaw[lag] + predictionGain * velY * horizon - (in.yaw() + remY));
        float errP = histPitch[lag] + predictionGain * velP * horizon - (in.pitch() + remP);

        // Добавляем «виртуальную цель» из мультипоинта
        errY += wrap(aimYaw - in.aimYaw());
        errP += (aimPitch - in.aimPitch());

        float dist = (float) Math.hypot(errY, errP);
        boolean busy = !subs.isEmpty();

        if (dist >= BALLISTIC_MIN) {
            if (!busy || dist >= RETARGET_MIN && sinceStart >= 2) {
                startBallistic(errY, errP, dist, engaged);
            }
            return;
        }

        boolean overlap = engaged && dist >= TRACK_CONTINUOUS && sinceStart >= 2;
        if (!overlap && (busy || holdTicks > 0)) return;

        if (!engaged) {
            if (dist > 0.02f) {
                subs.add(new Sub(errY, errP, dist < RETURN_FINISH ? 2 : 3));
                sinceStart = 0;
            }
            return;
        }

        float db = noiseScale;
        if (Math.abs(errY) > YAW_DEADBAND * db || Math.abs(errP) > PITCH_DEADBAND * db) {
            startCorrection(errY, errP, dist);
        }
    }

    private void startBallistic(float errY, float errP, float dist, boolean engaged) {
        float gain, sigma;
        if (engaged) {
            float mean = 1f - 0.04f * Math.min(1f, dist / 60f);
            gain = clamp(mean + gauss(0.035f), 0.88f, 1.08f);
            sigma = 0.04f * dist * noiseScale + 0.1f;
        } else {
            gain = 1f + gauss(0.01f);
            sigma = 0.01f * dist;
        }

        float ay = errY * gain + gauss(sigma);
        float ap = errP * gain + gauss(sigma * PITCH_NOISE_RATIO);
        float fitts = (2f + 1.1f * log2(1f + dist / 4f)) / speedScale
                * (float) Math.exp(0.12 * rnd.nextGaussian());
        int duration = Math.max(3, Math.round(Math.max(fitts, 1.875f * dist / PEAK_STEP)));

        // Дополнительно: на длинных дистанциях больше шанс «перелёта»
        if (dist > 30f && rnd.nextFloat() < 0.3f) {
            ay += gauss(0.15f * errY / dist);
            ap += gauss(0.10f * errP / dist);
        }

        subs.add(new Sub(ay, ap, duration));
        sinceStart = 0;
        holdTicks = duration + latency + rnd.nextInt(2);
    }

    private void startCorrection(float errY, float errP, float dist) {
        float gain = 0.75f + 0.25f * rnd.nextFloat();
        float sigma = 0.04f * dist * noiseScale + 0.03f;
        int duration = (dist < 1.5f ? 2 : 3) + rnd.nextInt(2);
        subs.add(new Sub(errY * gain + gauss(sigma), errP * gain + gauss(sigma * PITCH_NOISE_RATIO), duration));
        sinceStart = 0;
        holdTicks = duration + 1 + rnd.nextInt(3);
    }

    // ===== Утилиты =====
    private float gauss(float sigma) {
        return (float) rnd.nextGaussian() * sigma;
    }

    static float smooth(float t) {
        float x = Math.max(0f, Math.min(1f, t));
        return x * x * x * (10f - 15f * x + 6f * x * x);
    }

    static float wrap(float degrees) {
        float d = degrees % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float log2(float v) {
        return (float) (Math.log(v) / Math.log(2.0));
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static boolean finite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }
}
