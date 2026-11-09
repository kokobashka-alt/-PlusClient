package ru.zov.implement.features.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CrystalAura extends Module {

    public final ValueSetting targetRange = new ValueSetting("Радиус цели", "Дистанция поиска игрока")
            .setValue(6.0f).range(3.0f, 10.0f);
    public final ValueSetting placeRange = new ValueSetting("Дальность установки", "На каком расстоянии ставить")
            .setValue(4.5f).range(2.0f, 6.0f);
    public final ValueSetting breakRange = new ValueSetting("Дальность взрыва", "На каком расстоянии взрывать")
            .setValue(3.0f).range(1.0f, 6.0f);
    public final BooleanSetting autoPlace = new BooleanSetting("Авто установка", "Ставить обсидиан, если нет блока")
            .setValue(true);

    private PlayerEntity target;
    private long lastActionTime;

    public CrystalAura() {
        super("CrystalAura", "Автоматически ставит и взрывает кристаллы", ModuleCategory.COMBAT);
        this.setup(new Setting[]{targetRange, placeRange, breakRange, autoPlace});
    }

    public static CrystalAura getInstance() {
        return (CrystalAura) Instance.get(CrystalAura.class);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Задержка между действиями, чтобы не спамить пакетами
        if (System.currentTimeMillis() - lastActionTime < 50) return;

        // 1. Поиск цели
        if (target == null || !target.isAlive() || mc.player.distanceTo(target) > targetRange.getValue()) {
            target = findTarget();
        }

        if (target == null) return;

        // 2. Взрыв кристаллов рядом с нами
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                mc.player.getBoundingBox().expand(breakRange.getValue()),
                crystal -> true
        );

        if (!crystals.isEmpty()) {
            EndCrystalEntity crystal = crystals.get(0);
            if (mc.player.distanceTo(crystal) <= breakRange.getValue()) {
                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);
                lastActionTime = System.currentTimeMillis();
                return;
            }
        }

        // 3. Поиск места и установка
        BlockPos placePos = findPlacePosition();
        if (placePos != null) {
            // Проверяем, нет ли уже кристалла на этом месте
            if (!mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(placePos).expand(0.5), c -> true).isEmpty()) {
                return;
            }

            int crystalSlot = findItemSlot(Items.END_CRYSTAL);
            if (crystalSlot != -1) {
                int oldSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = crystalSlot;

                // Клик по верхней грани блока
                BlockHitResult hit = new BlockHitResult(
                        new Vec3d(placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5),
                        Direction.UP,
                        placePos,
                        false
                );

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);

                mc.player.getInventory().selectedSlot = oldSlot;
                lastActionTime = System.currentTimeMillis();
            }
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            double dist = mc.player.distanceTo(p);
            if (dist < bestDist && dist <= targetRange.getValue()) {
                best = p;
                bestDist = dist;
            }
        }
        return best;
    }

    private BlockPos findPlacePosition() {
        BlockPos targetPos = target.getBlockPos();
        List<BlockPos> candidates = new ArrayList<>();

        // Проверяем блоки вокруг цели
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = targetPos.add(x, y, z);

                    // Если это обсидиан/бедрок, и над ним пустое место
                    if ((mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK))
                            && mc.world.isAir(pos.up())
                            && mc.player.getEyePos().distanceTo(Vec3d.of(pos)) <= placeRange.getValue()) {
                        candidates.add(pos);
                    }
                }
            }
        }

        // Сортируем по дистанции до игрока и берем ближайший
        if (!candidates.isEmpty()) {
            candidates.sort(Comparator.comparingDouble(pos ->
                    mc.player.getEntityPos().squaredDistanceTo(Vec3d.of(pos))
            ));
            return candidates.get(0);
        }

        return null;
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
