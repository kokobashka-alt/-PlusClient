package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.security.SecureRandom;

public class FuntimeRotationMode extends RotationMode {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final float[] SPEED_STEPS = {0.38F, 0.8F, 1.0F};

    private float yawNoise;
    private float yawDirection = 1.0F;
    private boolean yawBurst;

    private float pitchNoise;
    private float pitchDirection = 1.0F;
    private boolean pitchBurst;

    private float noiseStrength;
    private int lastHitCount;
    private long lastProcessTime;
    private int stableTicks;
    private boolean wasAttacking;

    public FuntimeRotationMode() {
        super("FunTime");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec, Entity entity) {
        if (MinecraftClient.getInstance().player == null) return target;

        Aura aura = Aura.getInstance();
        int hitCount = aura.hitCounter(); // В твоей Aura есть hitCounter
        long now = System.currentTimeMillis();

        // Сброс стейта при долгом простое
        if (now - lastProcessTime > 200L) {
            resetState();
        }
        lastProcessTime = now;

        boolean newHit = hitCount != lastHitCount;
        if (newHit) lastHitCount = hitCount;

        // Генерация шума по Yaw
        float yawMomentum = yawDirection * yawNoise;
        if (RANDOM.nextFloat() < 0.1F + (yawMomentum > 0.0F ? 0.065F * yawMomentum : 0.0F)) {
            yawDirection = -yawDirection;
        }
        if (yawBurst ? RANDOM.nextFloat() < 0.505F : RANDOM.nextFloat() < 0.065F) {
            yawBurst = !yawBurst;
        }
        yawNoise = 0.97F * yawNoise + yawDirection * Math.min((yawBurst ? 10.5F : 4.3F) * exponentialRandom(), 18.0F);

        // Генерация шума по Pitch
        float pitchMomentum = pitchDirection * pitchNoise;
        if (RANDOM.nextFloat() < 0.1F + (pitchMomentum > 0.0F ? 0.11F * pitchMomentum : 0.0F)) {
            pitchDirection = -pitchDirection;
        }
        if (pitchBurst ? RANDOM.nextFloat() < 0.505F : RANDOM.nextFloat() < 0.065F) {
            pitchBurst = !pitchBurst;
        }
        pitchNoise = 0.97F * pitchNoise + pitchDirection * Math.min((pitchBurst ? 5.2F : 2.3F) * exponentialRandom(), 15.0F);

        boolean attacking = entity != null && aura.canAttack(MinecraftClient.getInstance().player, 3);

        if (!newHit && attacking == wasAttacking) {
            stableTicks++;
        } else {
            stableTicks = 0;
        }
        wasAttacking = attacking;

        float speed = MathHelper.clamp(
                SPEED_STEPS[Math.min(stableTicks, SPEED_STEPS.length - 1)] * randomLerp(0.9F, 1.08F),
                0.12F, 1.0F
        );

        float targetStrength = entity == null ? 0.0F : (attacking ? 0.7F : 1.0F);
        noiseStrength += (targetStrength - noiseStrength) * 0.35F;

        // Если целимся в энтити
        float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float deltaPitch = target.getPitch() - current.getPitch();

        if (attacking) {
            return new Rotation(
                    current.getYaw() + deltaYaw * speed + yawNoise * 0.15F,
                    current.getPitch() + deltaPitch * speed + pitchNoise * 0.25F
            );
        }

        // Если бьем в пустоту/ждем атаку — возвращаемся к камере
        // В твоем клиенте мы просто берем target (так как Aura управляет наводкой)
        return new Rotation(
                current.getYaw() + deltaYaw * speed + yawNoise * noiseStrength,
                current.getPitch() + deltaPitch * speed + pitchNoise * noiseStrength
        );
    }

    private void resetState() {
        yawNoise = 0.0F;
        pitchNoise = 0.0F;
        yawDirection = 1.0F;
        pitchDirection = 1.0F;
        yawBurst = false;
        pitchBurst = false;
        noiseStrength = 0.0F;
        stableTicks = 0;
        wasAttacking = false;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(RANDOM.nextFloat(), min, max);
    }

    private float exponentialRandom() {
        return -(float) Math.log(1.0F - RANDOM.nextFloat());
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
