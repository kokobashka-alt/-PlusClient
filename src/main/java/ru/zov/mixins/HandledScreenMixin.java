package ru.zov.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.implement.events.container.HandledScreenEvent;

@Mixin({HandledScreen.class})
public class HandledScreenMixin {
   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof HandledScreen<?> handler) {
         EventManager.callEvent(new HandledScreenEvent(handler, context));
      }

   }
}
