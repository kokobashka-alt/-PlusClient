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
 * ShardAC-AI rotation, ported from Aspect {@code GrimRotation}.
 * Differences from the source: the aim point comes from Aura's hit point resolver, a hit is detected from
 * Aura.hitCounter(), and the return-to-camera phase (entity == null) is driven by PlusClient's RotationManager,
 * which passes the player's real rotation as {@code target}; the 2 second timeout and FreeLookStorage are not ported.
 */
public class ShardAcAiRotationMode extends RotationMode {
    private Entity trackedTarget;
    private int lastHitCounter;
    private float acceleration;
    private boolean back;
    private boolean hit;
    private float slow = 1.0F;
    private float overshootYaw;
    private float overshootPitch;

    public ShardAcAiRotationMode() {
        super("ShardAC-AI");
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
            Rotation back = current;
            for (int i = 0; i < AspectRotationSupport.SUB_STEPS; i++) {
                float yawDelta = MathHelper.wrapDegrees(target.getYaw() - back.getYaw());
                float pitchDelta = target.getPitch() - back.getPitch();
                if (Math.abs(yawDelta) < 0.5F && Math.abs(pitchDelta) < 0.5F) {
                    return new Rotation(MathHelper.wrapDegrees(target.getYaw()), MathHelper.clamp(target.getPitch(), -89.0F, 89.0F));
                }
                back = new Rotation(
                        MathHelper.wrapDegrees(back.getYaw() + yawDelta * 0.2F),
                        MathHelper.clamp(back.getPitch() + pitchDelta * 0.2F, -89.0F, 89.0F));
            }
            return back;
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

        Rotation result = current;
        for (int i = 0; i < AspectRotationSupport.SUB_STEPS; i++) {
            result = step(result, target, entity, player);
        }
        return result;
    }

    private Rotation step(Rotation current, Rotation target, Entity entity, ClientPlayerEntity player) {
        float lastYaw = current.getYaw();
        float lastPitch = current.getPitch();
        float targetYaw = target.getYaw();
        float targetPitch = MathHelper.clamp(target.getPitch(), -89.0F, 89.0F);
        Vec3d eye = player.getEyePos();
        Box box = entity.getBoundingBox();
        boolean onTarget = AspectRotationSupport.lookRayHitsBox(player, lastYaw, lastPitch, box.expand(-0.3));
        float yawDelta = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float pitchDelta = targetPitch - lastPitch;
        boolean attackWindow = AspectRotationSupport.attackWindow(player);

        if (this.hit && attackWindow) {
            this.hit = false;
            this.back = false;
            this.acceleration = 0.0F;
        }

        float speed;
        if (this.hit && !attackWindow) {
            this.acceleration = Math.min(this.acceleration, 0.0F);
            this.slow = AspectRotationSupport.nextSlowFactor(this.slow, box, eye);
            speed = Math.max(this.acceleration, 0.05F) * this.slow * 0.4F;
        } else if (onTarget) {
            this.slow = AspectRotationSupport.nextSlowFactor(this.slow, box, eye);
            speed = Math.max(this.acceleration, 0.06F) * this.slow * 0.35F;
        } else {
            if (this.back) {
                float decay = Math.abs(yawDelta) > 30.0F ? 0.012F : 0.022F;
                if (this.acceleration >= -0.1F) {
                    this.acceleration -= decay;
                }
                if (this.acceleration <= -0.1F) {
                    this.back = false;
                }
            } else {
                this.acceleration += 0.0035F;
                if (this.acceleration >= 0.24F) {
                    this.back = true;
                }
            }
            this.slow = AspectRotationSupport.nextSlowFactor(this.slow, box, eye);
            speed = Math.max(this.acceleration, 0.0F) * this.slow;
        }
        float nextYaw = lastYaw + yawDelta * Math.min(Math.max(speed * 1.8F, 0.0F), 1.0F);
        float nextPitch = lastPitch + pitchDelta * Math.min(Math.max(speed * 0.4F, 0.0F), 1.0F);

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
