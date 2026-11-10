package ru.zov.implement.features.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;

public class AimAssist extends Module {

    private final List<LivingEntity> candidates = new ArrayList<>();
    private LivingEntity cachedTarget;
    private Vec3d lastSelfPosition;
    private double requiredSpeed = 1.0;
    private boolean hasTarget;
    private long lastTargetSwitch;
    private long lastReset;

    public final SelectSetting rotationMode = new SelectSetting("Ротация", "Тип наводки")
            .value("Linear", "Smooth", "Instant").selected("Linear");
    public final ValueSetting speed = new ValueSetting("Скорость", "Скорость наводки (чем меньше, тем плавнее)")
            .setValue(3.0f).range(1.0f, 10.0f);
    public final ValueSetting distance = new ValueSetting("Дистанция", "Максимальная дистанция цели")
            .setValue(4.5f).range(1.0f, 8.0f);
    public final ValueSetting fov = new ValueSetting("FOV", "Угол поиска цели")
            .setValue(90.0f).range(1.0f, 360.0f);
    public final SelectSetting priority = new SelectSetting("Приоритет", "Как выбирать цель")
            .value("Distance", "Health", "Angle").selected("Distance");
    public final ValueSetting slowThreshold = new ValueSetting("Задержка ротации", "Пауза между шагами наводки и до сброса цели (сек)")
            .setValue(0.4f).range(0.0f, 3.0f);
    public final ValueSetting legitSpeed = new ValueSetting("Легит скорость", "Делитель пройденного игроком пути за тик")
            .setValue(1.0f).range(0.1f, 5.0f);
    public final BooleanSetting sticky = new BooleanSetting("Сквозь стены", "Игнорировать стены при наведении")
            .setValue(false);
    public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Работать только если в руке меч/топор")
            .setValue(false);

    public AimAssist() {
        super("AimAssist", "CombatAim", ModuleCategory.COMBAT);
        this.setup(new Setting[]{rotationMode, speed, slowThreshold, legitSpeed, distance, fov, priority, sticky, onlyWeapon});
    }

    public static AimAssist getInstance() {
        return (AimAssist) Instance.get(AimAssist.class);
    }

    @Override
    public void activate() {
        cachedTarget = null;
        hasTarget = false;
        lastSelfPosition = null;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;

        if (onlyWeapon.isValue()) {
            ItemStack stack = mc.player.getMainHandStack();
            if (stack.isEmpty()) return;
            String name = stack.getItem().getName().getString().toLowerCase();
            if (!name.contains("sword") && !name.contains("axe") && !name.contains("меч") && !name.contains("топор") && !name.contains("mace")) {
                return;
            }
        }

        Vec3d selfPosition = mc.player.getEyePos();
        requiredSpeed = speed.getValue();
        if (hasTarget && lastSelfPosition != null) {
            double travelled = selfPosition.distanceTo(lastSelfPosition);
            requiredSpeed = travelled / legitSpeed.getValue();
        }
        lastSelfPosition = selfPosition;

        long now = System.nanoTime();
        long resetDelay = (long) (slowThreshold.getValue() * 1.0E9);

        LivingEntity target = findTarget(selfPosition);
        if (target == null) {
            if (resetDelay < 1L || cachedTarget == null || now - lastReset >= resetDelay) {
                resetRotation(now);
            }
            return;
        }

        if (cachedTarget == null || cachedTarget != target) {
            cachedTarget = target;
            lastTargetSwitch = now;
            hasTarget = true;
            return;
        }

        if (now - lastTargetSwitch < resetDelay) return;

        // Точка наведения (центр хитбокса)
        Vec3d targetPoint = target.getEyePos().add(0, 0.1, 0);
        double currentYaw = mc.player.getYaw();
        double currentPitch = mc.player.getPitch();

        double dx = targetPoint.x - selfPosition.x;
        double dy = targetPoint.y - selfPosition.y;
        double dz = targetPoint.z - selfPosition.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double targetPitch = -Math.toDegrees(Math.atan2(dy, horizontal));

        double yawDelta = wrap(targetYaw - currentYaw);
        double pitchDelta = targetPitch - currentPitch;

        double yaw;
        double pitch;
        if (rotationMode.isSelected("Instant")) {
            yaw = currentYaw + yawDelta;
            pitch = currentPitch + pitchDelta;
        } else {
            double divisor = Math.max(requiredSpeed, 1.0);
            yaw = currentYaw + yawDelta / divisor;
            pitch = currentPitch + pitchDelta / divisor;
        }

        mc.player.setYaw((float) clampYaw(yaw));
        mc.player.setPitch((float) clampPitch(pitch));
        lastTargetSwitch = now;
        hasTarget = true;
    }

    private void resetRotation(long now) {
        cachedTarget = null;
        hasTarget = false;
        lastReset = now;
    }

    private LivingEntity findTarget(Vec3d eyePos) {
        candidates.clear();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        double maxDist = distance.getValue();
        double maxFov = fov.getValue() / 2.0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player || !living.isAlive()) continue;
            if (living instanceof PlayerEntity p && FriendUtils.isFriend(p)) continue;

            if (!sticky.isValue() && !mc.player.canSee(living)) continue;
            if (distanceTo(eyePos, living) > maxDist) continue;

            Vec3d delta = living.getEntityPos().subtract(eyePos).normalize();
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(lookVec.dotProduct(delta), -1.0, 1.0)));
            if (angle > maxFov) continue;

            candidates.add(living);
        }

        if (candidates.isEmpty()) return null;

        String mode = priority.getSelected();
        Comparator<LivingEntity> comparator;
        if ("Health".equals(mode)) {
            comparator = Comparator.comparingDouble(LivingEntity::getHealth);
        } else if ("Angle".equals(mode)) {
            comparator = Comparator.comparingDouble(en -> yawAngleTo(eyePos, en));
        } else {
            comparator = Comparator.comparingDouble(en -> distanceTo(eyePos, en));
        }

        candidates.sort(comparator);
        return candidates.get(0);
    }

    private static double distanceTo(Vec3d from, LivingEntity entity) {
        return entity.getEntityPos().distanceTo(from);
    }

    private double yawAngleTo(Vec3d from, LivingEntity entity) {
        Vec3d p = entity.getEntityPos();
        double yaw = Math.toDegrees(Math.atan2(-(p.x - from.x), p.z - from.z));
        return Math.abs(wrap(mc.player.getYaw() - yaw));
    }

    private static double wrap(double angle) {
        angle %= 360.0;
        if (angle >= 180.0) angle -= 360.0;
        if (angle < -180.0) angle += 360.0;
        return angle;
    }

    private static double clampYaw(double yaw) {
        return yaw % 360.0;
    }

    private static double clampPitch(double pitch) {
        return Math.max(-90.0, Math.min(90.0, pitch));
    }
}
