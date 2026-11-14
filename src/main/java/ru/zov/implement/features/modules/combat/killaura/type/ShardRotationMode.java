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
 * ShardRotationMode — ротация "под Shard AC" в стиле ht3.
 *
 * Физиология человеческой руки:
 *  1. Reaction time (2–4 тика) при появлении/смене цели.
 *  2. Ballistic submovement с minimum-jerk профилем (5-я производная = 0).
 *     Длительность по Fitts's Law: MT = a + b * log2(2D/W).
 *  3. Overshoot (4–9% на больших углах) + corrective submovement(s).
 *  4. Deadzone (0.18° yaw / 0.25° pitch) — человек не корректирует микроотклонения.
 *  5. Micro-tremor ~9 Гц (физиологический тремор руки).
 *  6. Micro-wobble в tracking — постоянное микро-дыхание.
 *  7. Target-switch delay при смене цели.
 *  8. Всегда микро-кривизна — никаких прямых линий.
 */
public final class ShardRotationMode extends RotationMode {

    private static final float REACTION_MIN = 2.0f;
    private static final float REACTION_MAX = 4.0f;
    private static final float FITTS_A = 0.08f;
    private static final float FITTS_B = 0.11f;
    private static final float PEAK_CAP = 42.0f;
    private static final float DEADZONE_YAW = 0.18f;
    private static final float DEADZONE_PITCH = 0.25f;
    private static final float TREMOR_HZ = 9.0f;
    private static final float TREMOR_AMP = 0.07f;

    private final Random rnd = new Random(System.nanoTime());
    private final List<Sub> subs = new ArrayList<>();
    private final float[] hYaw = new float[6];
    private final float[] hPitch = new float[6];
    private int hCount;
    private LivingEntity lastTarget;
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
        lastTarget = null;
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

        if (targetId != lastTargetId) {
            lastTargetId = targetId;
            lastTarget = living;
            reactionLeft = REACTION_MIN + rnd.nextFloat() * (REACTION_MAX - REACTION_MIN);
            subs.clear();
            hCount = 0;
        }

        push(target.getYaw(), target.getPitch());

        if (reactionLeft > 0f) {
            reactionLeft -= 1f;
            applyTremorAndWobble();
            return new Rotation(current.getYaw(), current.getPitch());
        }

        float tgtYaw   = predictYaw(target.getYaw());
        float tgtPitch = MathHelper.clamp(predictPitch(target.getPitch()), -90f, 90f);

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

        if (dist > 3.0f) {
            if (!busy) startBallistic(errY, errP, dist);
            else if (dist > 12.0f && subs.size() < 2) startBallistic(errY, errP, dist);
        } else if (!busy && dist > DEADZONE_YAW) {
            startCorrection(errY, errP, dist);
        }

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
                if (s.ballistic && s.overshoot) {
                    float miniY = -s.ay * (0.4f + rnd.nextFloat() * 0.5f);
                    float miniP = -s.ap * (0.3f + rnd.nextFloat() * 0.4f);
                    int d = 2 + rnd.nextInt(2);
                    subs.add(new Sub(miniY, miniP, d, false, false));
                }
            }
        }

        tremorPhase += (TREMOR_HZ / 20f) * ((float) Math.PI * 2f);
        if (tremorPhase > (float) Math.PI * 2f) tremorPhase -= (float) Math.PI * 2f;

        wobbleVelY += (rnd.nextFloat() - 0.5f) * 0.015f;
        wobbleVelP += (rnd.nextFloat() - 0.5f) * 0.012f;
        wobbleVelY *= 0.94f;
        wobbleVelP *= 0.94f;
        wobbleYaw   = MathHelper.clamp(wobbleYaw   + wobbleVelY, -0.6f, 0.6f);
        wobblePitch = MathHelper.clamp(wobblePitch + wobbleVelP, -0.5f, 0.5f);

        float tremorY = (float) Math.sin(tremorPhase) * TREMOR_AMP;
        float tremorP = (float) Math.cos(tremorPhase * 1.13f) * TREMOR_AMP * 0.7f;
        float gaussY  = (float) rnd.nextGaussian() * 0.02f;
        float gaussP  = (float) rnd.nextGaussian() * 0.015f;

        dy += wobbleVelY * 0.6f + tremorY + gaussY;
        dp += wobbleVelP * 0.6f + tremorP + gaussP;

        dy = clamp(dy, -PEAK_CAP, PEAK_CAP);
        dp = clamp(dp, -PEAK_CAP, PEAK_CAP);

        return new Rotation(
                current.getYaw() + dy,
                MathHelper.clamp(current.getPitch() + dp, -90f, 90f));
    }

    private void startBallistic(float errY, float errP, float dist) {
        float curvature = (rnd.nextFloat() - 0.5f) * 0.18f;
        float perpY = -errP / dist;
        float perpP =  errY / dist;
        float curveOffsetY = perpY * dist * curvature;
        float curveOffsetP = perpP * dist * curvature;

        float widthDeg = 4.0f + rnd.nextFloat() * 1.5f;
        float id = (float) (Math.log(2.0 * dist / widthDeg) / Math.log(2.0));
        float mtSeconds = FITTS_A + FITTS_B * Math.max(0f, id);
        int duration = Math.max(3, Math.min(11, Math.round(mtSeconds * 20f)));

        float overshootFactor = 0f;
        boolean overshoot = false;
        if (dist > 20f && rnd.nextFloat() < 0.55f) {
            overshootFactor = 0.04f + rnd.nextFloat() * 0.05f;
            overshoot = true;
        }

        float ay = errY * (1f + overshootFactor) + curveOffsetY;
        float ap = errP * (1f + overshootFactor) + curveOffsetP;

        float sigma = 0.02f * dist + 0.1f;
        ay += (float) rnd.nextGaussian() * sigma;
        ap += (float) rnd.nextGaussian() * sigma * 1.15f;

        subs.add(new Sub(ay, ap, duration, true, overshoot));
    }

    private void startCorrection(float errY, float errP, float dist) {
        float gain = 0.7f + 0.25f * rnd.nextFloat();
        float sigma = 0.02f * dist + 0.05f;
        int duration = 2 + rnd.nextInt(2);

        float ay = errY * gain + (float) rnd.nextGaussian() * sigma;
        float ap = errP * gain + (float) rnd.nextGaussian() * sigma * 1.15f;

        subs.add(new Sub(ay, ap, duration, false, false));
    }

    private void applyTremorAndWobble() {
        tremorPhase += (TREMOR_HZ / 20f) * ((float) Math.PI * 2f);
        if (tremorPhase > (float) Math.PI * 2f) tremorPhase -= (float) Math.PI * 2f;
        wobbleVelY += (rnd.nextFloat() - 0.5f) * 0.015f;
        wobbleVelP += (rnd.nextFloat() - 0.5f) * 0.012f;
        wobbleVelY *= 0.94f;
        wobbleVelP *= 0.94f;
    }

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
        float lag = 1.0f + rnd.nextFloat() * 0.8f;
        return targetYaw + v * lag;
    }

    private float predictPitch(float targetPitch) {
        if (hCount < 2) return targetPitch;
        float v = hPitch[0] - hPitch[1];
        float lag = 1.0f + rnd.nextFloat() * 0.6f;
        return targetPitch + v * lag;
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * t * (10f - 15f * t + 6f * t * t);
    }

    private static float wrap(float degrees) {
        degrees %= 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
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
