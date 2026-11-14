package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * FunTime rotation ported from nexwear (Rotation027), see {@link FuntimeOrbitAim}. The aim point is an orbit
 * around the target computed here, so the rotation Aura passes in is only used as the camera rotation while no
 * target is held (entity == null).
 */
public class FuntimeRotationMode extends RotationMode {
    private final FuntimeOrbitAim aim = new FuntimeOrbitAim(System.nanoTime());

    public FuntimeRotationMode() {
        super("FunTime");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec, Entity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return target;
        }

        FuntimeOrbitAim.Result result;
        if (entity instanceof LivingEntity living && living.isAlive()) {
            result = this.aim.aim(new FuntimeOrbitAim.Frame(
                    player.getX(), player.getY(), player.getZ(), player.getEyeY(),
                    living.getX(), living.getY(), living.getZ(), living.getHeight(),
                    current.getYaw(), current.getPitch(), System.currentTimeMillis()));
        } else {
            result = this.aim.release(current.getYaw(), current.getPitch(), target.getYaw(), target.getPitch());
        }

        return new Rotation(result.yaw(), result.pitch());
    }

    public void reset() {
        this.aim.reset();
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
