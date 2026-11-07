package ru.zov.api.system.font;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.core.Main;

public class Fonts {
   private static final Map<Fonts.FontKey, FontRenderer> fontCache = new HashMap<>();

   @Native
   public static FontRenderer create(float size, String name) {
      try {
         String path = "assets/minecraft/fonts/" + name + ".otf";

         try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(path)) {
            Font font = Font.createFont(0, Objects.requireNonNull(inputStream)).deriveFont(0, size / 2.0F);
            return new FontRenderer(font, size / 2.0F);
         }
      } catch (Throwable $ex) {
         throw new RuntimeException("Unable to load font", $ex);
      }
   }

   @Native
   public static void init() {
      for (Fonts.Type type : Fonts.Type.values()) {
         for (int size = 4; size <= 32; size++) {
            fontCache.put(new Fonts.FontKey(size, type), create(size, type.getType()));
         }
      }
   }

   public static FontRenderer getSize(int size) {
      return getSize(size, Fonts.Type.BOLD);
   }

   public static FontRenderer getSize(int size, Fonts.Type type) {
      return fontCache.computeIfAbsent(new Fonts.FontKey(size, type), k -> create(size, type.getType()));
   }

   private record FontKey(int size, Fonts.Type type) {
   }

   public enum Type {
      DEFAULT("sfpromedium"),
      BOLD("sfprosemibold");

      private final String type;

      public String getType() {
         return this.type;
      }

      Type(final String type) {
         this.type = type;
      }
   }
}
