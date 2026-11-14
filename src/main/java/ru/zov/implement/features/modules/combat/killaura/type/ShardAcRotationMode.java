package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * ShardRotationMode — быстрая ротация с физиологией ht3.
 *
 * Отличия от "bypass-only" версии:
 *  - Reaction 0.4–1.2 тика (8–24 мс) — почти как у ht3.
 *  - Fitts ускорен: длительность ballistic 2–5 тиков вместо 5–11.
 *  - Overshoot только на очень больших углах (>40°) и реже (30%).
 *  - Deadzone меньше (0.08/0.12) — точнее наводится.
 *  - Peak cap 48°/тик — быстрый рывок.
 *  - Сохранены: tremor (9 Гц), wobble (дыхание), curvature (±6%),
 *    signal-dependent noise, target-switch delay.
 *
 * ВАЖНО: в Aura.updateRotation() для SHARD должно быть i3 = 3
 * (тиков на наводку), иначе ротация будет ждать 5 тиков.
 */
public final class ShardRotationMode extends RotationMode {

    // === Константы (ht3-ускоренные) ===
    private static final float REACTION_MIN = 0.4f;
    private static final float REACTION_MAX = 1.2f;
    private static final float FITTS_A = 0.025f;
    private static final float FITTS_B = 0.055f;
    private static final float PEAK_CAP = 48.0f;
    private static final float DEADZONE_YAW = 0.08f;
    private static final float DEADZONE_PITCH = 0.12f;
    private static final float TREMOR_HZ = 9.0f;
    private static final float TREMOR_AMP = 0.055f;
    private static final float SWITCH_DELAY_MAX = 1.2f;

    // === Состояние ===
    private final Random rnd = new Random(System.nanoTime());
    private final List<Sub> subs = new ArrayList<>();
    private final float[] hYaw = new float[6];
    private final float[] hPitch = new float[6];
    private int hCount;
    private int lastTargetId = -1;
    private float reactionLeft;
    private float tremorPhase;
    private float wobbleYaw;
    private float wobblePitch;
    private float wobbleVelY;
    private float wobbleVelP;

    public ShardRotationMode() {
        super("Shard");
    }

    public void reset() {
        subs.clear();
        hCount = 0;
        lastTargetId = -1;
        reactionLeft = 0f;
        tremorPhase = 0f;
        wobbleYaw = 0f;
        wobblePitch = 0f;
        wobbleVelY = 0f;
        wobbleVelP = 0f;
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec, Entity entity) {
        if (current == null || target == null) return current;

        if (!finite(current.getYaw()) || !finite(current.getPitch())
                || !finite(target.getYaw()) || !finite(target.getPitch())) {
            reset();
            return current;
        }

        LivingEntity living = entity instanceof LivingEntity le ? le : null;
        int targetId = living != null ? living.getId() : -1;

        // Смена цели — короткая реакция (0.4–1.2 тика)
        if (targetId != lastTargetId) {
            lastTargetId = targetId;
            reactionLeft = REACTION_MIN + rnd.nextFloat() * (REACTION_MAX - REACTION_MIN);
            subs.clear();
            hCount = 0;
        }

        push(target.getYaw(), target.getPitch());

        // Пока не истекла реакция — только tremor/wobble, без движения
        if (reactionLeft > 0f) {
            reactionLeft -= 1f;
            applyTremorAndWobble();
            return new Rotation(current.getYaw(), current.getPitch());
        }

        // Prediction с учётом latency глаза (1 тик)
        float tgtYaw   = predictYaw(target.getYaw());
        float tgtPitch = MathHelper.clamp(predictPitch(target.getPitch()), -90f, 90f);

        // Накопленное смещение от активных submovements
        float remY = 0f, remP = 0f;
        for (Sub s : subs) {
            float rest = 1f - smooth((float) s.age / s.duration);
            remY += s.ay * rest;
            remP += s.ap * rest;
        }

        float errY = wrap(tgtYaw   - (current.getYaw()   + remY));
        float errP = tgtPitch      - (current.getPitch() + remP);
        float dist = (float) Math.hypot(errY, errP);
        boolean busy = !subs.isEmpty();

        // === Решения по движению ===
        if (dist > 2.5f) {
            if (!busy) {
                startBallistic(errY, errP, dist);
            } else if (dist > 10.0f && subs.size() < 2) {
                // Ошибка выросла — досылаем коррекцию
                startBallistic(errY, errP, dist);
            }
        } else if (!busy && dist > DEADZONE_YAW) {
            startCorrection(errY, errP, dist);
        }

        // === Применение всех активных submovements ===
        float dy = 0f, dp = 0f;
        Iterator<Sub> it = subs.iterator();
        while (it.hasNext()) {
            Sub s = it.next();
            float from = smooth((float) s.age / s.duration);
            s.age++;
            float to = smooth((float) s.age / s.duration);
            dy += s.ay * (to - from);
            dp += s.ap * (to - from);
            if (s.age >= s.duration) {
                it.remove();
                // Overshoot correction только если реально был
                if (s.ballistic && s.overshoot) {
                    float miniY = -s.ay * (0.5f + rnd.nextFloat() * 0.4f);
                    float miniP = -s.ap * (0.4f + rnd.nextFloat() * 0.3f);
                    int d = 1 + rnd.nextInt(2); // 1–2 тика (быстро!)
                    subs.add(new Sub(miniY, miniP, d, false, false));
                }
            }
        }

        // === Tremor + wobble + noise ===
        tremorPhase += (TREMOR_HZ / 20f) * ((float) Math.PI * 2f);
        if (tremorPhase > (float) Math.PI * 2f) tremorPhase -= (float) Math.PI * 2f;

        wobbleVelY += (rnd.nextFloat() - 0.5f) * 0.012f;
        wobbleVelP += (rnd.nextFloat() - 0.5f) * 0.010f;
        wobbleVelY *= 0.94f;
        wobbleVelP *= 0.94f;
        wobbleYaw   = MathHelper.clamp(wobbleYaw   + wobbleVelY, -0.4f, 0.4f);
        wobblePitch = MathHelper.clamp(wobblePitch + wobbleVelP, -0.35f, 0.35f);

        float tremorY = (float) Math.sin(tremorPhase) * TREMOR_AMP;
        float tremorP = (float) Math.cos(tremorPhase * 1.13f) * TREMOR_AMP * 0.7f;
        float gaussY  = (float) rnd.nextGaussian() * 0.015f;
        float gaussP  = (float) rnd.nextGaussian() * 0.012f;

        dy += wobbleVelY * 0.5f + tremorY + gaussY;
        dp += wobbleVelP * 0.5f + tremorP + gaussP;

        // Лимит
        dy = clamp(dy, -PEAK_CAP, PEAK_CAP);
        dp = clamp(dp, -PEAK_CAP, PEAK_CAP);

        return new Rotation(
                current.getYaw() + dy,
                MathHelper.clamp(current.getPitch() + dp, -90f, 90f));
    }

    // === Ballistic ===
    private void startBallistic(float errY, float errP, float dist) {
        // Микро-кривизна ±6%
        float curvature = (rnd.nextFloat() - 0.5f) * 0.12f;
        float perpY = -errP / dist;
        float perpP =  errY / dist;
        float curveOffsetY = perpY * dist * curvature;
        float curveOffsetP = perpP * dist * curvature;

        // Fitts (ускоренный)
        float widthDeg = 4.0f + rnd.nextFloat() * 1.5f;
        float id = (float) (Math.log(2.0 * dist / widthDeg) / Math.log(2.0));
        float mtSeconds = FITTS_A + FITTS_B * Math.max(0f, id);
        int duration = Math.max(2, Math.min(5, Math.round(mtSeconds * 20f)));

        // Overshoot только на очень больших углах
        float overshootFactor = 0f;
        boolean overshoot = false;
        if (dist > 40f && rnd.nextFloat() < 0.30f) {
            overshootFactor = 0.015f + rnd.nextFloat() * 0.020f; // 1.5–3.5%
            overshoot = true;
        }

        float ay = errY * (1f + overshootFactor) + curveOffsetY;
        float ap = errP * (1f + overshootFactor) + curveOffsetP;

        // Signal-dependent noise (меньше, чем раньше)
        float sigma = 0.015f * dist + 0.08f;
        ay += (float) rnd.nextGaussian() * sigma;
        ap += (float) rnd.nextGaussian() * sigma * 1.1f;

        subs.add(new Sub(ay, ap, duration, true, overshoot));
    }

    // === Correction (малые углы) ===
    private void startCorrection(float errY, float errP, float dist) {
        float gain = 0.85f + 0.15f * rnd.nextFloat(); // 85–100% — почти доходит до цели
        float sigma = 0.015f * dist + 0.04f;
        int duration = 2 + rnd.nextInt(2); // 2–3 тика

        float ay = errY * gain + (float) rnd.nextGaussian() * sigma;
        float ap = errP * gain + (float) rnd.nextGaussian() * sigma * 1.1f;

        subs.add(new Sub(ay, ap, duration, false, false));
    }

    // === Tremor/wobble во время реакции ===
    private void applyTremorAndWobble() {
        tremorPhase += (TREMOR_HZ / 20f) * ((float) Math.PI * 2f);
        if (tremorPhase > (float) Math.PI * 2f) tremorPhase -= (float) Math.PI * 2f;
        wobbleVelY += (rnd.nextFloat() - 0.5f) * 0.012f;
        wobbleVelP += (rnd.nextFloat() - 0.5f) * 0.010f;
        wobbleVelY *= 0.94f;
        wobbleVelP *= 0.94f;
    }

    // === История / prediction ===
    private void push(float yaw, float pitch) {
        if (hCount == 0) {
            for (int i = 0; i < hYaw.length; i++) { hYaw[i] = yaw; hPitch[i] = pitch; }
            hCount = hYaw.length;
            return;
        }
        for (int i = hYaw.length - 1; i > 0; i--) {
            hYaw[i] = hYaw[i - 1];
            hPitch[i] = hPitch[i - 1];
        }
        hYaw[0] = yaw;
        hPitch[0] = pitch;
    }

    private float predictYaw(float targetYaw) {
        if (hCount < 2) return targetYaw;
        float v = wrap(hYaw[0] - hYaw[1]);
        return targetYaw + v * 0.6f; // 0.6 тика latency
    }

    private float predictPitch(float targetPitch) {
        if (hCount < 2) return targetPitch;
        float v = hPitch[0] - hPitch[1];
        return targetPitch + v * 0.5f;
    }

    // === Утилиты ===
    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * t * (10f - 15f * t + 6f * t * t);
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static boolean finite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private static final class Sub {
        final float ay;
        final float ap;
        final int duration;
        final boolean ballistic;
        final boolean overshoot;
        int age;

        Sub(float ay, float ap, int duration, boolean ballistic, boolean overshoot) {
            this.ay = ay;
            this.ap = ap;
            this.duration = duration;
            this.ballistic = ballistic;
            this.overshoot = overshoot;
        }
    }
}
