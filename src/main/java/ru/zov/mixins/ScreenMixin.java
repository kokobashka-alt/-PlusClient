package ru.zov.mixins;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.chat.ChatEvent;
import ru.zov.implement.events.render.DrawableChildEvent;
import ru.zov.implement.screens.menu.MenuScreen;

@Mixin({Screen.class})
public abstract class ScreenMixin implements QuickImports {
   @Shadow
   protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T var1);

   @Inject(method = {"handleClickEvent"}, at = {@At("HEAD")}, cancellable = true)
   private static void handleCustomClickEvent(ClickEvent clickEvent, MinecraftClient client, Screen screenAfterRun, CallbackInfo ci) {
      if (clickEvent instanceof ClickEvent.RunCommand || clickEvent instanceof ClickEvent.SuggestCommand) {
         String value = clickEvent instanceof ClickEvent.RunCommand runCommand ? runCommand.command() : ((ClickEvent.SuggestCommand)clickEvent).command();
         ChatEvent event = new ChatEvent(value);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void disableBackgroundBlurAndDimmingForMenu(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof MenuScreen) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"init()V"},
      at = {@At("TAIL")}
   )
   protected void init(CallbackInfo ci) {
      if ((Object)this instanceof Screen screen) {
         DrawableChildEvent event = new DrawableChildEvent(new ArrayList(), screen);
         EventManager.callEvent(event);
         event.list().forEach(this::addDrawableChild);
      }

   }
}
