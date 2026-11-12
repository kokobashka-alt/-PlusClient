package ru.zov.implement.features.draggables.celestial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;

public class CelestialStaff extends CelestialListPanel {
   private static final String[] STAFF_WORDS = {
      "admin", "moder", "helper", "owner", "curator", "staff", "dev", "support",
      "админ", "модер", "хелпер", "куратор", "стаж", "владел", "разработ", "саппорт"
   };

   public CelestialStaff() {
      super("CelestialStaff", "Staff Viewer", 0.588F, 0.6875F);
   }

   @Override
   protected Row preview() {
      return new Row("p", "Preview Staff", null, 0, "Status", YELLOW);
   }

   @Override
   protected List<Row> collect() {
      List<Row> out = new ArrayList<>();
      if (mc.getNetworkHandler() == null) {
         return out;
      }

      for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
         String name = entry.getProfile().name();
         if (name.equals(mc.player.getGameProfile().name())) {
            continue;
         }

         Team team = entry.getScoreboardTeam();
         String tag = (team == null ? "" : team.getPrefix().getString()) + (entry.getDisplayName() == null ? "" : entry.getDisplayName().getString());
         tag = tag.toLowerCase(Locale.ROOT);
         boolean staff = false;
         for (String word : STAFF_WORDS) {
            if (tag.contains(word)) {
               staff = true;
               break;
            }
         }

         if (!staff) {
            continue;
         }

         boolean near = mc.world.getPlayerByUuid(entry.getProfile().id()) != null;
         out.add(new Row(name, name, null, 0, near ? "Near" : "Online", near ? GREEN : YELLOW));
      }

      return out;
   }
}
