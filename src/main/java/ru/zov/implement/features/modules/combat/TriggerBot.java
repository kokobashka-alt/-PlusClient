package ru.zov.implement.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RaytracingUtil;

public class TriggerBot extends Module {

    private LivingEntity target = null;

    // Настройки (взяты из исходника)
    public final MultiSelectSetting conditions = new MultiSelectSetting("Условия", "Настройки атаки")
            .value("Crits With Space", "Break Shield", "UnPress Shield", "No Attack When Eat")
            .selected("Break Shield", "UnPress Shield");

    public final BooleanSetting weaponOnly = new BooleanSetting("Только с оружием", "Бить только с мечом/топором")
            .setValue(true);

    public final ValueSetting reach = new ValueSetting("Дистанция", "Максимальная дистанция удара")
            .setValue(3.0f).range(1.0f, 6.0f);

    public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Бить сквозь блоки")
            .setValue(false);

    public static TriggerBot getInstance() {
        return (TriggerBot) Instance.get(TriggerBot.class);
    }

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        this.setup(conditions, weaponOnly, reach, throughWalls);
    }

    @Override
    public void deactivate() {
        this.target = null;
        super.deactivate();
    }

    @EventHandler
    @Native
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Check for weapon using the item display name; SwordItem is unavailable in this Minecraft version.
        if (weaponOnly.isValue() && !isWeapon(mc.player.getMainHandStack())) {
            this.target = null;
            return;
        }

        // 2. Рейкаст в сущность
        EntityHitResult hit = (EntityHitResult) RaytracingUtil.raytraceEntity(
                reach.getValue(),
                AngleUtil.cameraAngle(),
                s -> !FriendUtils.isFriend(s.getName().getString()) && s instanceof LivingEntity
        );

        // Если не нашли цель напрямую, и включен режим "сквозь стены"
        if (hit == null && throughWalls.isValue()) {
            // Ищем ближайшую цель в радиусе
            target = findTargetThroughWalls();
        } else if (hit != null && hit.getEntity() instanceof LivingEntity entity) {
            this.target = entity;
        } else {
            this.target = null;
        }

        if (this.target == null) return;

        // 3. Проверка кулдауна
        if (mc.player.getAttackCooldownProgress(0.5F) <= 0.9F) {
            return;
        }

        // 4. Проверка условий
        if (conditions.isSelected("No Attack When Eat") && mc.player.isUsingItem()) {
            return;
        }
        if (conditions.isSelected("Crits With Space")
                && mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            return;
        }

        // 5. Отжимаем щит
        if (conditions.isSelected("UnPress Shield")
                && mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        // 6. Атакуем
        mc.interactionManager.attackEntity(mc.player, this.target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof AxeItem) return true;

        String name = stack.getItem().getName().getString().toLowerCase();
        return name.contains("sword") || name.contains("меч") || name.contains("mace");
    }

    private LivingEntity findTargetThroughWalls() {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double range = reach.getValue();

        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player || !living.isAlive()) continue;
            if (living instanceof PlayerEntity p && FriendUtils.isFriend(p)) continue;

            double dist = mc.player.distanceTo(living);
            if (dist <= range && dist < bestDist) {
                best = living;
                bestDist = dist;
            }
        }
        return best;
    }

    public LivingEntity getTarget() {
        return this.target;
    }
}
