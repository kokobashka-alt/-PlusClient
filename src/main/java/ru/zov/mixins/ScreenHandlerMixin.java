package ru.zov.mixins;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ScreenHandler.class})
public abstract class ScreenHandlerMixin {
   @Shadow
   protected abstract Slot addSlot(Slot var1);

   @Inject(
      method = {"addPlayerHotbarSlots"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void addPlayerHotbarSlots(Inventory inventory, int left, int y, CallbackInfo ci) {
      if (inventory instanceof PlayerInventory playerInventory) {
         if (playerInventory.player == null) {
            for(int i = 0; i < 9; ++i) {
               this.addSlot(new Slot(playerInventory, i, left + i * 18, y + 108));
            }

            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"addPlayerInventorySlots"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void addPlayerInventorySlots(Inventory inventory, int left, int top, CallbackInfo ci) {
      if (inventory instanceof PlayerInventory playerInventory) {
         if (playerInventory.player == null) {
            for(int i = 0; i < 9; ++i) {
               for(int j = 0; j < 9; ++j) {
                  this.addSlot(new Slot(playerInventory, j + (i + 1) * 9, left + j * 18, top + i * 18));
               }
            }

            ci.cancel();
         }
      }

   }
}
