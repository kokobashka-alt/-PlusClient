package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import static ru.zov.common.QuickImports.mc;

/**
 * Shard 2 (Nano): velocity/acceleration based aim with tanh limiting, startup ramp,
 * breathing sway and mouse-GCD snapping. Ported from lublin ShardRotation.
 * One process() call is one client tick.
 */
public final class ShardNanoRotationMode extends RotationMode {
    private static final float RESYNC_THRESHOLD = 30.0F;

    private boolean initialized;
    private Entity target;
    private int age;
    private float smoothYaw;
    private float smoothPitch;
    private float yawVelocity;
    private float pitchVelocity;
    private float offsetX;
    private float offsetY;
    private float offsetZ;
    private float swayYaw;
    private float swayPitch;
    private Vec3d randomBoxPoint = Vec3d.ZERO;
    private int randomBoxCounter;

    public ShardNanoRotationMode() {
        super("Shard 2 (Nano)");
        reset();
    }

    public void reset() {
        this.initialized = false;
        this.target = null;
        this.age = 0;
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
        this.offsetX = 0.0F;
        this.offsetY = 0.0F;
        this.offsetZ = 0.0F;
        this.swayYaw = 0.0F;
        this.swayPitch = 0.0F;
        this.randomBoxPoint = Vec3d.ZERO;
        this.randomBoxCounter = 0;
    }

    @Override
    public Rotation process(Rotation current, Rotation targetRotation, Vec3d vec, Entity entity) {
        ClientPlayerEntity player = mc.player;
        if (player == null || current == null || targetRotation == null
                || !Float.isFinite(current.getYaw()) || !Float.isFinite(current.getPitch())
                || !Float.isFinite(targetRotation.getYaw()) || !Float.isFinite(targetRotation.getPitch())) {
            reset();
            return current;
        }

        float currentYaw = current.getYaw();
        float currentPitch = current.getPitch();

        if (player.isBlocking()) {
            this.smoothYaw = currentYaw;
            this.smoothPitch = currentPitch;
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
            this.swayYaw = 0.0F;
            this.swayPitch = 0.0F;
            return current;
        }

        if (!this.initialized) {
            this.smoothYaw = currentYaw;
            this.smoothPitch = currentPitch;
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
            this.swayYaw = 0.0F;
            this.swayPitch = 0.0F;
            this.initialized = true;
        }

        if (this.target != entity) {
            this.target = entity;
            this.age = 0;
            this.smoothYaw = currentYaw;
            this.smoothPitch = currentPitch;
            this.yawVelocity *= 0.25F;
            this.pitchVelocity *= 0.20F;
            this.swayYaw *= 0.28F;
            this.swayPitch *= 0.28F;
            this.randomBoxPoint = Vec3d.ZERO;
            this.randomBoxCounter = 0;
        }

        if (Math.abs(MathHelper.wrapDegrees(this.smoothYaw - currentYaw)) > RESYNC_THRESHOLD
                || Math.abs(this.smoothPitch - currentPitch) > RESYNC_THRESHOLD) {
            this.smoothYaw = currentYaw;
            this.smoothPitch = currentPitch;
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
        }

        this.age++;
        boolean gliding = player.isGliding();

        float destYaw = targetRotation.getYaw();
        float destPitch = targetRotation.getPitch();
        if (entity instanceof LivingEntity living) {
            Vec3d point = gliding ? elytraAimPoint(living) : groundAimPoint(player, living, vec);
            if (point != null) {
                if (!gliding) {
                    point = point.add(updateOffsets());
                }

                float[] angles = anglesTo(player, point);
                destYaw = angles[0];
                destPitch = angles[1];
            }
        }

        float yawDelta = MathHelper.wrapDegrees(destYaw - this.smoothYaw);
        float pitchDelta = destPitch - this.smoothPitch;
        float absYawDelta = Math.abs(yawDelta);
        float absPitchDelta = Math.abs(pitchDelta);

        float startup = this.age <= 5 ? 0.26F + 0.148F * this.age : 1.0F;
        float yawDistanceFactor = MathHelper.clamp(absYawDelta / 39.0F, 0.13F, 1.0F);
        float pitchDistanceFactor = gliding
                ? MathHelper.clamp(absPitchDelta / 25.0F, 0.20F, 1.0F)
                : MathHelper.clamp(absPitchDelta / 19.0F, 0.085F, 0.82F);

        float baseSpeed = gliding ? 0.65F : 0.205F;
        float pitchSpeedMul = gliding ? 0.95F : 0.66F;
        if (pitchDelta < 0.0F && !gliding) {
            pitchSpeedMul *= 0.44F;
        }

        float targetYawVel = yawDelta * baseSpeed * yawDistanceFactor * startup;
        float targetPitchVel = pitchDelta * (baseSpeed * pitchSpeedMul) * pitchDistanceFactor * startup;

        float maxYawRate = gliding ? 110.0F : 24.5F;
        float maxPitchDown = gliding ? 120.0F : 11.2F;
        float maxPitchUp = gliding ? 90.0F : 3.9F;
        targetYawVel = tanhLimit(targetYawVel, maxYawRate);
        targetPitchVel = tanhLimit(targetPitchVel, targetPitchVel < 0.0F ? maxPitchUp : maxPitchDown);

        float maxAccelYaw = gliding ? 22.0F : 3.7F;
        float maxAccelPitchDown = gliding ? 22.0F : 2.2F;
        float maxAccelPitchUp = gliding ? 16.0F : 0.78F;
        float maxAccelPitch = targetPitchVel < this.pitchVelocity ? maxAccelPitchUp : maxAccelPitchDown;

        float diffYawVel = MathHelper.clamp(targetYawVel - this.yawVelocity, -maxAccelYaw, maxAccelYaw);
        float diffPitchVel = MathHelper.clamp(targetPitchVel - this.pitchVelocity, -maxAccelPitch, maxAccelPitch);

        float damping = absYawDelta < 4.0F ? 0.35F : 0.235F;
        this.yawVelocity += diffYawVel * damping;
        this.pitchVelocity += diffPitchVel * (damping * (gliding ? 1.0F : 1.11F));

        boolean closeToTarget = absYawDelta < 10.0F && absPitchDelta < 6.0F;
        if (closeToTarget && player.getAttackCooldownProgress(1.0F) > 0.8F) {
            this.yawVelocity *= 0.80F;
            this.pitchVelocity *= 0.80F;
        }

        this.smoothYaw += this.yawVelocity;
        this.smoothPitch += this.pitchVelocity;

        float pitchMin = gliding ? -89.0F : -40.0F;
        float pitchMax = gliding ? 89.0F : 75.0F;
        this.smoothPitch = MathHelper.clamp(this.smoothPitch, pitchMin, pitchMax);

        float swayYawAmp = 0.420F * swayDistanceMultiplier(player, entity);
        float swayPitchAmp = swayYawAmp * 0.38F;
        float targetSwayYaw = wave(swayYawAmp * 0.75F, false, 0.72F, 0.0)
                + wave(swayYawAmp * 0.25F, true, 1.45F, 0.85);
        float targetSwayPitch = wave(swayPitchAmp, true, 0.58F, 1.35);
        this.swayYaw = MathHelper.lerp(0.18F, this.swayYaw, targetSwayYaw);
        this.swayPitch = MathHelper.lerp(0.18F, this.swayPitch, targetSwayPitch);

        float finalYaw = this.smoothYaw + this.swayYaw;
        float finalPitch = this.smoothPitch + this.swayPitch;

        float gcd = gcd();
        if (gcd > 0.0F) {
            int yawTicks = Math.round(MathHelper.wrapDegrees(finalYaw - currentYaw) / gcd);
            int pitchTicks = Math.round((finalPitch - currentPitch) / gcd);
            finalYaw = currentYaw + yawTicks * gcd;
            finalPitch = currentPitch + pitchTicks * gcd;
        }

        finalPitch = MathHelper.clamp(finalPitch, pitchMin, pitchMax);
        return new Rotation(finalYaw, finalPitch);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private Vec3d groundAimPoint(ClientPlayerEntity player, LivingEntity living, Vec3d vec) {
        Box box = living.getBoundingBox();
        Vec3d point = vec != null ? player.getEyePos().add(vec) : box.getCenter();
        double height = box.maxY - box.minY;
        double minY = box.minY + height * 0.25;
        double maxY = box.minY + height * 0.48;
        return new Vec3d(point.x, MathHelper.clamp(point.y, minY, maxY), point.z);
    }

    private Vec3d elytraAimPoint(LivingEntity living) {
        Vec3d base = living.getEntityPos().add(0.0, living.getHeight() * 0.55F, 0.0);
        this.randomBoxCounter++;
        if (this.randomBoxCounter >= 12 + (int) (Math.random() * 15) || this.randomBoxPoint.equals(Vec3d.ZERO)) {
            this.randomBoxCounter = 0;
            Box box = living.getBoundingBox();
            this.randomBoxPoint = new Vec3d(
                    (Math.random() - 0.15) * box.getLengthX() * 1.2,
                    (Math.random() - 0.15) * box.getLengthY() * 1.2,
                    (Math.random() - 0.15) * box.getLengthZ() * 1.2);
        }

        return base.add(this.randomBoxPoint);
    }

    private Vec3d updateOffsets() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float targetX = (float) (random.nextDouble() - 0.5) * 0.042F;
        float targetY = (float) (random.nextDouble() - 0.5) * 0.016F;
        float targetZ = (float) (random.nextDouble() - 0.5) * 0.042F;
        this.offsetX = MathHelper.clamp(this.offsetX + (targetX - this.offsetX) * 0.085F, -0.050F, 0.050F);
        this.offsetY = MathHelper.clamp(this.offsetY + (targetY - this.offsetY) * 0.075F, -0.019F, 0.019F);
        this.offsetZ = MathHelper.clamp(this.offsetZ + (targetZ - this.offsetZ) * 0.085F, -0.050F, 0.050F);
        return new Vec3d(this.offsetX, this.offsetY, this.offsetZ);
    }

    private static float[] anglesTo(ClientPlayerEntity player, Vec3d point) {
        Vec3d d = point.subtract(player.getEyePos());
        double horizontal = Math.hypot(d.x, d.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, horizontal));
        return new float[]{yaw, pitch};
    }

    private static float swayDistanceMultiplier(ClientPlayerEntity player, Entity entity) {
        if (entity == null) {
            return 6.0F;
        }

        if (player.isGliding()) {
            return 2.10F;
        }

        float normalized = MathHelper.clamp(player.distanceTo(entity) / 6.0F, 0.2F, 6.0F);
        return MathHelper.clamp(4.20F - normalized * 6.75F, 1.45F, 2.20F);
    }

    private static float tanhLimit(float value, float max) {
        return max <= 0.0F ? 0.0F : (float) (max * Math.tanh(value / max));
    }

    private static float wave(float amplitude, boolean cosine, float frequencyHz, double phase) {
        double t = System.currentTimeMillis() / 1000.0 * frequencyHz * (2.0 * Math.PI) + phase;
        return (float) (cosine ? Math.cos(t) : Math.sin(t)) * amplitude;
    }

    private static float gcd() {
        double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }
}
