package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * WhiteRise rotation, ported from Aspect {@code ResetHelper_11}.
 * Differences from the source: the aim point comes from Aura's hit point resolver (Aspect used
 * RaycastUtil.getMultipoint), "ticksToAttack <= 1" is approximated by {@code Aura.canAttack(player, 1)},
 * mouse GCD snapping is left to RotationManager (Rotation.normalize()), and AuraTransition (target switch
 * smoothing) is not ported.
 */
public class WhiteRiseRotationMode extends RotationMode {
    private static final float RETURN_SPEED = 45.0F;

    private Entity trackedTarget;
    private float speedAcceleration;
    private boolean back;
    private int tickCounter;

    public WhiteRiseRotationMode() {
        super("WhiteRise");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        Aura aura = (Aura) AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
        if (player == null || aura == null) {
            return current;
        }
        if (entity == null) {
            this.trackedTarget = null;
            this.speedAcceleration = 0.0F;
            this.back = false;
            this.tickCounter = 0;
            float yawStep = MathHelper.clamp(MathHelper.wrapDegrees(target.getYaw() - current.getYaw()), -RETURN_SPEED, RETURN_SPEED);
            float pitchStep = MathHelper.clamp(target.getPitch() - current.getPitch(), -RETURN_SPEED, RETURN_SPEED);
            return new Rotation(MathHelper.wrapDegrees(current.getYaw() + yawStep), MathHelper.clamp(current.getPitch() + pitchStep, -89.0F, 89.0F));
        }
        if (player.isBlocking()) {
            return new Rotation(player.getYaw(), player.getPitch());
        }
        if (this.trackedTarget != entity) {
            this.trackedTarget = entity;
            this.speedAcceleration = 0.0F;
            this.back = false;
            this.tickCounter = 0;
        }

        boolean attackSoon = player.getAttackCooldownProgress(1.0F) > 0.9F && aura.canAttack(player, 1);
        Rotation result = current;
        for (int i = 0; i < AspectRotationSupport.SUB_STEPS; i++) {
            result = step(result, target, player, attackSoon);
        }
        return result;
    }

    private Rotation step(Rotation current, Rotation target, ClientPlayerEntity player, boolean attackSoon) {
        this.tickCounter++;
        float jitter = (float) ((Math.sin(this.tickCounter * 0.17) * 0.12 + (Math.random() * 0.08 - 0.04)) * 0.7F);
        float lastYaw = current.getYaw();
        float lastPitch = current.getPitch();
        float yawToTarget = MathHelper.wrapDegrees(target.getYaw() - lastYaw);
        boolean gliding = player.isGliding();

        if (!this.back) {
            float gain = 0.0055F;
            float absYaw = Math.abs(yawToTarget);
            if (absYaw > 60.0F) {
                gain += 0.028800001F;
            } else if (absYaw > 30.0F) {
                gain += 0.014400001F;
            } else {
                gain += 0.0072000003F;
            }
            if (attackSoon) {
                gain += 0.012857143F;
            }
            this.speedAcceleration += gain * (1.6F + jitter);
            if (this.speedAcceleration >= 0.22F) {
                this.back = true;
            }
        } else {
            float loss = attackSoon ? 0.045F : 0.008F;
            this.speedAcceleration -= loss * (2.1F + jitter);
            if (this.speedAcceleration <= -0.04F) {
                this.back = false;
            }
        }

        float speed = MathHelper.clamp(this.speedAcceleration, 0.0F, gliding ? 0.38F : 0.26F);
        if (attackSoon) {
            speed = Math.min(speed + 0.1F, gliding ? 0.46F : 0.34F);
        }
        speed += jitter * 0.5F;
        if (this.tickCounter % 7 == 0) {
            speed += 0.03F;
        }

        float maxYawStep = gliding ? 42.0F : (attackSoon ? 28.0F : 20.0F);
        float maxPitchStep = gliding ? 12.0F : (attackSoon ? 4.5F : 2.8F);
        float yawStep = MathHelper.clamp(yawToTarget, -maxYawStep, maxYawStep);
        float pitchStep = MathHelper.clamp(target.getPitch() - lastPitch, -maxPitchStep, maxPitchStep);
        float pitchFactor = speed * 0.28F;
        float yawFactor = speed * (0.85F + jitter * 0.4F);

        float nextYaw = lastYaw + yawStep * yawFactor;
        float nextPitch = MathHelper.clamp(lastPitch + pitchStep * pitchFactor, -89.0F, 89.0F);
        return new Rotation(MathHelper.wrapDegrees(nextYaw), nextPitch);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.0D, 0.0D, 0.0D);
    }
}
