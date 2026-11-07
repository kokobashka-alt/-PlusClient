package ru.zov.mixins;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.file.exception.FileProcessingException;
import ru.zov.common.QuickImports;
import ru.zov.common.util.logger.LoggerUtil;

@Mixin({MinecraftServer.class})
public class MinecraftServerMixin implements QuickImports {
   @Inject(
      method = {"shutdown"},
      at = {@At("HEAD")}
   )
   public void shutdown(CallbackInfo ci) {
      if (main.isInitialized()) {
         try {
            main.getFileController().saveFiles();
         } catch (FileProcessingException e) {
            LoggerUtil.error("Error occurred while saving files: " + e.getMessage());
         }
      }

   }
}
