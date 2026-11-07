package ru.zov.common.util.item;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.ItemCooldownManager.Entry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import ru.zov.common.QuickImports;

public final class ItemUtil implements QuickImports {
   public static int maxUseTick(Item item) {
      return maxUseTick(item.getDefaultStack());
   }

   public static int maxUseTick(ItemStack stack) {
      return switch (stack.getUseAction()) {
         case EAT, DRINK -> 32;
         case CROSSBOW, SPEAR -> 10;
         case BOW -> 20;
         case BLOCK -> 0;
         default -> stack.getMaxUseTime(mc.player);
      };
   }

   public static boolean hasCooldown(Item item) {
      return hasCooldown(item.getDefaultStack());
   }

   public static boolean hasCooldown(ItemStack stack) {
      return getCooldownProgress(stack) > 0.0F;
   }

   public static float getCooldownProgress(Item item) {
      return getCooldownProgress(item.getDefaultStack());
   }

   public static float getCooldownProgress(ItemStack stack) {
      if (mc.player == null) {
         return 0.0F;
      }

      ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
      Entry entry = cooldownManager.entries.get(cooldownManager.getGroup(stack));
      return entry == null ? 0.0F : Math.max(0.0F, (entry.endTick - cooldownManager.tick) / 20.0F);
   }

   public static int getEnchantmentLvl(ItemStack stack, RegistryKey<Enchantment> key) {
      return mc.world == null
         ? 0
         : EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(key.getValue()).orElse(null), stack);
   }

   private ItemUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
