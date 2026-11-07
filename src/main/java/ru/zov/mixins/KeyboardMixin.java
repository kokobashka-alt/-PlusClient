package ru.zov.mixins;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.screens.menu.MenuScreen;

@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Final
   @Shadow
   private MinecraftClient client;

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")}
   )
   private void onKey(long window, int action, net.minecraft.client.input.KeyInput keyInput, CallbackInfo ci) {
      int key = keyInput.key();
      int modifiers = keyInput.modifiers();
      if (key != -1 && window == this.client.getWindow().getHandle()) {
         if (action == 0 && key == 344 && this.client.currentScreen == null) {
            MenuScreen.INSTANCE.openGui();
         }

         EventManager.callEvent(new KeyEvent(this.client.currentScreen, Type.KEYSYM, key, action));
      }

   }
}
