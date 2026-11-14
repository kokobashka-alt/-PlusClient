package ru.zov.implement.features.modules.combat;

import java.util.Comparator;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.keyboard.MouseRotationEvent;
import ru.zov.implement.events.player.TickEvent;

public class AimAssist extends Module {

    private LivingEntity target;

    public final ValueSetting distance = new ValueSetting("Дистанция", "Максимальная дистанция цели")
            .setValue(3.5f).range(1.0f, 8.0f);
    public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Работать только если в руке меч/топор")
            .setValue(false);

    public AimAssist() {
        super("AimAssist", "CombatAim", ModuleCategory.COMBAT);
        this.setup(new Setting[]{distance, onlyWeapon});
    }

    public static AimAssist getInstance() {
        return (AimAssist) Instance.get(AimAssist.class);
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public void activate() {
        target = null;
    }

    @Override
    public void deactivate() {
        target = null;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || !weaponAllowed()) {
            target = null;
            return;
        }

        updateTarget();
    }

    @EventHandler
    public void onMouseRotation(MouseRotationEvent event) {
        if (target == null || mc.player == null) return;

        float deltaX = event.getCursorDeltaX();
        if (deltaX == 0.0f) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d center = target.getBoundingBox().getCenter();
        double dx = center.x - eye.x;
        double dz = center.z - eye.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());

        boolean movingToTarget = deltaX * yawDiff > 0.0f;
        float multiplier = movingToTarget ? 1.5f : 0.5f;

        float speed = Math.abs(deltaX);
        float strength = MathHelper.clamp(1.0f - speed / 50.0f, 0.0f, 1.0f);
        multiplier = MathHelper.lerp(strength, 1.0f, multiplier);

        event.setCursorDeltaX(applyGCD(deltaX * multiplier, deltaX));
    }

    private boolean weaponAllowed() {
        if (!onlyWeapon.isValue()) return true;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        String name = stack.getItem().getName().getString().toLowerCase();
        return name.contains("sword") || name.contains("axe") || name.contains("меч") || name.contains("топор") || name.contains("mace");
    }

    private void updateTarget() {
        double range = distance.getValue();
        double rangeSq = range * range;

        if (target == null || mc.player.squaredDistanceTo(target) > rangeSq || !isValidTarget(target)) {
            target = findTarget(range);
        }
    }

    private LivingEntity findTarget(double range) {
        Box box = mc.player.getBoundingBox().expand(range);
        double rangeSq = range * range;
        return mc.world.getEntitiesByClass(LivingEntity.class, box, this::isValidTarget)
                .stream()
                .filter(entity -> mc.player.squaredDistanceTo(entity) <= rangeSq)
                .min(Comparator.comparingDouble(this::fovTo))
                .orElse(null);
    }

    // Угол между направлением взгляда и целью (по позиции ног, как getEntityPos в исходнике)
    private double fovTo(LivingEntity entity) {
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d dir = entity.getEntityPos().subtract(mc.player.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(dir), -1.0, 1.0)));
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity.isDead() || !entity.isAlive()) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof PlayerEntity player) {
            return player != mc.player && !player.isSpectator() && !FriendUtils.isFriend(player);
        }
        return true;
    }

    private static float gcd() {
        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }

    private static float applyGCD(float angle, float previous) {
        float gcd = gcd();
        return previous + Math.round((angle - previous) / gcd) * gcd;
    }
}
