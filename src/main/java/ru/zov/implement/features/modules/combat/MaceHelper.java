package ru.zov.implement.features.modules.combat;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;

/**
 * Отдельный свап-хелпер булавы, независимый от Aura.
 *
 *  - Breach: держит булаву, пока падаешь и пока не ударил.
 *  - Swap: перед атакой берёт булаву, после — возвращает слот.
 *
 * Оригинал (MaceHelper из 1.21.4) использовал PlayerActionUtil.windowClick
 * (SWAP) / PlayerInventoryUtil.swapHand — здесь то же самое, но под ru.zov.
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

    // Состояние
    private int heldSlot = -1;       // слот, который держим в hotbar
    private int previousSlot = -1;   // слот, который был до свапа (для return)
    private int waitUntilTick = 0;   // после какого тика можно вернуть слот
    private boolean breaching;       // Breach активен

    public MaceHelper() {
        super("MaceHelper", "Свап булавы перед ударом", ModuleCategory.COMBAT);
        this.setup(new Setting[]{mode, holdTicks, swapMode});
    }

    @Override
    public void activate() {
        heldSlot = -1;
        previousSlot = -1;
        breaching = false;
        waitUntilTick = 0;
    }

    @Override
    public void deactivate() {
        restoreSlot();
    }

    /** Вызывается из Aura/TriggerBot перед ударом. */
    public void onAttack() {
        if (mc.player == null) return;
        if (mode.isSelected("Swap")) {
            swapToMace();
        } else {
            // Breach: если булава не в руке — свапнуть; иначе просто оставить
            if (!isHoldingMace()) {
                swapToMace();
            }
            breaching = true;
            waitUntilTick = mc.player.age + (int) holdTicks.getValue();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        if (mode.isSelected("Breach")) {
            // Если падаем вниз (velocity.y < 0) и булава не в руке — свапаем
            if (mc.player.getVelocity().y < 0 && !isHoldingMace()) {
                swapToMace();
            }
            // Если ударил — ждём holdTicks тиков и возвращаем
            if (breaching && mc.player.age >= waitUntilTick) {
                restoreSlot();
                breaching = false;
            }
        }
    }

    private boolean isHoldingMace() {
        return mc.player.getMainHandStack().isOf(Items.MACE);
    }

    private void swapToMace() {
        if (mc.player == null) return;
        if (isHoldingMace()) return;

        int current = mc.player.getInventory().selectedSlot;
        int mace = findMaceSlot();
        if (mace == -1) return;

        if (previousSlot == -1) previousSlot = current;

        if (swapMode.isSelected("Funtime")) {
            // Silent swap через PlayerInventoryUtil.swapHand — не переключает hotbar визуально
            ru.zov.common.util.item.PlayerInventoryUtil.swapHand(
                    ru.zov.common.util.item.PlayerInventoryUtil.slots()
                            .filter(s -> s.getStack().isOf(Items.MACE))
                            .findFirst()
                            .orElse(null),
                    net.minecraft.util.Hand.MAIN_HAND,
                    true
            );
        } else {
            // Vanilla — обычная смена слота
            mc.player.getInventory().selectedSlot = mace;
            mc.player.networkHandler.sendPacket(
                    new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(mace)
            );
        }
        heldSlot = mace;
    }

    private void restoreSlot() {
        if (mc.player == null || previousSlot == -1) return;
        mc.player.getInventory().selectedSlot = previousSlot;
        mc.player.networkHandler.sendPacket(
                new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(previousSlot)
        );
        previousSlot = -1;
        heldSlot = -1;
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) return i;
        }
        return -1;
    }
}
