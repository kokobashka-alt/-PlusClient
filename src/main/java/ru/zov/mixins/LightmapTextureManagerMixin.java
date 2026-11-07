package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.zov.implement.features.modules.render.WorldTweaks;

@Mixin({LightmapTextureManager.class})
public class LightmapTextureManagerMixin {
   @ModifyExpressionValue(
      method = {"update(F)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"
)}
   )
   private Object injectXRayFullBright(Object original) {
      WorldTweaks tweaks = WorldTweaks.getInstance();
      return tweaks.isState() && tweaks.modeSetting.isSelected("Bright") ? Math.max((Double)original, (double)(tweaks.brightSetting.getValue() * 10.0F)) : original;
   }
}
