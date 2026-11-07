package ru.zov.implement.features.modules.misc.auction;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.apache.commons.lang3.StringUtils;
import ru.zov.common.QuickImports;
import ru.zov.common.util.world.ServerUtil;

public final class AuctionUtil implements QuickImports {
   public static int getPrice(Slot slot) {
      return getPrice(slot.getStack());
   }

   public static int getPrice(ItemStack stack) {
      LoreComponent lore = stack.get(DataComponentTypes.LORE);
      if (lore == null) {
         return -1;
      }

      String price = getPrice(lore.toString());
      return price != null && !price.isEmpty() ? Integer.parseInt(price.replace(",", "").replace(" ", "")) : -1;
   }

   private static String getPrice(String string) {
      return switch (ServerUtil.server) {
         case "HolyWorld" -> StringUtils.substringBetween(
            string, "Цена:}[style={color=#D5DBDC,!italic}], literal{ }[style={color=#00D8FF,!italic}, siblings=[literal{", "}, literal{¤}]"
         );
         case "SpookyTime" -> StringUtils.substringBetween(string, "{Цена: }[style={color=white,!italic}], literal{$", "}[");
         case "FunTime" -> StringUtils.substringBetween(string, "literal{ Ценa}[style={color=white}], literal{ $", "}[");
         default -> null;
      };
   }

   private AuctionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
