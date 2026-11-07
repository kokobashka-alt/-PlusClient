package ru.zov.api.system.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

public final class SoundManager implements QuickImports {
   public static SoundEvent OPEN_GUI = SoundEvent.of(Identifier.of("minecraft:gui_open"));
   public static SoundEvent CLOSE_GUI = SoundEvent.of(Identifier.of("minecraft:gui_close"));
   public static SoundEvent ENABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_enable"));
   public static SoundEvent DISABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_disable"));

   @Native
   public static void init() {
      Registry.register(Registries.SOUND_EVENT, OPEN_GUI.id(), OPEN_GUI);
      Registry.register(Registries.SOUND_EVENT, CLOSE_GUI.id(), CLOSE_GUI);
      Registry.register(Registries.SOUND_EVENT, ENABLE_MODULE.id(), ENABLE_MODULE);
      Registry.register(Registries.SOUND_EVENT, DISABLE_MODULE.id(), DISABLE_MODULE);
   }

   @Native
   public static void playSound(SoundEvent sound) {
      playSound(sound, 1.0F, 1.0F);
   }

   @Native
   public static void playSound(SoundEvent sound, float volume, float pitch) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
      }
   }

   private SoundManager() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
