package ru.zov.api.feature.command.datatypes;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.feature.command.helpers.TabCompleteHelper;
import ru.zov.common.util.other.StringUtil;

public enum KeyDataType implements IDatatypeFor<java.util.Map.Entry<String, Integer>> {
   INSTANCE;

   @Override
   public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
      Stream<String> keys = getKeys().keySet().stream();
      String context = datatypeContext.getConsumer().getString();
      return new TabCompleteHelper().append(keys).filterPrefix(context).sortAlphabetically().stream();
   }

   public java.util.Map.Entry<String, Integer> get(IDatatypeContext datatypeContext) throws CommandException {
      String key = datatypeContext.getConsumer().getString();
      return getKeys().entrySet().stream().filter(s -> s.getKey().equalsIgnoreCase(key)).findFirst().orElse(null);
   }

   private static Map<String, Integer> getKeys() {
      Map<String, Integer> keys = new HashMap<>();
      ObjectIterator var1 = Type.KEYSYM.map.int2ObjectEntrySet().iterator();

      while (var1.hasNext()) {
         Entry<Key> entry = (Entry<Key>)var1.next();
         int keyCode = entry.getIntKey();
         String bindName = StringUtil.getBindName(keyCode).toLowerCase();
         keys.put(bindName, keyCode);
      }

      return keys;
   }
}
