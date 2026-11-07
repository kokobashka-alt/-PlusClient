package ru.zov.implement.features.modules.combat;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.entity.SimulatedPlayer;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.events.player.EntityUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.modules.render.ProjectilePrediction;

public class TargetPearl extends Module {

    private final StopWatch stopWatch = new StopWatch();

    private final SelectSetting modeSetting = new SelectSetting(
        "Mode", "When will target pearl work", "Bind", "Always").selected("Always");
    private final SelectSetting targetSetting = new SelectSetting(
        "Targets", "Targets for which pearls will be thrown", "Aura Target", "All").selected("Aura Target");
    private final BindSetting throwSetting = new BindSetting("Throw", "Throw Key")
        .visible(() -> modeSetting.isSelected("Bind"));
    private final ValueSetting distanceSetting = new ValueSetting(
        "Distance", "Target Pearl Trigger Distance").setValue(6.0f).range(3, 15);

    public TargetPearl() {
        super("TargetPearl", "Target Pearl", ModuleCategory.COMBAT);
        setup(new Setting[]{modeSetting, targetSetting, throwSetting, distanceSetting});
    }

    // ---- 1. Подмена владельца у чужих жемчужин, чтобы Aura их видела ----
    @EventHandler
    public void onEntityUpdate(EntityUpdateEvent e) {
        if (!e.type().equals(EntityUpdateEvent.Type.ADD)) return;
        if (!(e.entity() instanceof EnderPearlEntity pearl)) return;
        if (mc.world == null) return;

        Optional<Entity> owner = mc.world.getPlayers().stream()
            .filter(p -> p.getEyePos().distanceTo(pearl.getPos()) <= 6.0)
            .min(Comparator.comparingDouble(p -> p.distanceTo(pearl)))
            .map(Entity.class::cast);

        owner.ifPresent(pearl::setOwner);
    }

    // ---- 2. Основная логика броска ----
    @EventHandler(0)
    @Native
    public void onTick(TickEvent e) {
        int protocol = PlayerIntersectionUtil.getProtocolVersion();
        if (protocol < 755 || protocol > 766) {
            Notifications.getInstance().addList(
                "[Target Pearl] нужна версия " + Formatting.RED + "1.17-1.20.6", 2000L);
            disableSoft();
            return;
        }

        LivingEntity target = Aura.getInstance().getLastTarget();
        Slot slot = findPearlSlot();
        if (slot == null || ItemUtil.hasCooldown(slot.getStack())) return;
        if (!stopWatch.finished(1000.0)) return;
        if (modeSetting.isSelected("Bind") && !PlayerIntersectionUtil.isKey(throwSetting)) return;

        // Все жемчужины в мире
        List<EnderPearlEntity> pearls = PlayerIntersectionUtil.streamEntities()
            .filter(EnderPearlEntity.class::isInstance)
            .map(EnderPearlEntity.class::cast)
            .toList();

        // Своя уже летит — ждём
        if (pearls.stream().anyMatch(p -> mc.player.equals(p.getOwner()))) {
            stopWatch.reset();
            return;
        }

        Vec3d eye = SimulatedPlayer
            .simulateLocalPlayer(PlayerInventoryComponent.getTaskTime())
            .getEyePos();

        // Копия стека — swapAndUse выест оригинал
        EnderPearlEntity simulated =
            new EnderPearlEntity(mc.world, mc.player, slot.getStack().copy());

        ProjectilePrediction prediction = ProjectilePrediction.getInstance();

        pearls.stream()
            .filter(p -> p.getOwner() != null
                && !FriendUtils.isFriend(p.getOwner())
                && (targetSetting.isSelected("All")
                    || (target != null && target.equals(p.getOwner()))))
            .min(Comparator.comparingDouble(p -> RotationController.computeRotationDifference(
                AngleUtil.cameraAngle(),
                AngleUtil.calculateAngle(prediction.calcTrajectory(p).getPos()))))
            .ifPresent(pearl -> {
                HitResult targetResult = prediction.calcTrajectory(pearl);
                double triggerDist = distanceSetting.getInt();
                if (mc.player.getPos().distanceTo(targetResult.getPos()) <= triggerDist) {
                    return;
                }
                Angle resultAngle = AngleUtil.fromVec3d(targetResult.getPos().subtract(eye));

                IntStream.rangeClosed(-89, 89)
                    .mapToObj(pitch -> new Angle(resultAngle.getYaw(), (float) pitch))
                    .filter(angle -> prediction
                        .checkTrajectory(eye, angle.toVector(), simulated, 1.5, false)
                        .getPos()
                        .distanceTo(targetResult.getPos()) <= 1.0)
                    .max(Comparator.comparingDouble(Angle::getPitch))
                    .ifPresent(angle -> {
                        PlayerInventoryUtil.swapAndUse(slot, angle, true);
                        pearl.setOwner(null);
                        stopWatch.reset();
                    });
            });
    }

    // ---- 3. Поиск слота под жемчуг (fallback, если getSlot нет) ----
    private Slot findPearlSlot() {
        try {
            // если в вашей сборке есть готовый хелпер
            Slot slot = PlayerInventoryUtil.getSlot(Items.ENDER_PEARL);
            if (slot != null) return slot;
        } catch (NoSuchMethodError ignored) {
            // нет метода — идём через slots()
        }
        return PlayerInventoryUtil.slots()
            .filter(s -> s.getStack().isOf(Items.ENDER_PEARL))
            .findFirst()
            .orElse(null);
    }

    // ---- 4. Мягкое выключение (совместимо с разными версиями базового Module) ----
    private void disableSoft() {
        // В разных сборках zov это либо toggle(), либо setEnabled(false),
        // либо toggleSilent(false). Оставьте тот, что компилируется.
        try {
            this.toggle();
        } catch (NoSuchMethodError ex) {
            this.setEnabled(false);
        }
    }
}
