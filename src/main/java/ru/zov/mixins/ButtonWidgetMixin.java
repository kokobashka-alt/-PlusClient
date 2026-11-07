package ru.zov.mixins;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ButtonWidget.Builder.class})
public class ButtonWidgetMixin {
   @Shadow
   @Final
   public Text message;
   @Shadow
   private int x;

   @Inject(
      method = {"position"},
      at = {@At("TAIL")}
   )
   private void builderHook(int x, int y, CallbackInfoReturnable<ButtonWidget.Builder> cir) {
      if (this.message.getString().equals("ViaFabricPlus")) {
         this.x = 5;
      }

   }
}
