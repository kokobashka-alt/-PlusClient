package ru.zov.common.util.performance;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import ru.zov.common.util.logger.LoggerUtil;

public final class PerformanceConfig {
   private PerformanceConfig() {
   }

   public static void install() {
      FabricLoader loader = FabricLoader.getInstance();
      if (loader.getEnvironmentType() != EnvType.CLIENT || !loader.isModLoaded("sodium")) {
         return;
      }

      ClientLifecycleEvents.CLIENT_STARTED.register(client -> apply());
   }

   private static void apply() {
      try {
         SodiumOptions options = SodiumClientMod.options();
         boolean changed = false;

         changed |= setQualityOptions(options);
         changed |= setPerformanceOptions(options);
         changed |= setAdvancedOptions(options);

         if (changed) {
            SodiumOptions.writeToDisk(options);
            LoggerUtil.info("Applied the ZOV maximum-performance Sodium profile");
         } else {
            LoggerUtil.info("Verified the ZOV maximum-performance Sodium profile");
         }
      } catch (Throwable throwable) {
         // A Sodium update should never prevent the client itself from starting.
         LoggerUtil.warn("Could not apply the Sodium performance profile", throwable);
      }
   }

   private static boolean setQualityOptions(SodiumOptions options) {
      boolean changed = false;

      changed |= options.quality.hiddenFluidCulling != true;
      options.quality.hiddenFluidCulling = true;
      changed |= options.quality.improvedFluidShaping != false;
      options.quality.improvedFluidShaping = false;
      changed |= options.quality.useClosestPointEntitySort != false;
      options.quality.useClosestPointEntitySort = false;

      return changed;
   }

   private static boolean setPerformanceOptions(SodiumOptions options) {
      boolean changed = false;

      changed |= options.performance.chunkBuilderThreads != 0;
      options.performance.chunkBuilderThreads = 0;
      changed |= options.performance.chunkBuildDeferMode != DeferMode.ALWAYS;
      options.performance.chunkBuildDeferMode = DeferMode.ALWAYS;
      changed |= options.performance.animateOnlyVisibleTextures != true;
      options.performance.animateOnlyVisibleTextures = true;
      changed |= options.performance.useEntityCulling != true;
      options.performance.useEntityCulling = true;
      changed |= options.performance.useFogOcclusion != true;
      options.performance.useFogOcclusion = true;
      changed |= options.performance.useBlockFaceCulling != true;
      options.performance.useBlockFaceCulling = true;
      changed |= options.performance.useNoErrorGLContext != true;
      options.performance.useNoErrorGLContext = true;
      changed |= options.performance.quadSplittingMode != QuadSplittingMode.OFF;
      options.performance.quadSplittingMode = QuadSplittingMode.OFF;

      return changed;
   }

   private static boolean setAdvancedOptions(SodiumOptions options) {
      boolean changed = false;

      changed |= options.advanced.enableMemoryTracing != false;
      options.advanced.enableMemoryTracing = false;
      changed |= options.advanced.useAdvancedStagingBuffers != true;
      options.advanced.useAdvancedStagingBuffers = true;
      changed |= options.advanced.cpuRenderAheadLimit != 3;
      options.advanced.cpuRenderAheadLimit = 3;

      return changed;
   }
}
