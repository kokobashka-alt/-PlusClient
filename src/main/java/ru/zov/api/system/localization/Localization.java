package ru.zov.api.system.localization;

import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.QuickImports;

public class Localization implements QuickImports {
   private static final Map<Language, Map<String, String>> cache = new ConcurrentHashMap<>();

   public static String get(String key) {
      Map<String, String> translations = cache.computeIfAbsent(Language.ENG, Localization::loadTranslations);
      return translations.getOrDefault(key, key);
   }

   @Native
   private static Map<String, String> loadTranslations(Language language) {
      try {
         Identifier identifier = Identifier.of("translations/" + language.getFile() + ".json");
         InputStream stream = mc.getResourceManager().getResource(identifier).get().getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
         return (Map<String, String>)gson.fromJson(reader, (new TypeToken<Map<String, String>>() {}).getType());
      } catch (Throwable $ex) {
         throw new RuntimeException("Unable to load localization", $ex);
      }
   }
}
