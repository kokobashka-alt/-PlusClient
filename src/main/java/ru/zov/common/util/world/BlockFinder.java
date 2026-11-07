package ru.zov.common.util.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import ru.zov.api.event.EventHandler;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.block.BlockEntityProgressEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;

public class BlockFinder implements QuickImports {
   public final List<BlockEntity> blockEntities = new ArrayList<>();
   public static BlockFinder INSTANCE = new BlockFinder();

   public BlockFinder() {
      main.getEventManager().register(this);
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.blockEntities.clear();
   }

   @EventHandler
   public void onBlockEntityProgress(BlockEntityProgressEvent e) {
      BlockEntity blockEntity = e.blockEntity();
      switch (e.type()) {
         case ADD:
            this.blockEntities.add(blockEntity);
            break;
         case REMOVE:
            this.blockEntities.remove(blockEntity);
      }
   }
}
