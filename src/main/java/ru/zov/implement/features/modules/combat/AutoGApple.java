package ru.zov.implement.features.modules.combat;

import lombok.Generated;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;

public class AutoGApple extends Module {

    private final ValueSetting healthThreshold = new ValueSetting(
    "Здоровье", "Порог здоровья для еды")
    .setValue(16.0f)
    .range(3.0f, 20.0f);
    private final BooleanSetting smartMode = new BooleanSetting(
        "Умный", "Есть из хотбара, иначе только с левой руки").setValue(true);
    private final BooleanSetting goldenHearts = new BooleanSetting(
        "Золотые сердца", "Учитывать absorption").setValue(true);
    private final BooleanSetting returnSlot = new BooleanSetting(
        "Возвращать слот", "Возвращать предыдущий слот после еды").setValue(true);

    private boolean isEating = false;
    private int previousSlot = -1;

    public AutoGApple() {
        super("AutoGApple", "Автоматически ест золотые яблоки", ModuleCategory.COMBAT);
        this.setup(new Setting[]{ healthThreshold, smartMode, goldenHearts, returnSlot });
    }

    public static AutoGApple getInstance() {
        return (AutoGApple) Instance.get(AutoGApple.class);
    }

    @Override
    public void onEnable() {
        isEating = false;
        previousSlot = -1;
    }

    @Override
    public void onDisable() {
        stopEating();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        handleEating();
    }

    private void handleEating() {
        if (canEat()) {
            if (smartMode.isValue()) {
                if (!hasGappleInHand()) swapToGappleSlot();
                startEating();
            } else if (hasGappleInOffhand()) {
                startEating();
            }
        } else if (isEating && !shouldContinueEating()) {
            stopEating();
        }
    }

    private boolean hasGappleInHand() {
        return mc.player.getMainHandStack().isOf(Items.GOLDEN_APPLE);
    }

    private boolean hasGappleInOffhand() {
        return mc.player.getOffHandStack().isOf(Items.GOLDEN_APPLE);
    }

    private boolean canEat() {
        if (mc.player.isDead()) return false;
        if (mc.player.getItemCooldownManager()
                .isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) return false;

        if (smartMode.isValue()) {
            if (!hasGapple()) return false;
        } else if (!hasGappleInOffhand()) {
            return false;
        }

        return getEffectiveHealth() <= healthThreshold.getValue();
    }

    private boolean shouldContinueEating() {
        if (mc.player.isDead()) return false;
        if (!mc.player.isUsingItem()) return false;
        return mc.player.getActiveItem().isOf(Items.GOLDEN_APPLE);
    }

    private float getEffectiveHealth() {
        float health = mc.player.getHealth();
        if (goldenHearts.isValue()) health += mc.player.getAbsorptionAmount();
        return health;
    }

    private boolean hasGapple() {
        return hasGappleInOffhand() || hasGappleInHotbar();
    }

    private boolean hasGappleInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.GOLDEN_APPLE)) return true;
        }
        return false;
    }

    private int findGappleInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.GOLDEN_APPLE)) return i;
        }
        return -1;
    }

    private void swapToGappleSlot() {
        int gappleSlot = findGappleInHotbar();
        if (gappleSlot == -1) return;
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (currentSlot == gappleSlot) return;

        if (previousSlot == -1 && returnSlot.isValue()) {
            previousSlot = currentSlot;
        }
        selectSlot(gappleSlot);
    }

    private void startEating() {
        if (!isEating && !mc.options.useKey.isPressed()) {
            mc.options.useKey.setPressed(true);
            isEating = true;
        }
    }

    private void stopEating() {
        if (!isEating) return;
        mc.options.useKey.setPressed(false);
        isEating = false;
        if (previousSlot != -1 && returnSlot.isValue()) {
            selectSlot(previousSlot);
            previousSlot = -1;
        }
    }

    /** Совместимость: в 1.21.x метод мог переименоваться. */
    private void selectSlot(int slot) {
        try {
            mc.player.getInventory().setSelectedSlot(slot);
        } catch (NoSuchMethodError ex) {
            // если метода нет — правый клик по слоту через PlayerInventoryUtil
            // или поле selectedSlot через аксессор:
            // mc.player.getInventory().selectedSlot = slot;
        }
    }

    @Generated public ValueSetting  getHealthThreshold() { return healthThreshold; }
    @Generated public BooleanSetting getSmartMode()      { return smartMode; }
    @Generated public BooleanSetting getGoldenHearts()   { return goldenHearts; }
    @Generated public BooleanSetting getReturnSlot()     { return returnSlot; }
    @Generated public boolean        isEating()          { return isEating; }
    @Generated public int            getPreviousSlot()   { return previousSlot; }
}
