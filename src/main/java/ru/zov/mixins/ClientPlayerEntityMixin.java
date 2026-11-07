package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.event.MovementUpdateEvent;
import ru.zov.implement.features.modules.combat.killaura.event.MovementUpdateSource;
import ru.zov.implement.features.modules.combat.killaura.math.DirectionalInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.block.PushEvent;
import ru.zov.implement.events.container.CloseScreenEvent;
import ru.zov.implement.events.item.UsingItemEvent;
import ru.zov.implement.events.player.MotionEvent;
import ru.zov.implement.events.player.MoveEvent;
import ru.zov.implement.events.player.PostMotionEvent;
import ru.zov.implement.events.player.PostTickEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.features.modules.movement.AutoSprint;
import ru.zov.implement.features.modules.movement.ElytraRecast;
import ru.zov.implement.features.modules.movement.NoSlow;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Final
   @Shadow
   protected MinecraftClient client;

   @Shadow
   public abstract float getPitch(float var1);

   @Shadow
   public abstract float getYaw(float var1);

   @Shadow
   protected abstract void autoJump(float var1, float var2);

   @Shadow
   public Input input;

   @Shadow
   public abstract boolean canSprint(boolean allowTouchingWater);

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"<init>"}
   )
   private void onInit(MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler, StatHandler stats, ClientRecipeBook recipeBook, PlayerInput lastPlayerInput, boolean lastSneaking, CallbackInfo ci) {
      EventManager.callEvent(new PlayerLoadEvent());
   }

   @ModifyExpressionValue(
      method = {"tickMovement"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/PlayerInput;jump()Z",
   ordinal = 0
)}
   )
   public boolean recastHook(boolean original) {
      return original && !ElytraRecast.getInstance().canStart();
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo info) {
      if (this.client.player != null && this.client.world != null) {
         EventManager.callEvent(new TickEvent());
      }

   }

   @Inject(
      method = {"tick"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
   shift = Shift.AFTER
)}
   )
   public void postTick(CallbackInfo callbackInfo) {
      if (this.client.player != null && this.client.world != null) {
         EventManager.callEvent(new PostTickEvent());
      }

   }

   @ModifyExpressionValue(
      method = {"sendMovementPackets", "tick"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
)}
   )
   private float hookSilentRotationYaw(float original) {
      return RotationController.INSTANCE.getRotation().getYaw();
   }

   @ModifyExpressionValue(
      method = {"sendMovementPackets", "tick"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
)}
   )
   private float hookSilentRotationPitch(float original) {
      return RotationController.INSTANCE.getRotation().getPitch();
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preMotion(CallbackInfo ci) {
      MotionEvent event = new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYaw(1.0F), this.getPitch(1.0F), this.isOnGround());
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"closeHandledScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void closeHandledScreenHook(CallbackInfo ci) {
      CloseScreenEvent event = new CloseScreenEvent(this.client.currentScreen);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @ModifyExpressionValue(
      method = {"applyMovementSpeedFactors"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
)}
   )
   private boolean usingItemHook(boolean original) {
      if (original) {
         UsingItemEvent event = new UsingItemEvent((byte)1);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            return false;
         }
      }

      return original;
   }

   @ModifyExpressionValue(
      method = {"canSprint"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprintOrFly()Z"
)}
   )
   private boolean canSprintHungerHook(boolean original) {
      AutoSprint autoSprint = AutoSprint.getInstance();
      return autoSprint.isState() && autoSprint.ignoreHunger.isValue() || original;
   }

   @Inject(
      method = {"move"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
)},
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      MoveEvent event = new MoveEvent(movement);
      EventManager.callEvent(event);
      double d = this.getX();
      double e = this.getZ();
      super.move(movementType, event.getMovement());
      this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
      ci.cancel();
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void postMotion(CallbackInfo ci) {
      PostMotionEvent postMotionEvent = new PostMotionEvent();
      EventManager.callEvent(postMotionEvent);
      if (postMotionEvent.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
      PushEvent event = new PushEvent(PushEvent.Type.BLOCK);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"isBlockedFromSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isBlockedFromSprintingHook(CallbackInfoReturnable<Boolean> cir) {
      NoSlow noSlow = NoSlow.getInstance();
      if (noSlow.isState() && noSlow.slowType.isSelected("Item")) {
         cir.setReturnValue(false);
      }
   }

   @ModifyExpressionValue(
      method = {"tickMovement"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/PlayerInput;sprint()Z"
)}
   )
   public boolean sprintIsPressedHook(boolean original) {
      boolean sprint = original || AutoSprint.getInstance().isState();
      MovementUpdateEvent event = new MovementUpdateEvent(
         DirectionalInput.fromInput(this.input.playerInput),
         sprint,
         MovementUpdateSource.MOVEMENT_TICK
      );
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(event);
      return event.isSprint();
   }

   @ModifyExpressionValue(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldStopSprinting()Z"
      )}
   )
   private boolean donorShouldStopSprintingHook(boolean vanillaShouldStop) {
      MovementUpdateEvent event = new MovementUpdateEvent(
         DirectionalInput.fromInput(this.input.playerInput),
         this.canSprint(this.getAbilities().flying),
         MovementUpdateSource.MOVEMENT_TICK
      );
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(event);
      return vanillaShouldStop || !event.isSprint();
   }

   @ModifyExpressionValue(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldStopSwimSprinting()Z"
      )}
   )
   private boolean donorShouldStopSwimSprintingHook(boolean vanillaShouldStop) {
      MovementUpdateEvent event = new MovementUpdateEvent(
         DirectionalInput.fromInput(this.input.playerInput),
         this.canSprint(true),
         MovementUpdateSource.MOVEMENT_TICK
      );
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(event);
      return vanillaShouldStop || !event.isSprint();
   }

   @Inject(
      method = {"canSprint"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void canSprintHook(boolean swimming, CallbackInfoReturnable<Boolean> cir) {
      if (!AutoSprint.getInstance().stopWatch.finished()) {
         cir.setReturnValue(false);
      }

   }

   @Inject(method = "getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;", at = @At("RETURN"), cancellable = true)
   private void onUpdateCrosshairHook(float tickDelta, net.minecraft.entity.Entity entity, CallbackInfoReturnable<net.minecraft.util.hit.HitResult> cir) {
      if (ru.zov.common.util.entity.PlayerIntersectionUtil.nullCheck()) return;
      ru.zov.implement.events.player.CrosshairTargetEvent event = new ru.zov.implement.events.player.CrosshairTargetEvent(cir.getReturnValue());
      ru.zov.api.event.EventManager.callEvent(event);
      cir.setReturnValue(event.getResult());
   }

   @org.spongepowered.asm.mixin.injection.Redirect(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
   private static net.minecraft.util.hit.HitResult hookRaycast(net.minecraft.entity.Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
      if (instance != net.minecraft.client.MinecraftClient.getInstance().player) return instance.raycast(maxDistance, tickDelta, includeFluids);
      return ru.zov.common.util.rotation.RaytracingUtil.raycast(tickDelta, maxDistance, ru.zov.common.util.rotation.RotationController.INSTANCE.getInterpolatedRotation(), includeFluids);
   }

   @org.spongepowered.asm.mixin.injection.Redirect(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
   private static net.minecraft.util.math.Vec3d hookRotationVector(net.minecraft.entity.Entity instance, float tickDelta) {
      return ru.zov.common.util.rotation.RotationController.INSTANCE.getInterpolatedRotation().toVector();
   }
}
