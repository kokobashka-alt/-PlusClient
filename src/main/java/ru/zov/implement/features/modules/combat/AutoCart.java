package ru.zov.implement.features.modules.combat;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
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
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;

public class AutoCart extends Module {

    // ---------- Константы ----------
    private static final double BLOCK_REACH = 4.2;
    private static final double ENTITY_REACH = 2.9;
    private static final int SEARCH_RADIUS = 5;

    private static final int STAGE_FIND          = 0;
    private static final int STAGE_SWAP_CART     = 1;
    private static final int STAGE_PLACE_CART    = 2;
    private static final int STAGE_SWAP_IGNITER  = 3;
    private static final int STAGE_FLINT_IGNITE  = 4;
    private static final int STAGE_BOW_DRAW      = 5;
    private static final int STAGE_RESTORE       = 6;

    // ---------- Настройки ----------
    public final BooleanSetting ignite = new BooleanSetting(
        "Поджигать", "Взрывать вагонетку после установки").setValue(true);

public final SelectSetting igniteMode = new SelectSetting(
    "Поджиг", "Способ детонации")
    .value("Лук", "Огниво")
    .selected("Лук")
    .visible(() -> ignite.isValue());

    public final ValueSetting drawTicks = new ValueSetting(
        "Натяжка", "Сколько тиков натягивать лук")
        .setValue(20.0f)
        .range(5.0f, 25.0f)
        .visible(() -> ignite.isValue() && igniteMode.isSelected("Лук"));

    public final SelectSetting swapMode = new SelectSetting(
    "Свап", "Способ переключения")
    .value("Silent", "Legit")
    .selected("Silent");

    public final BooleanSetting debugMessages = new BooleanSetting(
        "Сообщения", "Писать в чат причины неудач").setValue(true);

    // ---------- Состояние ----------
    private int stage;
    private int waitTicks;
    private int drawCounter;
    private boolean swapped;
    private boolean silentSwap;
    private BlockPos railPos;
    private Slot prevSlot;

    public AutoCart() {
        super("AutoCart", "Ставит и взрывает ТНТ-вагонетку на рельсе", ModuleCategory.COMBAT);
        setup(new Setting[]{ ignite, igniteMode, drawTicks, swapMode, debugMessages });
    }

    // ---------- Жизненный цикл ----------
    @Override
    public void activate() {
        stage = STAGE_FIND;
        waitTicks = 0;
        drawCounter = 0;
        swapped = false;
        silentSwap = swapMode.isSelected("Silent");
        railPos = null;
        prevSlot = null;
    }

    @Override
    public void deactivate() {
        if (stage == STAGE_BOW_DRAW) {
            mc.options.useKey.setPressed(false);
        }
        if (swapped) {
            restoreSlot();
            swapped = false;
        }
        railPos = null;
        prevSlot = null;
    }

    private boolean isBowMode() {
        return igniteMode.isSelected("Лук");
    }

    // ---------- Тик ----------
    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            setState(false);
            return;
        }

        Entity cart;
        switch (stage) {
            case STAGE_FIND -> {
                railPos = findNearestRail();
                if (railPos == null) {
                    abort("рельсы в радиусе " + BLOCK_REACH + " нет");
                    return;
                }

                Slot cartSlot = PlayerInventoryUtil.getSlot(Items.TNT_MINECART);
                boolean hasCart = cartSlot != null || findCart() != null;
                if (!hasCart) { abort("нет ТНТ-вагонетки"); return; }

                if (ignite.isValue()) {
                    if (isBowMode()) {
                        if (PlayerInventoryUtil.getSlot(Items.BOW) == null) { abort("нет лука"); return; }
                        if (!mc.player.getAbilities().creativeMode && !hasArrows()) { abort("нет стрел"); return; }
                    } else if (PlayerInventoryUtil.getSlot(Items.FLINT_AND_STEEL) == null) {
                        abort("нет огнива"); return;
                    }
                }

                silentSwap = swapMode.isSelected("Silent")
                    && (!ignite.isValue() || !isBowMode());

                waitTicks = 0;
                stage = findCart() != null
                    ? (ignite.isValue() ? STAGE_SWAP_IGNITER : STAGE_RESTORE)
                    : STAGE_SWAP_CART;
            }
            case STAGE_SWAP_CART -> {
                if (swapTo(Items.TNT_MINECART)) stage = STAGE_PLACE_CART;
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
                    if (swapTo(Items.BOW)) {
                        drawCounter = 0;
                        stage = STAGE_BOW_DRAW;
                    }
                } else {
                    if (mc.player.getEyePos().distanceTo(cart.getEntityPos()) > ENTITY_REACH) {
                        abort("вагонетка дальше " + ENTITY_REACH);
                        return;
                    }
                    if (swapTo(Items.FLINT_AND_STEEL)) stage = STAGE_FLINT_IGNITE;
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
                    abort("вагонетка пропала до выстрела");
                    return;
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
                setState(false);
            }
        }
    }

    // ---------- Наводка ----------
    private void aimAt(Entity cart) {
        Vec3d point = cart.getEntityPos().add(0, cart.getHeight() * 0.5, 0);
        Angle angle = AngleUtil.fromVec3d(point.subtract(mc.player.getEyePos()));
        // если в вашем AngleUtil нет fromVec3d — используйте AngleUtil.calculateAngle(point)
        // но в TargetPearl вы уже используете calculateAngle — проверьте.
        // На всякий случай — прямая замена:
        // Angle angle = AngleUtil.calculateAngle(point);
        PlayerInventoryUtil.swapAndUse(
            PlayerInventoryUtil.getSlot(Items.BOW), angle, false);
        // НЕ используем RotationManager.rotateTo — он требует Module (у нас AutoCart),
        // но для лука достаточно swapAndUse(Slot, Angle, task).
    }

    // ---------- Хелперы ----------
    private boolean hasArrows() {
        return PlayerInventoryUtil.getSlot(Items.ARROW) != null
            || PlayerInventoryUtil.getSlot(Items.TIPPED_ARROW) != null
            || PlayerInventoryUtil.getSlot(Items.SPECTRAL_ARROW) != null;
    }

    private void abort(String reason) {
        if (debugMessages.isValue()) {
            Notifications.getInstance().addList("AutoCart: " + reason, 2000L);
        }
        if (swapped) { restoreSlot(); swapped = false; }
        setState(false);
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
            if (entity instanceof TntMinecartEntity
                && entity.isAlive()
                && entity.getEntityPos().distanceTo(center) < 2.0) {
                return entity;
            }
        }
        return null;
    }

    /** Свап на предмет в хотбаре. Возвращает true, если предмет уже в руке. */
    private boolean swapTo(net.minecraft.item.Item item) {
        Slot slot = PlayerInventoryUtil.getSlot(item);
        if (slot == null) {
            abort("предмет " + item.getName().getString() + " пропал");
            return false;
        }
        // если это уже выбранный слот — ничего не делаем
        if (slot.id == PlayerInventoryUtil.mainHandSlot().id) return true;

        if (!swapped) {
            prevSlot = PlayerInventoryUtil.mainHandSlot();
            swapped = true;
        }
        // silentSwap фактически дублируется в swapHand(task=true/false)
        // PlayerInventoryUtil сам решает, как свапать.
        PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, silentSwap);
        return true;
    }

    private void restoreSlot() {
        if (prevSlot == null) return;
        PlayerInventoryUtil.swapHand(prevSlot, Hand.MAIN_HAND, silentSwap);
        prevSlot = null;
    }
}
