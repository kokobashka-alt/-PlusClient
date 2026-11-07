package ru.zov.implement.events.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import ru.zov.api.event.events.Event;

public class BlockCollisionEvent implements Event {
   BlockState state;
   BlockPos pos;

   public void setState(BlockState state) {
      this.state = state;
   }

   public void setPos(BlockPos pos) {
      this.pos = pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockCollisionEvent(BlockState state, BlockPos pos) {
      this.state = state;
      this.pos = pos;
   }
}
