package ru.zov.mixins;

import java.util.ConcurrentModificationException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.events.render.DrawEvent;
import ru.zov.implement.features.modules.render.CrossHair;
import ru.zov.implement.features.modules.render.Hud;
import ru.zov.implement.features.modules.render.PouchHud;

@Mixin({InGameHud.class})
public abstract class InGameHudMixin implements QuickImports {
   @Unique
   private final Hud hud = Hud.getInstance();
   @Final
   @Shadow
   private MinecraftClient client;

   @Shadow
   protected abstract void renderStatusBars(DrawContext var1);

   @Shadow
   protected abstract void renderMountHealth(DrawContext var1);

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      blur.setup();
      EventManager.callEvent(new DrawEvent(context));
      Render2DUtil.onRender(context);
      if (!this.client.options.hudHidden && !(this.client.currentScreen instanceof ChatScreen)) {
         main.getDraggableRepository().draggable().forEach((draggable) -> {
            if (draggable.canDraw(this.hud, draggable)) {
               draggable.startAnimation();
            } else {
               draggable.stopAnimation();
            }

            if (!draggable.isBackwardsFinished()) {
               draggable.validPosition();

               try {
                  MathUtil.multAlpha(draggable.getScaleAnimation().getOutput().floatValue(), () -> draggable.drawDraggable(context));
               } catch (ConcurrentModificationException var4) {
               }
            }

         });
      }

   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (this.client.currentScreen != null) {
         ci.cancel();
         return;
      }

      CrossHair crossHair = CrossHair.getInstance();
      if (crossHair.isState()) {
         crossHair.onRenderCrossHair();
         ci.cancel();
      }

   }

   @Inject(
      at = {@At("HEAD")},
      method = {"renderStatusEffectOverlay"},
      cancellable = true
   )
   public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (this.hud.isState() && this.hud.interfaceSettings.isSelected("Potions")) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (this.hud.isState() && this.hud.interfaceSettings.isSelected("Score Board")) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderOverlayMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (this.hud.isState() && this.hud.interfaceSettings.isSelected("HotBar")) {
         ci.cancel();
      }

   }

   @Redirect(
      method = "renderMainHud",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"
      )
   )
   private void renderExperienceLevelHook(
      DrawContext context,
      net.minecraft.client.font.TextRenderer textRenderer,
      int level
   ) {
      if (!this.hud.isState() || !this.hud.interfaceSettings.isSelected("HotBar")) {
         net.minecraft.client.gui.hud.bar.Bar.drawExperienceLevel(context, textRenderer, level);
      }
   }



   @Inject(
      method = {"renderMainHud"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (this.hud.isState() && this.hud.interfaceSettings.isSelected("HotBar")) {
         if (this.client.interactionManager.hasStatusBars()) {
            this.renderStatusBars(context);
         }

         this.renderMountHealth(context);
         ci.cancel();
      }

   }
}
