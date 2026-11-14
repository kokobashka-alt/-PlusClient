package ru.zov.implement.features.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.AttackEvent;
import ru.zov.implement.events.player.TickEvent;

/**
 * Отдельный свап-хелпер булавы.
 *
 *  - Breach: пока падаешь (velocity.y < 0) и булава не в руке — свапает её.
 *            После удара держит holdTicks тиков и возвращает слот.
 *  - Swap:   перед ударом свапает булаву, после удара возвращает слот.
 *
 * Подписан на AttackEvent, так что ручной вызов из Aura не нужен.
 */
public class MaceHelper extends Module {

    public final SelectSetting mode = new SelectSetting(
            "Режим", "Как работать с булавой")
            .value("Breach", "Swap")
            .selected("Breach");

    public final ValueSetting holdTicks = new ValueSetting(
            "Удерживать булаву", "Сколько тиков держать булаву")
            .setValue(2f).range(1f, 5f)
            .visible(() -> mode.isSelected("Breach"));

    public final SelectSetting swapMode = new SelectSetting(
            "Свап", "Способ свапа булавы")
            .value("Vanilla", "Funtime")
            .selected("Vanilla")
            .visible(() -> mode.isSelected("Swap"));

    public static MaceHelper getInstance() {
        return (MaceHelper) Instance.get(MaceHelper.class);
    }

    private int previousSlot = -1;
    private int waitUntilTick = 0;
    private boolean breaching;

    public MaceHelper() {
        super("MaceHelper", "Свап булавы перед ударом", ModuleCategory.COMBAT);
        this.setup(new Setting[]{mode, holdTicks, swapMode});
    }

    @Override
    public void activate() {
        previousSlot = -1;
        breaching = false;
        waitUntilTick = 0;
    }

    @Override
    public void deactivate() {
        restoreSlot();
    }

    /**
     * Срабатывает перед ударом (на AttackEvent).
     * Vanilla-свап делаем ДО swing, чтобы удар пришёлся уже булавой.
     */
    @EventHandler
    public void onAttack(AttackEvent e) {
        if (mc.player == null) return;
        Entity target = e.getEntity();
        if (!(target instanceof LivingEntity) || target == mc.player) return;

        if (mode.isSelected("Swap")) {
            swapToMace();
        } else {
            // Breach: свапаем перед ударом, если булава не в руке
            if (!isHoldingMace()) swapToMace();
            breaching = true;
            waitUntilTick = mc.player.age + (int) holdTicks.getValue();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        if (!mode.isSelected("Breach")) return;

        // Падаем и булава не в руке → свап
        if (mc.player.getVelocity().y < 0 && !isHoldingMace()) {
            swapToMace();
        }

        // Отпустили удар — вернуть слот через holdTicks тиков
        if (breaching && mc.player.age >= waitUntilTick) {
            restoreSlot();
            breaching = false;
        }
    }

    private boolean isHoldingMace() {
        return mc.player.getMainHandStack().isOf(Items.MACE);
    }

    private void swapToMace() {
        if (mc.player == null || isHoldingMace()) return;

        int current = mc.player.getInventory().selectedSlot;
        int mace = findMaceSlot();
        if (mace == -1) return;

        if (previousSlot == -1) previousSlot = current;

        if (swapMode.isSelected("Funtime")) {
            var slotOpt = PlayerInventoryUtil.slots()
                    .filter(s -> s.getStack().isOf(Items.MACE))
                    .findFirst();
            if (slotOpt.isPresent()) {
                PlayerInventoryUtil.swapHand(slotOpt.get(), Hand.MAIN_HAND, true);
            }
        } else {
            // Vanilla
            mc.player.getInventory().selectedSlot = mace;
            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mace));
            }
        }
    }

    private void restoreSlot() {
        if (mc.player == null || previousSlot == -1) return;

        if (swapMode.isSelected("Funtime") && !mode.isSelected("Swap")) {
            // Funtime + Breach: вернуть предыдущий слот через silent swap не получится,
            // потому что мы уже переключили selectedSlot. Просто ставим selectedSlot.
        }

        mc.player.getInventory().selectedSlot = previousSlot;
        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        }
        previousSlot = -1;
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) return i;
        }
        return -1;
    }
}
