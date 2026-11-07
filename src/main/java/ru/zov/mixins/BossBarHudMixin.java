package ru.zov.mixins;

import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.implement.features.modules.render.Hud;

@Mixin({BossBarHud.class})
public class BossBarHudMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderHook(CallbackInfo ci) {
      if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Boss Bars")) {
         ci.cancel();
      }

   }
}
