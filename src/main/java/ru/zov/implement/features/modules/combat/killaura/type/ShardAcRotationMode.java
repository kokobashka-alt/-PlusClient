package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * ShardAC rotation, ported from Aspect {@code ResetHelper_9}.
 * Differences from the source: the aim point comes from Aura's hit point resolver instead of Aspect's
 * RaycastUtil, and a hit is detected from Aura.hitCounter() instead of an onAttack() callback.
 * The source clamps the target pitch to [0, 90]; that is kept as is.
 */
public class ShardAcRotationMode extends RotationMode {
    private static final float RETURN_YAW_SPEED = 30.0F;
    private static final float RETURN_PITCH_SPEED = 9.0F;

    private Entity trackedTarget;
    private int lastHitCounter;
    private float acceleration;
    private boolean back;
    private boolean hit;
    private float slow = 1.0F;
    private float overshootYaw;
    private float overshootPitch;

    public ShardAcRotationMode() {
        super("ShardAC");
    }

    private void resetState() {
        this.acceleration = 0.0F;
        this.back = false;
        this.hit = false;
        this.slow = 1.0F;
        this.overshootYaw = 0.0F;
        this.overshootPitch = 0.0F;
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
            resetState();
            float yawStep = MathHelper.clamp(MathHelper.wrapDegrees(target.getYaw() - current.getYaw()), -RETURN_YAW_SPEED, RETURN_YAW_SPEED);
            float pitchStep = MathHelper.clamp(target.getPitch() - current.getPitch(), -RETURN_PITCH_SPEED, RETURN_PITCH_SPEED);
            return new Rotation(MathHelper.wrapDegrees(current.getYaw() + yawStep), MathHelper.clamp(current.getPitch() + pitchStep, -89.0F, 89.0F));
        }
        if (this.trackedTarget != entity) {
            this.trackedTarget = entity;
            this.lastHitCounter = aura.hitCounter();
            resetState();
        }
        if (aura.hitCounter() != this.lastHitCounter) {
            this.lastHitCounter = aura.hitCounter();
            this.hit = true;
            this.back = true;
            this.acceleration = -0.04F;
        }

        float lastYaw = current.getYaw();
        float lastPitch = current.getPitch();
        float targetYaw = target.getYaw();
        float targetPitch = AspectRotationSupport.clampPitch(target.getPitch(), 0.0F, 90.0F);
        Vec3d eye = player.getEyePos();
        Box box = entity.getBoundingBox();
        boolean onTarget = AspectRotationSupport.lookRayHitsBox(player, lastYaw, lastPitch, box.expand(-0.3));
        float yawDelta = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float pitchDelta = targetPitch - lastPitch;
        float nextYaw = lastYaw;
        float nextPitch = lastPitch;
        boolean attackWindow = AspectRotationSupport.attackWindow(player);

        if (this.hit && attackWindow) {
            this.hit = false;
            this.back = false;
            this.acceleration = 0.0F;
        }

        if (this.hit && !attackWindow) {
            this.acceleration = Math.min(this.acceleration, 0.0F);
        } else if (!onTarget) {
            if (this.back) {
                float decay = Math.abs(yawDelta) > 30.0F ? 0.01F : 0.046F;
                if (this.acceleration >= -0.1F) {
                    this.acceleration -= decay;
                }
                if (this.acceleration <= -0.1F) {
                    this.back = false;
                }
            } else {
                this.acceleration += 0.003F;
                if (this.acceleration >= 0.25F) {
                    this.back = true;
                }
            }
            this.slow = AspectRotationSupport.nextSlowFactor(this.slow, box, eye);
            float speed = Math.max(this.acceleration, 0.0F) * this.slow;
            nextYaw = lastYaw + yawDelta * Math.min(Math.max(speed * 1.8F, 0.0F), 1.0F);
            nextPitch = lastPitch + pitchDelta * Math.min(Math.max(speed * 0.4F, 0.0F), 1.0F);
        }

        if (Math.abs(yawDelta) > 18.0F && Math.abs(this.acceleration) > 0.08F) {
            this.overshootYaw = Math.signum(yawDelta) * Math.min(Math.abs(yawDelta) * 0.06F, 1.2F) * 0.9F;
            this.overshootPitch = Math.signum(pitchDelta) * Math.min(Math.abs(pitchDelta) * 0.04F, 0.8F) * 0.9F;
        } else {
            this.overshootYaw *= 0.65F;
            this.overshootPitch *= 0.65F;
        }

        return new Rotation(
                MathHelper.wrapDegrees(nextYaw + this.overshootYaw),
                MathHelper.clamp(nextPitch + this.overshootPitch, -89.0F, 89.0F));
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.0D, 0.0D, 0.0D);
    }
}
