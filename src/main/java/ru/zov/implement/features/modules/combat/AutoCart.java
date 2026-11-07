package ru.zov.implement.features.modules.combat;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.entity.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;

public class AutoCart extends Module {
    private static final double BLOCK_REACH = 4.2;
    private static final double ENTITY_REACH = 2.9;
    private static final int SEARCH_RADIUS = 5;

    private static final int STAGE_FIND = 0;
    private static final int STAGE_SWAP_CART = 1;
    private static final int STAGE_PLACE_CART = 2;
    private static final int STAGE_SWAP_IGNITER = 3;
    private static final int STAGE_FLINT_IGNITE = 4;
    private static final int STAGE_BOW_DRAW = 5;
    private static final int STAGE_RESTORE = 6;

    private final BooleanSetting ignite = new BooleanSetting(
        "Поджигать", "Взрывать вагонетку после установки").setValue(true);
    private final SelectSetting igniteMode = new SelectSetting(
        "Поджиг", "Способ детонации", "Лук", "Лук", "Огниво")
        .visible(() -> this.ignite.isValue());
    private final ValueSetting drawTicks = new ValueSetting(
        "Натяжка", "Сколько тиков натягивать лук", 20.0f, 5.0f, 25.0f, 1.0f)
        .visible(() -> this.ignite.isValue() && this.igniteMode.isSelected("Лук"));
    private final SelectSetting swapMode = new SelectSetting(
        "Свап", "Способ переключения", "Silent", "Silent", "Legit");
    private final BooleanSetting debugMessages = new BooleanSetting(
        "Сообщения", "Писать в чат причины неудач").setValue(true);

    private int stage;
    private int waitTicks;
    private int drawCounter;
    private boolean swapped;
    private boolean silentSwap;
    private BlockPos railPos;
    private int prevSlot = -1;

    public AutoCart() {
        super("AutoCart", "Ставит и взрывает ТНТ-вагонетку на рельсе", ModuleCategory.COMBAT);
        this.setup(new Setting[]{ignite, igniteMode, drawTicks, swapMode, debugMessages});
    }

    public static AutoCart getInstance() {
        return (AutoCart) Instance.get(AutoCart.class);
    }

    @Override
    public void onEnable() {
        this.stage = STAGE_FIND;
        this.waitTicks = 0;
        this.drawCounter = 0;
        this.swapped = false;
        this.silentSwap = swapMode.isSelected("Silent");
        this.railPos = null;
    }

    @Override
    public void onDisable() {
        if (this.stage == STAGE_BOW_DRAW) {
            mc.options.useKey.setPressed(false);
        }
        if (this.swapped) {
            restoreSlot();
            this.swapped = false;
        }
        this.railPos = null;
    }

    private boolean isBowMode() {
        return igniteMode.isSelected("Лук");
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            this.toggle();
            return;
        }

        Entity cart;
        switch (stage) {
            case STAGE_FIND -> {
                railPos = findNearestRail();
                if (railPos == null) { abort("рельсы в радиусе " + BLOCK_REACH + " нет"); return; }

                boolean hasCart = findHotbarSlot(Items.TNT_MINECART) != -1 || findCart() != null;
                if (!hasCart) { abort("нет ТНТ-вагонетки"); return; }

                if (ignite.isValue()) {
                    if (isBowMode()) {
                        if (findHotbarSlot(Items.BOW) == -1) { abort("нет лука"); return; }
                        if (!mc.player.getAbilities().creativeMode && !hasArrows()) { abort("нет стрел"); return; }
                    } else if (findHotbarSlot(Items.FLINT_AND_STEEL) == -1) {
                        abort("нет огнива"); return;
                    }
                }
                silentSwap = swapMode.isSelected("Silent") && (!ignite.isValue() || !isBowMode());
                waitTicks = 0;
                stage = findCart() != null
                    ? (ignite.isValue() ? STAGE_SWAP_IGNITER : STAGE_RESTORE)
                    : STAGE_SWAP_CART;
            }
            case STAGE_SWAP_CART -> {
                if (swapTo(findHotbarSlot(Items.TNT_MINECART))) stage = STAGE_PLACE_CART;
            }
            case STAGE_PLACE_CART -> {
                Vec3d hit = Vec3d.ofCenter(railPos);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hit, Direction.UP, railPos, false));
                mc.player.swingHand(Hand.MAIN_HAND);
                waitTicks = 0;
                stage = ignite.isValue() ? STAGE_SWAP_IGNITER : STAGE_RESTORE;
            }
            case STAGE_SWAP_IGNITER -> {
                cart = findCart();
                if (cart == null) {
                    if (++waitTicks > 8) abort("вагонетка не появилась");
                    return;
                }
                if (isBowMode()) {
                    if (swapTo(findHotbarSlot(Items.BOW))) {
                        drawCounter = 0;
                        stage = STAGE_BOW_DRAW;
                    }
                } else {
                    if (mc.player.getEyePos().distanceTo(cart.getPos()) > ENTITY_REACH) {
                        abort("вагонетка дальше " + ENTITY_REACH); return;
                    }
                    if (swapTo(findHotbarSlot(Items.FLINT_AND_STEEL))) stage = STAGE_FLINT_IGNITE;
                }
            }
            case STAGE_FLINT_IGNITE -> {
                cart = findCart();
                if (cart != null) {
                    mc.interactionManager.interactEntity(mc.player, cart, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                stage = STAGE_RESTORE;
            }
            case STAGE_BOW_DRAW -> {
                cart = findCart();
                if (cart == null) {
                    mc.options.useKey.setPressed(false);
                    abort("вагонетка пропала до выстрела"); return;
                }
                aimAt(cart);
                mc.options.useKey.setPressed(true);
                if (++drawCounter >= (int) drawTicks.getValue()) {
                    mc.options.useKey.setPressed(false);
                    stage = STAGE_RESTORE;
                }
            }
            case STAGE_RESTORE -> {
                if (swapped) { restoreSlot(); swapped = false; }
                this.toggle();
            }
        }
    }

    private void aimAt(Entity cart) {
        Vec3d point = cart.getPos().add(0, cart.getHeight() * 0.5, 0);
        RotationController.getInstance().rotateTo(
            AngleUtil.fromVec3d(point.subtract(mc.player.getEyePos())));
    }

    private boolean hasArrows() {
        return findItemAnywhere(Items.ARROW) != -1
            || findItemAnywhere(Items.TIPPED_ARROW) != -1
            || findItemAnywhere(Items.SPECTRAL_ARROW) != -1;
    }

    private void abort(String reason) {
        if (debugMessages.isValue()) {
            Notifications.getInstance().addList("AutoCart: " + reason, 2000L);
        }
        if (swapped) { restoreSlot(); swapped = false; }
        this.toggle();
    }

    private BlockPos findNearestRail() {
        BlockPos center = mc.player.getBlockPos();
        Vec3d eyes = mc.player.getEyePos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(
                center.add(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                center.add(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
            if (isRail(mc.world.getBlockState(pos))) {
                double d = eyes.distanceTo(Vec3d.ofCenter(pos));
                if (d <= BLOCK_REACH && d < bestDist) {
                    best = pos.toImmutable();
                    bestDist = d;
                }
            }
        }
        return best;
    }

    private boolean isRail(BlockState state) {
        return state.getBlock() instanceof AbstractRailBlock;
    }

    private Entity findCart() {
        if (railPos == null) return null;
        Vec3d center = Vec3d.ofCenter(railPos);
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntMinecartEntity && entity.isAlive()
                && entity.getPos().distanceTo(center) < 2.0) {
                return entity;
            }
        }
        return null;
    }

    /** Возвращает hotbar-слот (0..8) с предметом, либо -1. */
    private int findHotbarSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item)) return i;
        }
        return -1;
    }

    /** Поиск в инвентаре, если нужен — расширьте через PlayerInventoryUtil.slots(). */
    private int findItemAnywhere(net.minecraft.item.Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /**
     * Свап на слот (0..8 hotbar). Учитывает silent/legit.
     * При silent — используется PlayerInventoryUtil.swapHand (как в AutoWeb).
     */
    private boolean swapTo(int slot) {
        if (slot == -1) { abort("предмет пропал"); return false; }
        if (mc.player.getInventory().getSelectedSlot() == slot) return true;

        if (!swapped) {
            prevSlot = mc.player.getInventory().getSelectedSlot();
            swapped = true;
        }

        if (silentSwap) {
            // Silent: подмена пакетами, визуально слот не меняется.
            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true);
        } else {
            mc.player.getInventory().setSelectedSlot(slot);
        }
        return true;
    }

    private void restoreSlot() {
        if (prevSlot < 0) return;
        if (silentSwap) {
            PlayerInventoryUtil.swapHand(prevSlot, Hand.MAIN_HAND, true);
        } else {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
        prevSlot = -1;
    }
}
