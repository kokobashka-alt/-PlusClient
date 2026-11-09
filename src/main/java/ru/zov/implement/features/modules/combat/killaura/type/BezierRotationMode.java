package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.util.concurrent.ThreadLocalRandom;

public class BezierRotationMode extends RotationMode {

    private float internalYaw, internalPitch;
    private float velYawSm, velPitchSm;
    private float lastYawAccel;
    private float prevAppliedYawVel, prevAppliedPitchVel;
    
    private float noiseSm;
    private float speedOsc;
    private boolean accelerating;
    private int tickCounter;
    private float accelPeak, decelFloor, peakMul;
    private float yawJitterSm, pitchJitterSm;
    private float drift;
    
    private int pauseTicks;
    private float activity = 1.0F;
    private LivingEntity currentTarget;

    public BezierRotationMode() {
        super("Bezier");
    }

    @Override
    public Rotation process(Rotation serverRotation, Rotation targetRotation, Vec3d targetVec, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return targetRotation; // Если цели нет, просто смотрим куда надо
        }

        // Если цель сменилась, сбрасываем физику
        if (this.currentTarget != living) {
            this.currentTarget = living;
            this.internalYaw = serverRotation.getYaw();
            this.internalPitch = serverRotation.getPitch();
            this.velYawSm = 0.0F;
            this.velPitchSm = 0.0F;
            this.tickCounter = 0;
            this.pauseTicks = 0;
            this.accelerating = false;
            this.speedOsc = 0.0F;
            this.noiseSm = 0.0F;
            this.yawJitterSm = 0.0F;
            this.pitchJitterSm = 0.0F;
            this.lastYawAccel = 0.0F;
            this.prevAppliedYawVel = 0.0F;
            this.prevAppliedPitchVel = 0.0F;
            
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            this.drift = (rnd.nextBoolean() ? 1.0F : -1.0F) * (0.001F + rnd.nextFloat() * 4.0E-4F);
            this.accelPeak = 0.18F + rnd.nextFloat() * 0.1F;
            this.decelFloor = -0.02F - rnd.nextFloat() * 0.05F;
            this.peakMul = 0.8F + rnd.nextFloat() * 0.4F;
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        this.tickCounter++;
        this.noiseSm = this.noiseSm * 0.85F + (float) rnd.nextGaussian() * 0.045F;
        
        float aimYaw = targetRotation.getYaw();
        float aimPitch = targetRotation.getPitch();
        float yawDist = Math.abs(MathHelper.wrapDegrees(aimYaw - serverRotation.getYaw()));
        
        // Проверка кулдауна атаки
        boolean ready = net.minecraft.client.MinecraftClient.getInstance().player.getAttackCooldownProgress(1.0F) > 0.9F;

        // 1. Логика ускорения/замедления (физика)
        if (!this.accelerating) {
            float gain = 0.0055F;
            gain += (0.004F + 0.012F * (1.0F - (float) Math.exp(-yawDist / 35.0F))) * 1.8F;
            gain += rnd.nextFloat() * 0.0025F;
            if (ready) gain += 0.0128F;
            
            this.speedOsc += gain * (1.6F + this.noiseSm);
            if (this.speedOsc >= this.accelPeak) {
                this.accelerating = true;
                this.decelFloor = -0.02F - rnd.nextFloat() * 0.05F;
            }
        } else {
            float drop = (ready ? 0.045F : 0.008F) * (0.85F + rnd.nextFloat() * 0.3F);
            this.speedOsc -= drop * (2.1F + this.noiseSm);
            if (this.speedOsc <= this.decelFloor) {
                this.accelerating = false;
                this.accelPeak = 0.18F + rnd.nextFloat() * 0.1F;
            }
        }

        // Ограничение скорости
        float stepCap = MathHelper.clamp(this.speedOsc, 0.0F, 0.312F);
        if (ready) stepCap = Math.min(stepCap + 0.1F, 0.408F);
        stepCap += this.noiseSm * 0.25F;
        if (this.tickCounter <= 3) stepCap *= 0.2F * (float) this.tickCounter * this.peakMul;

        float dYaw = MathHelper.wrapDegrees(aimYaw - this.internalYaw);
        float dPitch = aimPitch - this.internalPitch;

        // Паузы (имитация человеческого залипания)
        if (this.pauseTicks > 0) {
            this.pauseTicks--;
        } else if (rnd.nextFloat() < 0.03F && this.tickCounter > 4 && !ready && yawDist < 10.0F) {
            this.pauseTicks = rnd.nextInt(3) + 2;
        }
        this.activity += ((this.pauseTicks > 0 ? 0.0F : 1.0F) - this.activity) * 0.35F;

        // Ограничение по максимальному шагу
        float randScale = 0.9F + rnd.nextFloat() * 0.2F;
        float maxYawStep = (ready ? 38.4F : 28.8F) * randScale;
        float maxPitchStep = (ready ? 5.4F : 3.36F) * randScale;
        dYaw = smoothClamp(dYaw, maxYawStep, rnd);
        dPitch = smoothClamp(dPitch, maxPitchStep, rnd);

        // Применение скорости
        float moveYaw = stepCap * (0.94F + this.noiseSm * 0.92F);
        float movePitch = Math.min(stepCap * 0.836F, moveYaw * 0.96F);

        float wantVelYaw = dYaw * moveYaw * this.activity;
        float wantVelPitch = dPitch * movePitch * this.activity;
        
        float kYaw = (0.16F + rnd.nextFloat() * 0.08F) * (Math.abs(dYaw) < 3.0F ? 0.4F + 0.6F * (Math.abs(dYaw) / 3.0F) : 1.0F);
        float kPitch = (0.45F + rnd.nextFloat() * 0.15F) * (Math.abs(dPitch) < 3.0F ? 0.4F + 0.6F * (Math.abs(dPitch) / 3.0F) : 1.0F);

        // Джиттер
        float yawSigma = 0.006F + Math.abs(this.velYawSm) * 0.012F;
        float pitchSigma = 0.02F + Math.abs(this.velPitchSm) * 0.05F;
        this.yawJitterSm = this.yawJitterSm * 0.55F + (float) rnd.nextGaussian() * yawSigma * 0.45F;
        this.pitchJitterSm = this.pitchJitterSm * 0.55F + (float) rnd.nextGaussian() * pitchSigma * 0.45F;

        // Инерция
        float accelYaw = (wantVelYaw - this.velYawSm) * kYaw + this.yawJitterSm;
        float accelBound = 0.7F + Math.abs(this.velYawSm) * 0.2F;
        accelYaw = MathHelper.clamp(accelYaw, this.lastYawAccel - accelBound, this.lastYawAccel + accelBound);
        
        this.velYawSm += accelYaw;
        this.lastYawAccel = accelYaw;
        this.velPitchSm += (wantVelPitch - this.velPitchSm) * kPitch + this.pitchJitterSm;
        this.velPitchSm = smoothClamp(this.velPitchSm, 4.8F, rnd);

        // Обновление углов
        float appliedYawVel = (this.velYawSm + this.prevAppliedYawVel) * 0.5F;
        float appliedPitchVel = (this.velPitchSm + this.prevAppliedPitchVel) * 0.5F;
        this.prevAppliedYawVel = this.velYawSm;
        this.prevAppliedPitchVel = this.velPitchSm;
        
        this.internalYaw += appliedYawVel;
        this.internalPitch = MathHelper.clamp(this.internalPitch + appliedPitchVel, -89.0F, 89.0F);

        // Дыхание (плавные колебания)
        this.internalYaw += breath(1.1F, 18.0F);
        this.internalPitch += breath(0.05F, 32.0F);

        // Дрифт (GCD-эмуляция для античитов)
        this.drift += (float) (rnd.nextGaussian() * 2.0E-5);
        this.drift = this.drift >= 0.0F ? MathHelper.clamp(this.drift, 7.0E-4F, 0.0016F) : MathHelper.clamp(this.drift, -0.0016F, -7.0E-4F);
        
        float rawDeltaYaw = this.internalYaw - serverRotation.getYaw();
        float sendYaw = serverRotation.getYaw() + rawDeltaYaw * (1.0F + this.drift);

        return new Rotation(sendYaw, this.internalPitch);
    }

    private float smoothClamp(float val, float max, ThreadLocalRandom rnd) {
        if (Math.abs(val) > max) {
            return max * Math.signum(val) * (0.92F + rnd.nextFloat() * 0.08F);
        }
        return val;
    }

    private float breath(float amp, float period) {
        long t = System.currentTimeMillis();
        return (float) (amp * (Math.sin(t / period) * 0.5 + Math.sin(t / (period * 0.73)) * 0.3));
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
