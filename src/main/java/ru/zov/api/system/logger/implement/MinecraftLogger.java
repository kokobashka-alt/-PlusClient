package ru.zov.api.system.logger.implement;

import java.util.Arrays;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.zov.api.system.logger.Logger;
import ru.zov.common.QuickImports;

public class MinecraftLogger implements Logger, QuickImports {
   @Override
   public void log(Object message) {
   }

   @Override
   public void minecraftLog(Text... components) {
      if (mc.player != null) {
         MutableText component = Text.literal("");
         Arrays.asList(components).forEach(component::append);
         mc.inGameHud.getChatHud().addMessage(component);
      }
   }
}
