package ru.zov.implement.features.draggables.celestial;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;

public class CelestialPotions extends CelestialListPanel {
   public CelestialPotions() {
      super("CelestialPotions", "Potions", 0.494F, 0.303F);
   }

   @Override
   protected Row preview() {
      return new Row("p", "Preview Potion", "999", RED, "**:**", WHITE);
   }

   @Override
   protected List<Row> collect() {
      List<Row> out = new ArrayList<>();
      for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
         String name = effect.getEffectType().value().getName().getString();
         String tag = effect.getAmplifier() > 0 ? String.valueOf(effect.getAmplifier() + 1) : null;
         String value;
         if (effect.isInfinite()) {
            value = "**:**";
         } else {
            int seconds = effect.getDuration() / 20;
            value = (seconds / 60) + ":" + String.format("%02d", seconds % 60);
         }

         out.add(new Row(effect.getTranslationKey(), name, tag, RED, value, WHITE));
      }

      return out;
   }
}
