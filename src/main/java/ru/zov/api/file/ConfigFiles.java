package ru.zov.api.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Name handling and listing for user config files (<name>.json) in the configs directory. */
public final class ConfigFiles {
   private static final String EXTENSION = ".json";
   private static final int MAX_NAME_LENGTH = 64;
   private static final Pattern FORBIDDEN = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

   private ConfigFiles() {
   }

   /** Returns the config name without extension, or null when it is empty or could escape the configs directory. */
   public static String normalizeName(String raw) {
      if (raw == null) {
         return null;
      }

      String name = raw.trim();
      if (name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
         name = name.substring(0, name.length() - EXTENSION.length()).trim();
      }

      if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || name.startsWith(".") || FORBIDDEN.matcher(name).find()) {
         return null;
      }

      return name;
   }

   public static String fileName(String name) {
      return name + EXTENSION;
   }

   public static File file(File configsDir, String name) {
      return new File(configsDir, fileName(name));
   }

   public static List<String> list(File configsDir) {
      List<String> names = new ArrayList<>();
      File[] files = configsDir.listFiles();
      if (files != null) {
         for (File file : files) {
            String fileName = file.getName();
            if (file.isFile() && fileName.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
               names.add(fileName.substring(0, fileName.length() - EXTENSION.length()));
            }
         }
      }

      names.sort(String.CASE_INSENSITIVE_ORDER);
      return names;
   }
}
