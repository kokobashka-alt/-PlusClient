package ru.zov.common.util.other;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

public final class StringUtil {
   public static String randomString(int length) {
      return IntStream.range(0, length).mapToObj(operand -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
   }

   public static String getBindName(int key) {
      return key < 0
         ? "N/A"
         : PlayerIntersectionUtil.getKeyType(key)
            .createFromCode(key)
            .getTranslationKey()
            .replace("key.keyboard.", "")
            .replace("key.mouse.", "mouse ")
            .replace(".", " ")
            .toUpperCase();
   }

   public static String wrap(String input, int width, int size) {
      String[] words = input.split(" ");
      StringBuilder output = new StringBuilder();
      float lineWidth = 0.0F;

      for (String word : words) {
         float wordWidth = Fonts.getSize(size).getStringWidth(word);
         if (lineWidth + wordWidth > width) {
            output.append("\n");
            lineWidth = 0.0F;
         } else if (lineWidth > 0.0F) {
            output.append(" ");
            lineWidth += Fonts.getSize(size).getStringWidth(" ");
         }

         output.append(word);
         lineWidth += wordWidth;
      }

      return output.toString();
   }

   public static String getUserRole() {
      return "ADMIN";
   }

   public static String getDuration(int time) {
      return getDurationMS(time * 10, false);
   }

   public static String getDurationMS(int time, boolean drawMs) {
      if (time == -1) {
         return "** **";
      } else {
         int ms = time % 10;
         int second = time / 10;
         int sec = second % 60;
         int totalMinutes = second / 60;
         int min = totalMinutes % 60;
         int totalHours = totalMinutes / 60;
         int hour = totalHours % 24;
         int day = totalHours / 24;
         if (totalMinutes == 0) {
            return sec + (drawMs ? "," + ms : "") + "s";
         } else if (totalHours == 0) {
            return String.format("%2dm %2ds", min, sec);
         } else {
            return day == 0 ? String.format("%2dh %2dm %2ds", hour, min, sec) : String.format("%2dd %2dh %2dm %2ds", day, hour, min, sec);
         }
      }
   }

   private StringUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
