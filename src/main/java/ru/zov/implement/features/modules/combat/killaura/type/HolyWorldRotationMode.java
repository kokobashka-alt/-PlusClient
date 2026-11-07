package ru.zov.implement.features.modules.combat.killaura.type;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;
import ru.zov.implement.features.modules.combat.killaura.util.RaycastUtil;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.util.RotationManager;
import ru.zov.implement.features.modules.combat.killaura.math.RotationMath;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class HolyWorldRotationMode extends RotationMode {
    public HolyWorldRotationMode() {
        super("Хуйня не используйте");
    }

    @Override
    public Rotation process(Rotation class007Var, Rotation class007Var2, Vec3d vec3d, Entity entity) {
        Rotation class007VarCalculateRotationDifference = RotationMath.INSTANCE.calculateRotationDifference(class007Var2, class007Var);
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if ((player.age & 1) == 1) {
            return class007Var;
        }
        float yaw = class007VarCalculateRotationDifference.getYaw();
        float pitch = class007VarCalculateRotationDifference.getPitch();
        return new Rotation(MathHelper.wrapDegrees(class007Var.getYaw() + Math.copySign(Math.min(Math.abs(yaw), 80.0f), yaw) + ((float) (Math.sin(System.currentTimeMillis() / 40.0d) * ((double) randomLerp(3.0f, 4.0f))))), MathHelper.clamp(class007Var.getPitch() + Math.copySign(Math.min(Math.abs(pitch), ((entity != null && isCritReady(entity, player)) || entity == null) ? Math.abs(pitch) : 4.0f), pitch) + ((float) (Math.cos(System.currentTimeMillis() / 40.0d) * ((double) randomLerp(1.0f, 2.0f)))), -89.0f, 89.0f));
    }

    public boolean isCritReady(Entity entity, ClientPlayerEntity clientPlayerEntity) {
        Aura class878Var = (Aura) AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
        if (class878Var == null) {
            return false;
        }
        return (!class878Var.canCrit(clientPlayerEntity, 1) || class878Var.target() == null || RaycastUtil.rayTrace(RotationMath.INSTANCE.rotationToVector(RotationManager.INSTANCE.getCurrentRotation()), 3.0d, entity.getBoundingBox())) ? false : true;
    }

    public boolean isInsideEntity(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
        double width = livingEntity.getWidth() / 2.0f;
        return new Vec3d((livingEntity.getX() - clientPlayerEntity.getX()) + MathUtil.clamp(clientPlayerEntity.getX() - livingEntity.getX(), -width, width), (livingEntity.getY() - clientPlayerEntity.getEyeY()) + MathUtil.clamp(clientPlayerEntity.getEyeY() - livingEntity.getY(), 0.0d, (double) livingEntity.getHeight()), (livingEntity.getZ() - clientPlayerEntity.getZ()) + MathUtil.clamp(clientPlayerEntity.getZ() - livingEntity.getZ(), -width, width)).length() == 0.0d;
    }

    public float randomLerp(float f, float f2) {
        return MathHelper.lerp(ThreadLocalRandom.current().nextFloat(), f, f2);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.0d, 0.0d, 0.0d);
    }
}
