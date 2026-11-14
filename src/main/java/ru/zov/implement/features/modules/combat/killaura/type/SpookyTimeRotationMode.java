package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Порт fun.shpak.SpookyTimeRotation под ru.zov.
 *
 * Логика (все константы — из оригинала):
 *  - Каждые 25–50 мс пересчитываются параметры (max yaw speed 142..200, angle err 34..54,
 *    vel limit 7..14, drift ±0.04..0.12, deadzone 0.4..0.9).
 *  - Точка прицеливания: mace → 0.25 (низко) или 0.9 (высоко) в зависимости от высоты цели;
 *    не mace → heightMul (0.34..0.65 или 0.54..0.72 после удара) + offsetX/Z.
 *  - Snap-back: если rand() > rand(0.43..0.75) → новая цель; иначе в течение holdMs
 *    (180–240 мс после удара) держим старые yaw/pitch.
 *  - Fade: если луч не попадает в хитбокс — нарастает, уменьшает скорость.
 *  - Итоговые delta yaw/pitch: min(vel, minStep) * множитель, с учётом drift.
 *
 * `onAttack()` нужно вызывать из Aura.attackTarget().
 */
public final class SpookyTimeRotationMode extends RotationMode {

    // === Параметры ===
    private long paramsUpdateMs;
    private long paramsIntervalMs;
    private float maxYawSpeed;
    private float maxAngleErr;
    private float velLimit;
    private float driftYaw;
    private float deadzone;

    // === Состояние snap/switch ===
    private float switchTimer;
    private float snapYaw;
    private float snapPitch;
    private long lastAttackMs;
    private long holdMs;

    // === Скорости ===
    private float velYawSm;
    private float velPitchSm;

    // === Fade ===
    private long missSinceMs;
    private float fade;

    // === Оффсет прицела ===
    private float heightMul;
    private float offsetX;
    private float offsetZ;

    public SpookyTimeRotationMode() {
        super("SpookyTime");
    }

    public void reset() {
        paramsUpdateMs = 0L;
        paramsIntervalMs = 0L;
        maxYawSpeed = maxAngleErr = velLimit = driftYaw = deadzone = 0f;
        switchTimer = 0f;
        snapYaw = snapPitch = 0f;
        lastAttackMs = holdMs = 0L;
        velYawSm = velPitchSm = 0f;
        missSinceMs = 0L;
        fade = 0f;
        heightMul = 0.54f;
        offsetX = offsetZ = 0f;
    }

    /** Вызывать из Aura.attackTarget() когда попал по цели. */
    public void onAttack() {
        switchTimer = rand(1.9f, 2.3f);
        lastAttackMs = System.currentTimeMillis();
        holdMs = (long) ThreadLocalRandom.current().nextDouble(180.0, 240.0);
        heightMul = rand(0.54f, 0.72f);
        offsetX = rand(-0.12f, 0.12f);
        offsetZ = rand(-0.12f, 0.12f);
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return current;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return current;

        long now = System.currentTimeMillis();
        boolean maceMode = isMaceMode(mc);

        float attackRange = (float) Math.max(3.0f, mc.player.getEntityInteractionRange());

        // Параметры по таймеру
        if (paramsUpdateMs == 0L) paramsUpdateMs = now;
        if (lastAttackMs == 0L) lastAttackMs = now;
        if (heightMul == 0f) heightMul = rand(0.34f, 0.65f);

        if (now - paramsUpdateMs > paramsIntervalMs) {
            paramsUpdateMs = now;
            paramsIntervalMs = (long) rand(25f, 50f);
            maxYawSpeed = rand(142f, 200f);
            maxAngleErr = rand(34f, 54f);
            velLimit = rand(7f, 14f);
            driftYaw = rand(0.04f, 0.12f) * (ThreadLocalRandom.current().nextBoolean() ? 1f : -1f);
            deadzone = rand(0.4f, 0.9f);
        }

        // Точка прицеливания
        boolean higher = maceMode && (
                living.getY() >= mc.player.getY() - 0.3
                        || living.getY() - living.prevY > 0.05
        );
        double mul = maceMode ? (higher ? 0.25 : 0.9) : heightMul;
        double ox = maceMode ? 0.0 : offsetX;
        double oz = maceMode ? 0.0 : offsetZ;

        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d point = living.getPos().add(
                ox,
                MathHelper.clamp(living.getHeight() * mul, 0.1, Math.max(0.1, living.getHeight() - 0.1)),
                oz
        );
        Vec3d dir = point.subtract(eye).normalize();

        float goalYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float goalPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))),
                -90.0, 90.0);

        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();

        // Snap-back
        boolean didSwitch = false;
        if (switchTimer > rand(0.43f, 0.75f)) {
            didSwitch = true;
            switchTimer = rand(0.13f, 0.25f);
        }
        if (didSwitch) {
            snapYaw = goalYaw;
            snapPitch = goalPitch;
        } else if (now - lastAttackMs < holdMs) {
            goalYaw = snapYaw;
            goalPitch = snapPitch;
        }

        // Fade при промахе
        if (!rayHitsBox(eye, dir, living.getBoundingBox(), attackRange + 1.0)) {
            if (missSinceMs == 0L) missSinceMs = now;
        } else {
            missSinceMs = 0L;
        }
        float missProgress = MathHelper.clamp(
                ((missSinceMs == 0L ? 0f : now - missSinceMs) - 250f) / 300f, 0f, 1f);
        if (missProgress >= fade) fade = missProgress;
        else fade += (missProgress - fade) * 4.0f;

        // Скорости
        float dYaw = MathHelper.wrapDegrees(goalYaw - curYaw);
        float dPitch = goalPitch - curPitch;
        float dist = (float) Math.hypot(dYaw, dPitch);

        float speed = maxYawSpeed
                * easeInOut(MathHelper.clamp(dist / maxAngleErr, 0f, 1f))
                * MathHelper.lerp(MathHelper.clamp((now - lastAttackMs) / 220f, 0f, 1f), 0.78f, 1f)
                * (1f - MathHelper.clamp(fade, 0f, 1f) * (maceMode ? 0.35f : 1f))
                * (didSwitch ? 1.25f : 1f)
                * (higher ? 2.2f : (maceMode ? 1.8f : 1f))
                * (mc.player.handSwingTicks > rand(0.2f, 0.4f) && !maceMode ? 0.5f : 1f);

        float limit = velLimit
                * (didSwitch ? 4f : 1f)
                * (higher ? 2f : (maceMode ? 1.5f : 1f));

        float proximityMul = intersects(mc.player.getBoundingBox(), living.getBoundingBox()) ? 2f : 1f;
        float uYaw = dist < 0.001f ? 0f : dYaw / dist;
        float uPitch = dist < 0.001f ? 0f : dPitch / dist;

        velYawSm += MathHelper.clamp(
                speed * uYaw * 1.25f / proximityMul - velYawSm,
                -limit * (maceMode ? 0.6f : 1.2f),
                limit * (maceMode ? 0.6f : 1.2f));

        velPitchSm += MathHelper.clamp(
                speed * uPitch + velYawSm * driftYaw - velPitchSm,
                -limit * 0.68f,
                limit * 0.68f);

        velYawSm = MathHelper.clamp(velYawSm, -Math.abs(dYaw), Math.abs(dYaw));
        velPitchSm = MathHelper.clamp(velPitchSm,
                -Math.abs(dPitch) - Math.abs(velYawSm * driftYaw),
                Math.abs(dPitch) + Math.abs(velYawSm * driftYaw));

        if (dist < deadzone) {
            velYawSm = 0f;
            velPitchSm = 0f;
        }

        // Финальный шаг
        float minYawStep = minStep(Math.abs(dYaw), maceMode ? 0.8f : 1.0f, maceMode ? 1.2f : 1.5f);
        float minPitchStep = minStep(Math.abs(dPitch), maceMode ? 1.0f : 2.5f, maceMode ? 1.8f : 4.0f);

        float stepYaw = Math.min(Math.abs(velYawSm), minYawStep)
                * (higher ? 2.2f : (maceMode ? 1.9f : 1f));
        float stepPitch = Math.min(Math.abs(velPitchSm), minPitchStep)
                * (higher ? 2.4f : (maceMode ? 1.9f : 1f));

        if (maceMode && !higher) {
            stepYaw *= 2f;
            stepPitch *= 2f;
        }

        float finalYaw = curYaw + Math.copySign(stepYaw, velYawSm == 0f ? (dYaw >= 0 ? 1f : -1f) : velYawSm);
        float finalPitch = MathHelper.clamp(
                curPitch + Math.copySign(stepPitch, velPitchSm == 0f ? (dPitch >= 0 ? 1f : -1f) : velPitchSm),
                -90f, 90f);

        return new Rotation(finalYaw, finalPitch);
    }

    // === helpers ===

    private static boolean isMaceMode(MinecraftClient mc) {
        return mc.player != null
                && mc.player.getMainHandStack().isOf(Items.MACE)
                && !mc.player.isOnGround();
    }

    private static boolean intersects(Box a, Box b) {
        return a.intersects(b);
    }

    /** Минимальный шаг (на основе mouse sensitivity). */
    private static float minStep(float delta, float minMul, float maxMul) {
        float noise = rand(minMul, maxMul);
        return Math.max(0.05f, Math.abs(delta) / Math.max(1f, noise));
    }

    /** Cubic in-out ease. */
    private static float easeInOut(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    private static float rand(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    /** AABB ray test (slab method). */
    private static boolean rayHitsBox(Vec3d origin, Vec3d dir, Box box, double reach) {
        double tMin = 0.0;
        double tMax = reach;
        for (int axis = 0; axis < 3; axis++) {
            double d = axis == 0 ? dir.x : (axis == 1 ? dir.y : dir.z);
            double o = axis == 0 ? origin.x : (axis == 1 ? origin.y : origin.z);
            double min = axis == 0 ? box.minX : (axis == 1 ? box.minY : box.minZ);
            double max = axis == 0 ? box.maxX : (axis == 1 ? box.maxY : box.maxZ);
            if (Math.abs(d) < 1.0E-9) {
                if (o < min || o > max) return false;
            } else {
                double inv = 1.0 / d;
                double t1 = (min - o) * inv;
                double t2 = (max - o) * inv;
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return false;
            }
        }
        return tMax >= 0 && tMin <= reach;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
