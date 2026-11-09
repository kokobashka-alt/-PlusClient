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
    private long lastTargetSwitch;

    public final SelectSetting rotationMode = new SelectSetting("Ротация", "Тип наводки")
            .value("Linear", "Smooth", "Instant").selected("Linear");
    public final ValueSetting speed = new ValueSetting("Скорость", "Скорость наводки")
            .setValue(3.0f).range(1.0f, 10.0f);
    public final ValueSetting distance = new ValueSetting("Дистанция", "Максимальная дистанция цели")
            .setValue(4.5f).range(1.0f, 8.0f);
    public final ValueSetting fov = new ValueSetting("FOV", "Угол поиска цели")
            .setValue(90.0f).range(1.0f, 360.0f);
    public final SelectSetting priority = new SelectSetting("Приоритет", "Как выбирать цель")
            .value("Distance", "Health", "Angle").selected("Distance");
    public final BooleanSetting sticky = new BooleanSetting("Сквозь стены", "Игнорировать стены при наведении")
            .setValue(false);
    public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Работать только если в руке меч/топор")
            .setValue(false);

    public AimAssist() {
        super("AimAssist", "Автоматическая наводка на цель", ModuleCategory.COMBAT);
        this.setup(new Setting[]{rotationMode, speed, distance, fov, priority, sticky, onlyWeapon});
    }

    public static AimAssist getInstance() {
        return (AimAssist) Instance.get(AimAssist.class);
    }

    @Override
    public void activate() {
        cachedTarget = null;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Проверка на оружие (без SwordItem/AxeItem, используя урон)
        if (onlyWeapon.isValue()) {
            ItemStack stack = mc.player.getMainHandStack();
            // Проверяем, что у предмета есть урон атаки > 1 (мечи, топоры)
            if (stack.isEmpty()) return;
            
            // В 1.21.11 проверка урона может быть сложной, поэтому используем простую проверку по имени
            String name = stack.getItem().getName().getString().toLowerCase();
            if (!name.contains("sword") && !name.contains("axe") && !name.contains("меч") && !name.contains("топор") && !name.contains("mace")) {
                return;
            }
        }

        LivingEntity target = findTarget();
        if (target == null) {
            cachedTarget = null;
            return;
        }

        Vec3d targetPoint = target.getEyePos().add(0, 0.1, 0);
        Vec3d diff = targetPoint.subtract(mc.player.getEyePos());

        float targetYaw = (float) (Math.toDegrees(Math.atan2(-diff.x, diff.z)));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        targetPitch = MathHelper.clamp(targetPitch, -90.0f, 90.0f);

        if (cachedTarget == null || System.currentTimeMillis() - lastTargetSwitch > 300) {
            cachedTarget = target;
            lastTargetSwitch = System.currentTimeMillis();
        }

        float speedFactor = speed.getValue();
        float dYaw = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float dPitch = targetPitch - mc.player.getPitch();

        mc.player.setYaw(mc.player.getYaw() + dYaw / speedFactor);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + dPitch / speedFactor, -90.0f, 90.0f));
    }

    private LivingEntity findTarget() {
        candidates.clear();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        double maxDist = distance.getValue();
        double maxFov = fov.getValue() / 2.0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player || !living.isAlive()) continue;
            if (living instanceof PlayerEntity p && FriendUtils.isFriend(p)) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > maxDist) continue;

            if (!sticky.isValue() && !mc.player.canSee(living)) continue;

            Vec3d delta = living.getEntityPos().subtract(eyePos).normalize();
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(lookVec.dotProduct(delta), -1.0, 1.0)));
            if (angle > maxFov) continue;

            candidates.add(living);
        }

        if (candidates.isEmpty()) return null;

        String mode = priority.getValue();
        Comparator<LivingEntity> comparator;
        if ("Health".equals(mode)) {
            comparator = Comparator.comparingDouble(LivingEntity::getHealth);
        } else if ("Angle".equals(mode)) {
            comparator = Comparator.comparingDouble(e -> {
                Vec3d d = e.getEntityPos().subtract(eyePos).normalize();
                return Math.acos(MathHelper.clamp(lookVec.dotProduct(d), -1.0, 1.0));
            });
        } else {
            comparator = Comparator.comparingDouble(mc.player::distanceTo);
        }

        candidates.sort(comparator);
        return candidates.get(0);
    }
}
