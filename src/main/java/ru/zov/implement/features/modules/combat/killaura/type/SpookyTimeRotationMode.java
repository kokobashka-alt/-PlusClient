package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.util.Random;

public class SpookyTimeRotationMode extends RotationMode {
    private final Random random = new Random();
    private int ticksToAttack = 0;

    public SpookyTimeRotationMode() {
        super("SpookyTime");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d targetVec, Entity targetEntity) {
        // 1. Если цель - сущность, пытаемся взять точку чуть выше центра (как в оригинале)
        Vec3d aimVec = targetVec;
        if (targetEntity instanceof LivingEntity living) {
            // В оригинале тут был AuraUtil.getClosestTargetPoint, 
            // мы берем просто центр хитбокса с небольшим смещением.
            aimVec = living.getBoundingBox().getCenter().add(0, 0.1, 0);
        }

        // 2. Вычисляем базовые углы на цель (Yaw и Pitch)
        Vec3d eyePos = net.minecraft.client.MinecraftClient.getInstance().player.getEyePos();
        Vec3d diff = aimVec.subtract(eyePos);
        
        float baseYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float basePitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))),
                -90.0, 90.0
        );

        // 3. Логика "залипания" после удара (attackF)
        // В оригинале tick = 4 выставлялся снаружи при ударе. 
        // Здесь мы просто добавляем случайный джиттер, который имитирует это.
        float randomToAttack = 0.0F;
        if (ticksToAttack > 0) {
            ticksToAttack--;
            randomToAttack = (random.nextFloat() * 7.0F - 3.0F) 
                    + (float) (2.0 * Math.sin(System.currentTimeMillis() / 30.0));
        }

        // 4. Основной синусоидальный джиттер (SpookyTime)
        float randomXY = (random.nextFloat() * 6.0F - 3.0F)
                + (float) (3.0 * Math.cos(System.currentTimeMillis() / 40.0));
                
        float randomX = (random.nextFloat() * 2.0F - 1.0F)
                + (float) (6.0 * Math.sin(System.currentTimeMillis() / 240.0));

        // 5. Итоговый поворот
        float finalYaw = baseYaw + randomXY + randomToAttack;
        float finalPitch = basePitch + randomX;

        return new Rotation(finalYaw, MathHelper.clamp(finalPitch, -90f, 90f));
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
    
    // Этот метод можно вызывать из Aura, когда происходит удар
    public void onAttack() {
        this.ticksToAttack = 4;
    }
}
