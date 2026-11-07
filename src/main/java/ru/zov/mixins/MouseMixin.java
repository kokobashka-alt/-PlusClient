package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.keyboard.HotBarScrollEvent;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.keyboard.MouseRotationEvent;
import ru.zov.implement.events.render.FovEvent;

@Mixin({Mouse.class})
public class MouseMixin {
   @Final
   @Shadow
   private MinecraftClient client;
   @Shadow
   public double cursorDeltaX;
   @Shadow
   public double cursorDeltaY;

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")}
   )
   public void onMouseButtonHook(long window, net.minecraft.client.input.MouseInput mouseInput, int action, CallbackInfo ci) {
      int button = mouseInput.button();
      int mods = mouseInput.modifiers();
      if (button != -1 && window == this.client.getWindow().getHandle()) {
         EventManager.callEvent(new KeyEvent(this.client.currentScreen, Type.MOUSE, button, action));
      }

   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
)},
      cancellable = true
   )
   public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
      HotBarScrollEvent event = new HotBarScrollEvent(horizontal, vertical);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"updateMouse"},
      at = {@At("HEAD")}
   )
   private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
      int optionsFov = (Integer)this.client.options.getFov().getValue();
      FovEvent event = new FovEvent(optionsFov);
      EventManager.callEvent(event);
      double slowdown = (double)event.getFov() / (double)optionsFov;
      this.cursorDeltaX *= slowdown;
      this.cursorDeltaY *= slowdown;
   }

   @WrapWithCondition(
      method = {"updateMouse"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
)},
      require = 1,
      allow = 1
   )
   private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
      MouseRotationEvent event = new MouseRotationEvent((float)cursorDeltaX, (float)cursorDeltaY);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         return false;
      } else {
         instance.changeLookDirection((double)event.getCursorDeltaX(), (double)event.getCursorDeltaY());
         return false;
      }
   }
}
