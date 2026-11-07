package ru.zov.api.file;

import java.util.ArrayList;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.file.impl.AutoFarmFile;
import ru.zov.api.file.impl.BlockESPFile;
import ru.zov.api.file.impl.EntityESPFile;
import ru.zov.api.file.impl.FriendFile;
import ru.zov.api.file.impl.MacroFile;
import ru.zov.api.file.impl.ModuleFile;
import ru.zov.api.file.impl.PrefixFile;
import ru.zov.api.file.impl.WayFile;
import ru.zov.core.Main;

public class FileRepository {
   private final List<ClientFile> clientFiles = new ArrayList<>();

   @Native
   public void setup(Main main) {
      this.register(
         new ModuleFile(main),
         new AutoFarmFile(main),
         new EntityESPFile(main),
         new BlockESPFile(main),
         new MacroFile(main),
         new WayFile(main),
         new PrefixFile(),
         new FriendFile()
      );
   }

   public void register(ClientFile... clientFIle) {
      this.clientFiles.addAll(List.of(clientFIle));
   }

   public List<ClientFile> getClientFiles() {
      return this.clientFiles;
   }
}
