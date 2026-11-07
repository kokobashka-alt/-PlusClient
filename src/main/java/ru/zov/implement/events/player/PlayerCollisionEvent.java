package ru.zov.implement.events.player;

import net.minecraft.block.Block;
import ru.zov.api.event.events.callables.EventCancellable;

public class PlayerCollisionEvent extends EventCancellable {
   private Block block;

   public void setBlock(Block block) {
      this.block = block;
   }

   public Block getBlock() {
      return this.block;
   }

   public PlayerCollisionEvent(Block block) {
      this.block = block;
   }
}
