package ru.zov.implement.features.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.implement.events.player.AttackEvent;

/**
 * PacketMace: на ударе подменяет выбранный слот на булаву из хотбара, бьёт и возвращает слот.
 *
 * Перенесено из исходника без изменения логики: слот меняется только локально (без UpdateSelectedSlotC2SPacket),
 * поэтому на сервере удар может идти не булавой. Проверить на целевом сервере. Для варианта с пакетом есть MaceHelper.
 */
public class PacketMace extends Module {
    public final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", "Работать только когда игрок стоит на земле")
            .setValue(true);
    public final ValueSetting switchDelay = new ValueSetting("Задержка переключения", "Пауза между переключением и ударом, мс")
            .setValue(50.0f).range(0.0f, 200.0f);

    public PacketMace() {
        super("PacketMace", "Автоматически бьёт булавой и возвращает предмет", ModuleCategory.COMBAT);
        this.setup(new Setting[]{onlyOnGround, switchDelay});
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (this.onlyOnGround.isValue() && !mc.player.isOnGround()) return;
        if (mc.player.getMainHandStack().isOf(Items.MACE)) return;

        int maceSlot = findMaceSlot();
        Entity target = e.getEntity();
        if (maceSlot == -1 || target == null) return;

        int previous = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = maceSlot;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = previous;
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) return i;
        }
        return -1;
    }
}
