package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.item.HandAnimationEvent;
import ru.zov.implement.events.item.HandOffsetEvent;
import ru.zov.implement.events.render.ItemRendererEvent;

@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
   @Unique
   private boolean arizonaCustomAnimation = false;

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
         shift = Shift.AFTER
      )}
   )
   private void renderFirstPersonItemHook(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, CallbackInfo ci) {
      HandOffsetEvent event = new HandOffsetEvent(matrices, stack, hand);
      EventManager.callEvent(event);
   }

   @WrapOperation(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
      )}
   )
   private void itemRenderHook(HeldItemRenderer instance, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, Operation<Void> original) {
      ItemRendererEvent event = new ItemRendererEvent(player, item, hand);
      EventManager.callEvent(event);
      original.call(instance, event.getPlayer(), tickDelta, pitch, event.getHand(), swingProgress, event.getStack(), equipProgress, matrices, commandQueue, light);
   }

   @WrapOperation(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"))
   private void wrapApplyEquipOffset(HeldItemRenderer instance, MatrixStack matrices, Arm arm, float equipProgress, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) AbstractClientPlayerEntity player, @Local(ordinal = 0, argsOnly = true) Hand hand, @Local(ordinal = 2, argsOnly = true) float swingProgress) {
      boolean isUsingItem = player.isUsingItem() && player.getActiveHand() == hand;

      if (isUsingItem) {
         arizonaCustomAnimation = false;
         original.call(instance, matrices, arm, equipProgress);
         return;
      }

      HandAnimationEvent event = new HandAnimationEvent(matrices, hand, swingProgress);
      EventManager.callEvent(event);

      if (event.isCancelled()) {
         arizonaCustomAnimation = true;
         return;
      }

      arizonaCustomAnimation = false;
      original.call(instance, matrices, arm, equipProgress);
   }

   @WrapOperation(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"))
   private void wrapSwingArm(HeldItemRenderer instance, float swingProgress, MatrixStack matrices, int armX, Arm arm, Operation<Void> original) {
      if (arizonaCustomAnimation) {
         return;
      }
      original.call(instance, swingProgress, matrices, armX, arm);
   }
}
