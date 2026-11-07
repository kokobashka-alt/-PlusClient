package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.item.SwingDurationEvent;
import ru.zov.implement.events.player.JumpEvent;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.features.modules.movement.ElytraRecast;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {
   @Shadow
   public float bodyYaw;
   @Unique
   private final MinecraftClient client = MinecraftClient.getInstance();

   @Shadow
   public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> var1);

   @Shadow
   public abstract @Nullable StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> var1);

   @Shadow
   protected abstract float getMaxRelativeHeadRotation();

   @ModifyExpressionValue(
      method = {"canGlideWith"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/item/ItemStack;willBreakNextUse()Z"
)}
   )
   private static boolean canGlideWithHook(boolean original) {
      ElytraRecast recast = ElytraRecast.getInstance();
      return (!recast.isState() || !recast.ignoreDurability.isValue()) && original;
   }

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void jump(CallbackInfo info) {
      if ((Object)this instanceof ClientPlayerEntity player) {
         JumpEvent event = new JumpEvent(player);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }

   }

   @ModifyExpressionValue(
      method = {"jump"},
      at = {@At(
   value = "NEW",
   target = "(DDD)Lnet/minecraft/util/math/Vec3d;"
)}
   )
   private Vec3d hookFixRotation(Vec3d original) {
      if ((Object)this != this.client.player) {
         return original;
      } else {
         float yaw = RotationController.INSTANCE.getMoveRotation().getYaw() * ((float)Math.PI / 180F);
         return new Vec3d((double)(-MathHelper.sin(yaw) * 0.2F), (double)0.0F, (double)(MathHelper.cos(yaw) * 0.2F));
      }
   }

   @ModifyExpressionValue(
      method = {"calcGlidingVelocity"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"
)}
   )
   private float hookModifyFallFlyingPitch(float original) {
      return (Object)this != this.client.player ? original : RotationController.INSTANCE.getMoveRotation().getPitch();
   }

   @ModifyExpressionValue(
      method = {"calcGlidingVelocity"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
)}
   )
   private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
      return (Object)this != this.client.player ? original : RotationController.INSTANCE.getMoveRotation().toVector();
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this == this.client.player) {
         SwingDurationEvent event = new SwingDurationEvent();
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(this.client.player)) {
               animation *= (float)(6 - (1 + StatusEffectUtil.getHasteAmplifier(this.client.player)));
            } else {
               animation *= (float)(this.hasStatusEffect(StatusEffects.MINING_FATIGUE) ? 6 + (1 + this.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6);
            }

            cir.setReturnValue((int)animation);
         }

      }
   }

   @ModifyExpressionValue(
      method = {"turnHead"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/math/MathHelper;wrapDegrees(F)F",
   ordinal = 1
)}
   )
   private float wrapDegreesHook(float original) {
      if ((Object)this == this.client.player) {
         return MathHelper.wrapDegrees(RotationController.INSTANCE.getMoveRotation().getYaw() - this.bodyYaw);
      }
      return original;
   }
}
