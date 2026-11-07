package ru.zov.common.util.world;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import ru.zov.common.QuickImports;
import ru.zov.common.util.math.StopWatch;
import ru.zov.implement.events.packet.PacketEvent;

public final class ServerUtil implements QuickImports {
   private static final StopWatch pvpWatch = new StopWatch();
   public static String server = "Vanilla";
   public static float TPS = 20.0F;
   public static long timestamp;
   public static int anarchy;

   public static void tick() {
      anarchy = getAnarchyMode();
      if (inPvp()) {
         pvpWatch.reset();
      }
   }

   public static void updateServerInfo(ServerInfo info, String brand) {
      server = updateServer(info, brand.toLowerCase());
   }

   public static void packet(PacketEvent e) {
      if (e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         long nanoTime = System.nanoTime();
         float maxTPS = 20.0F;
         float rawTPS = maxTPS * (1.0E9F / (float)(nanoTime - timestamp));
         TPS = MathHelper.clamp(rawTPS, 0.0F, maxTPS);
         timestamp = nanoTime;
      }
   }

   private static String updateServer(ServerInfo info, String brand) {
      if (info != null && brand != null) {
         String address = info.address;
         if (brand.contains("botfilter")) {
            return "FunTime";
         } else if (brand.contains("§6spooky§ccore")) {
            return "SpookyTime";
         } else if (address.contains("funtime") || address.contains("skytime") || address.contains("space-times") || address.contains("funsky")) {
            return "CopyTime";
         } else if (brand.contains("holyworld") || brand.contains("vk.com/idwok") || brand.contains("leaf")) {
            return "HolyWorld";
         } else {
            return address.contains("reallyworld") ? "ReallyWorld" : "Vanilla";
         }
      } else {
         return "Vanilla";
      }
   }

   private static int getAnarchyMode() {
      Text header = mc.inGameHud.getPlayerListHud().header;
      switch (server) {
         case "FunTime":
            if (header != null) {
               String[] split = header.getString().split("-");
               if (split.length > 1) {
                  return Integer.parseInt(split[1]);
               }
            }
            break;
         case "HolyWorld":
            if (header != null && !header.getString().contains("Лобби")) {
               String[] split = header.getString().split("#");
               if (split.length > 1) {
                  return Integer.parseInt(split[1].replace("\n", ""));
               }
            }
      }

      return -1;
   }

   public static boolean isPvp() {
      return !pvpWatch.finished(500.0);
   }

   private static boolean inPvp() {
      return mc.inGameHud
         .getBossBarHud()
         .bossBars
         .values()
         .stream()
         .map(c -> c.getName().getString().toLowerCase())
         .anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
   }

   public static String getWorldType() {
      return mc.world.getRegistryKey().getValue().getPath();
   }

   public static Text getHeader() {
      Text header = mc.inGameHud.getPlayerListHud().header;
      return header != null ? header : Text.empty();
   }

   public static boolean isCopyTime() {
      return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");
   }

   public static boolean isFunTime() {
      return server.equals("FunTime");
   }

   public static boolean isReallyWorld() {
      return server.equals("ReallyWorld");
   }

   public static boolean isHolyWorld() {
      return server.equals("HolyWorld");
   }

   public static boolean isSpookyTime() {
      return server.equals("SpookyTime");
   }

   public static boolean isVanilla() {
      return server.equals("Vanilla");
   }

   private ServerUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static int getAnarchy() {
      return anarchy;
   }
}
