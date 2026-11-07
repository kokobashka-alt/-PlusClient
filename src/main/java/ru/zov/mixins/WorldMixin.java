package ru.zov.mixins;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.features.draggables.MiniMap;

@Mixin({World.class})
public abstract class WorldMixin implements QuickImports {
   @Inject(
      method = {"setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z"},
      at = {@At("RETURN")}
   )
   private void setBlockStateHook(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof ClientWorld world) {
         BlockPos blockPos = pos.toImmutable();
         BlockUpdateEvent.Type type = BlockUpdateEvent.Type.UPDATE;
         EventManager.callEvent(new BlockUpdateEvent(List.of(new BlockUpdateEvent.BlockData(blockPos, state, 0)), type));
         MiniMap.getInstance().onBlockUpdate(world.getWorldChunk(blockPos), type, blockPos);
      }

   }
}
