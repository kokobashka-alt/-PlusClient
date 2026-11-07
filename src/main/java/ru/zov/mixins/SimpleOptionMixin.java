package ru.zov.mixins;

import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.QuickImports;

@Mixin({SimpleOption.class})
public abstract class SimpleOptionMixin<T> implements QuickImports {
   @Inject(
      method = {"setValue"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void setValueHook(T value, CallbackInfo ci) {
      if (value instanceof GraphicsMode mode) {
         if (mode.equals(GraphicsMode.FABULOUS)) {
            ci.cancel();
         }
      }

   }
}
