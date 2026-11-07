package ru.zov.mixins;

import java.util.List;
import java.util.ConcurrentModificationException;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.common.QuickImports;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.core.Main;
import ru.zov.implement.features.modules.render.Hud;

@Mixin({ChatScreen.class})
public class ChatScreenMixin extends Screen implements QuickImports {
   @Unique
   List<AbstractDraggable> draggables = Main.getInstance().getDraggableRepository().draggable();

   protected ChatScreenMixin() {
      super(Text.empty());
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Hud hud = Hud.getInstance();
      blur.setup();
      this.draggables.forEach(draggable -> {
         if (draggable.canDraw(hud, draggable)) {
            draggable.startAnimation();
         } else {
            draggable.stopAnimation();
         }

         if (!draggable.isBackwardsFinished()) {
            draggable.validPosition();

            try {
               MathUtil.multAlpha(draggable.getScaleAnimation().getOutput().floatValue(), () -> draggable.drawDraggable(context));
            } catch (ConcurrentModificationException ignored) {
            }
         }
      });

      this.draggables.stream().filter((draggable) -> draggable.canDraw(hud, draggable) && (draggable.isDragging() || draggable.isChangeSize())).reduce((first, second) -> second).ifPresent((active) -> this.draggables.forEach((draggable) -> {
         if (active == draggable) {
            draggable.render(context, mouseX, mouseY, delta);
            }

         }));
      Render2DUtil.onRender(context);
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("TAIL")}
   )
   private void onMouseClicked(net.minecraft.client.gui.Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      this.draggables.forEach((draggable) -> draggable.mouseClicked(click.x(), click.y(), click.button()));
   }

   public boolean mouseReleased(net.minecraft.client.gui.Click click) {
      this.draggables.forEach((draggable) -> draggable.mouseReleased(click.x(), click.y(), click.button()));
      return super.mouseReleased(click);
   }

   @Inject(
      method = "keyPressed",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/ChatScreen;sendMessage(Ljava/lang/String;Z)V"
      ),
      cancellable = true
   )
   private void restoreIrcMessageDelay(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
      if (!Main.getInstance().getIrcRepository().message.finished(4000.0)) {
         cir.setReturnValue(false);
      }
   }

}
