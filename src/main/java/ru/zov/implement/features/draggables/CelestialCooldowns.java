package ru.zov.implement.features.draggables.celestial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.ItemStack;

public class CelestialCooldowns extends CelestialListPanel {
   private final Map<String, float[]> samples = new HashMap<>();

   public CelestialCooldowns() {
      super("CelestialCooldowns", "Cooldowns", 0.251F, 0.347F);
   }

   @Override
   protected Row preview() {
      return new Row("p", "Preview Cooldown", null, 0, "**:**", WHITE);
   }

   @Override
   protected List<Row> collect() {
      List<Row> out = new ArrayList<>();
      Set<String> seen = new HashSet<>();
      float now = seconds();
      for (int i = 0; i < 10; i++) {
         ItemStack stack = i < 9 ? mc.player.getInventory().getStack(i) : mc.player.getOffHandStack();
         if (stack.isEmpty()) {
            continue;
         }

         String id = stack.getItem().getTranslationKey();
         if (!seen.add(id)) {
            continue;
         }

         float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
         if (progress <= 0.0F) {
            this.samples.remove(id);
            continue;
         }

         float[] sample = this.samples.computeIfAbsent(id, k -> new float[]{progress, 0.0F, now});
         float elapsed = now - sample[2];
         if (elapsed > 0.05F) {
            float rate = (sample[0] - progress) / elapsed;
            sample[1] = sample[1] == 0.0F ? rate : sample[1] + (rate - sample[1]) * 0.2F;
            sample[0] = progress;
            sample[2] = now;
         }

         String value = sample[1] > 0.001F ? String.format(Locale.ROOT, "%.1fs", progress / sample[1]) : Math.round(progress * 100.0F) + "%";
         out.add(new Row(id, stack.getName().getString(), null, 0, value, WHITE));
      }

      return out;
   }
}
