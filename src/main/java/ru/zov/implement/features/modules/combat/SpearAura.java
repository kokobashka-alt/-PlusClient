package ru.zov.implement.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;

public class SpearAura extends Module {

    public final ValueSetting aimRadius = new ValueSetting("Радиус наводки", "Угол в градусах")
            .setValue(45.0f).range(5.0f, 180.0f);
            
    public final ValueSetting attackRange = new ValueSetting("Дистанция удара", "На каком расстоянии бить")
            .setValue(3.0f).range(1.0f, 6.0f);

    public final BooleanSetting autoSpear = new BooleanSetting("Авто копье", "Автоматически брать копье в руку")
            .setValue(true);

    private PlayerEntity target;

    public SpearAura() {
        super("SpearAura", "Аура для копья", ModuleCategory.COMBAT);
        this.setup(new Setting[]{aimRadius, attackRange, autoSpear});
    }

    public static SpearAura getInstance() {
        return (SpearAura) Instance.get(SpearAura.class);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Ищем цель, если текущая мертва или далеко
        if (target == null || !target.isAlive() || mc.player.distanceTo(target) > 128) {
            target = findTarget();
        }

        if (target != null) {
            // 1. Берем копье в руку
            if (autoSpear.isValue()) {
                int spearSlot = findSpearSlot();
                if (spearSlot != -1 && mc.player.getInventory().selectedSlot != spearSlot) {
                    mc.player.getInventory().selectedSlot = spearSlot;
                }
            }

            // 2. Наводимся на цель (центр хитбокса)
            Vec3d aim = target.getPos().add(0, target.getHeight() * 0.5, 0);
            double dx = aim.x - mc.player.getX();
            double dy = aim.y - mc.player.getEyeY();
            double dz = aim.z - mc.player.getZ();
            
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            
            // Плавно поворачиваем камеру
            mc.player.setYaw(yaw);
            mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));

            // 3. Бьем, если цель в радиусе и кулдаун прошел
            if (mc.player.distanceTo(target) <= attackRange.getValue() 
                    && mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        Vec3d look = mc.player.getRotationVec(1.0f);

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            
            double dist = mc.player.distanceTo(p);
            if (dist > 128 || dist >= bestDist) continue;

            Vec3d delta = p.getPos().subtract(mc.player.getPos()).normalize();
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(delta), -1.0, 1.0)));
            
            if (angle <= aimRadius.getValue()) {
                best = p;
                bestDist = dist;
            }
        }
        return best;
    }

    private int findSpearSlot() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            // В 1.21.11 копье может быть кастомным, поэтому проверяем по имени
            String name = item.getName().getString().toLowerCase();
            if (name.contains("spear") || name.contains("копье") || item == Items.TRIDENT) {
                return i;
            }
        }
        return -1;
    }
}
