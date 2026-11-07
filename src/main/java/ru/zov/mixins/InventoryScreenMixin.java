package ru.zov.mixins;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;

@Mixin({InventoryScreen.class})
public abstract class InventoryScreenMixin extends ScreenMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   protected void init(CallbackInfo ci) {
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Выкинуть всё"), (button) -> PlayerInventoryComponent.addTask(() -> {
            button.setFocused(false);
            PlayerInventoryUtil.getSlots().stream().filter((s) -> !s.getStack().isEmpty()).forEach((s) -> PlayerInventoryUtil.clickSlot(s, 1, SlotActionType.THROW, false));
            PlayerInventoryUtil.updateSlots();
         })).width(80).position(mc.getWindow().getScaledWidth() / 2 - 40, mc.getWindow().getScaledHeight() / 2 - 106).build());
   }
}
