package ru.zov.implement.features.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.command.Command;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.api.repository.RCTRepository;
import ru.zov.common.QuickImports;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.core.Main;
import ru.zov.implement.features.draggables.Notifications;

public class RCTCommand extends Command implements QuickImports {
   private final RCTRepository repository;

   protected RCTCommand(Main main) {
      super("rct");
      this.repository = main.getRCTRepository();
   }

   @Native
   @Override
   public void execute(String label, IArgConsumer args) throws CommandException {
      if (!ServerUtil.isHolyWorld()) {
         Notifications.getInstance().addList("[RCT] Не работает на этом " + Formatting.RED + "сервере", 3000L);
      } else if (ServerUtil.isPvp()) {
         Notifications.getInstance().addList("[RCT] Вы находитесь в режиме " + Formatting.RED + "пвп", 3000L);
      } else {
         if (args.hasAny()) {
            args.requireMin(1);
            int anarchy = args.getArgs().getFirst().getAs(Integer.class);
            this.repository.reconnect(anarchy);
         } else {
            this.repository.reconnect(ServerUtil.getAnarchy());
         }
      }
   }

   @Native
   @Override
   public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
      return Stream.empty();
   }

   @Native
   @Override
   public String getShortDesc() {
      return "Перезаходит на анархию";
   }

   @Native
   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Перезаходит на анархию", "", "Использование:", "> rct <anarchy> - Заходит на <anarchy>", "> rct - Перезаходит на анархию где вы только что были"
      );
   }
}
