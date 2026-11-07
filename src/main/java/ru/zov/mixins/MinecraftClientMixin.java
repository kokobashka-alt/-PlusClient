package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.api.file.exception.FileProcessingException;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.logger.LoggerUtil;
import ru.zov.common.util.other.BufferUtil;
import ru.zov.core.Main;
import ru.zov.implement.events.container.OpenScreenEvent;
import ru.zov.implement.events.player.HotBarUpdateEvent;
import ru.zov.implement.features.modules.combat.NoInteract;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin implements QuickImports {
   @Shadow
   public ClientPlayerEntity player;
   @Shadow
   public @Nullable HitResult crosshairTarget;
   @Shadow
   public @Nullable Screen currentScreen;
   @Shadow
   public @Nullable ClientWorld world;

   @Inject(
      at = {@At("TAIL")},
      method = {"<init>"}
   )
   private void onInit(RunArgs args, CallbackInfo ci) {
      Fonts.init();
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"stop"}
   )
   private void stop(CallbackInfo ci) {
      LoggerUtil.info("Stopping for MinecraftClient");
      if (main.isInitialized()) {
         try {
            main.getFileController().saveFiles();
         } catch (FileProcessingException e) {
            String var10000 = e.getMessage();
            LoggerUtil.error("Error occurred while saving files: " + var10000 + " " + String.valueOf(e.getCause()));
         } finally {
            main.getFileController().stopAutoSave();
         }
      }

   }

   @ModifyExpressionValue(
      method = {"doItemUse"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isRiding()Z"
)}
   )
   public boolean doItemUseHook(boolean original) {
      if (NoInteract.getInstance().isState()) {
         for(Hand hand : Hand.values()) {
            ItemStack stack = this.player.getStackInHand(hand);
            if (!stack.isEmpty() && !(stack.getItem() instanceof BucketItem)) {
               if (ItemUtil.hasCooldown(stack)) {
                  original = true;
               }

               ActionResult var8 = stack.use(this.world, this.player, hand);
               if (var8 instanceof ActionResult.Success) {
                  ActionResult.Success success = (ActionResult.Success)var8;
                  PlayerIntersectionUtil.sendSequencedPacket((i) -> new PlayerInteractItemC2SPacket(hand, i, this.player.getYaw(), this.player.getPitch()));
                  if (success.swingSource().equals(SwingSource.CLIENT)) {
                     this.player.swingHand(hand);
                  }

                  return true;
               }
            }
         }
      }

      return original;
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setScreenHook(Screen screen, CallbackInfo ci) {
      OpenScreenEvent event = new OpenScreenEvent(screen);
      EventManager.callEvent(event);
      main.getDraggableRepository().draggable().forEach((drag) -> drag.openScreen(event));
      Screen eventScreen = event.getScreen();
      if (screen != eventScreen) {
         mc.setScreen(eventScreen);
         ci.cancel();
      }

   }

   @Redirect(
      method = {"<init>"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/Window;setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V"
)
   )
   private void onChangeIcon(Window instance, ResourcePack resourcePack, Icons icons) throws IOException {
      if (GLFW.glfwGetPlatform() == 393218) {
         MacWindowUtil.setApplicationIconImage(icons.getMacIcon(resourcePack));
      } else {
         BufferUtil.setWindowIcon(Main.class.getResourceAsStream("/zov.png"), Main.class.getResourceAsStream("/zov.png"));
      }
   }

   @Inject(
      method = {"getWindowTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onWindowTitle(CallbackInfoReturnable<String> cir) {
      cir.setReturnValue("ZOV - " + SharedConstants.getGameVersion().name());
   }

   @Inject(
      method = {"handleInputEvents"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
)},
      cancellable = true
   )
   public void handleInputEventsHook(CallbackInfo ci) {
      HotBarUpdateEvent event = new HotBarUpdateEvent();
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
