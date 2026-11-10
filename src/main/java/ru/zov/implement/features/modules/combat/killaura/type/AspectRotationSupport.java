package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Pure helpers shared by the WhiteRise, ShardAC and ShardAC-AI rotation modes (ported from the Aspect client). */
final class AspectRotationSupport {
    private static final double LOOK_RAY_LENGTH = 1950.0;

    private AspectRotationSupport() {
    }

    /** True when the current look ray already passes through the given box (or the eye is inside it). */
    static boolean lookRayHitsBox(ClientPlayerEntity player, float yaw, float pitch, Box box) {
        Vec3d eye = player.getEyePos();
        if (box.contains(eye)) {
            return true;
        }
        Vec3d look = player.getRotationVector(pitch, yaw);
        return box.raycast(eye, eye.add(look.multiply(LOOK_RAY_LENGTH))).isPresent();
    }

    /** Eased slow factor: drops fast to 0.05 when the eye is inside the target hitbox, recovers slowly otherwise. */
    static float nextSlowFactor(float slow, Box targetBox, Vec3d eye) {
        boolean close = targetBox.expand(-0.3).contains(eye) || targetBox.expand(0.3).contains(eye);
        float goal = close ? 0.05F : 1.0F;
        float rate = close ? 1.0F : 0.01F;
        return MathHelper.clamp(slow + (goal - slow) * rate, 0.05F, 1.0F);
    }

    /** True when an attack is about to be (or can be) delivered: cooled down enough, or falling for a crit. */
    static boolean attackWindow(ClientPlayerEntity player) {
        return player.getAttackCooldownProgress(0.5F) >= 0.65F
                || (!player.isOnGround() && player.getVelocity().y < 0.0 && player.fallDistance > 0.0F);
    }

    static float clampPitch(float pitch, float min, float max) {
        return MathHelper.clamp(pitch, min, max);
    }
}
