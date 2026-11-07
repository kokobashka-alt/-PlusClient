package ru.zov.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.common.util.item.ItemUsage;
import ru.zov.implement.events.block.BlockBreakingEvent;
import ru.zov.implement.events.block.BreakBlockEvent;
import ru.zov.implement.events.container.ClickSlotEvent;
import ru.zov.implement.events.item.UsingItemEvent;
import ru.zov.implement.events.player.AttackEvent;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
   @Inject(
      method = {"attackEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void attackEntityPre(PlayerEntity player, Entity target, CallbackInfo info) {
      AttackEvent event = new AttackEvent(target);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"interactItem"},
      at = {@At("RETURN")}
   )
   public void interactItemHook(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      Object var5 = cir.getReturnValue();
      if (var5 instanceof ActionResult.Success success) {
         if (!success.swingSource().equals(SwingSource.CLIENT)) {
            UsingItemEvent event = new UsingItemEvent((byte)0);
            EventManager.callEvent(event);
         }
      }

   }

   @Inject(
      method = {"stopUsingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void stopUsingItemHook(CallbackInfo ci) {
      UsingItemEvent event = new UsingItemEvent((byte)2);
      EventManager.callEvent(event);
      if (event.isCancelled() || ItemUsage.INSTANCE.isUseItem()) {
         ItemUsage.INSTANCE.setUseItem(false);
         ci.cancel();
      }

   }

   @Inject(
      method = {"clickSlot"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
      ClickSlotEvent event = new ClickSlotEvent(syncId, slotId, button, actionType);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"updateBlockBreakingProgress"},
      at = {@At("HEAD")}
   )
   private void injectBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      EventManager.callEvent(new BlockBreakingEvent(pos, direction));
   }

   @Inject(
      method = {"breakBlock"},
      at = {@At("RETURN")}
   )
   private void injectBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      EventManager.callEvent(new BreakBlockEvent(pos));
   }
}
