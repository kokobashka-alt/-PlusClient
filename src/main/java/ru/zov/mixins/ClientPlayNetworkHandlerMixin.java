package ru.zov.mixins;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.core.Main;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.events.chat.ChatEvent;
import ru.zov.implement.features.draggables.MiniMap;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin {
   @Shadow
   private ClientWorld world;

   @Inject(
      method = {"onChunkData"},
      at = {@At("RETURN")}
   )
   private void onChunkDataHook(ChunkDataS2CPacket packet, CallbackInfo ci) {
      WorldChunk chunk = this.world.getChunk(packet.getChunkX(), packet.getChunkZ());
      BlockUpdateEvent.Type type = BlockUpdateEvent.Type.LOAD;
      MiniMap.getInstance().onBlockUpdate(chunk, type, BlockPos.ORIGIN);
      this.scanChunk(chunk, type);
   }

   @Inject(
      method = {"onUnloadChunk"},
      at = {@At("HEAD")}
   )
   private void onUnloadChunkHook(UnloadChunkS2CPacket packet, CallbackInfo ci) {
      WorldChunk chunk = this.world.getChunk(packet.pos().x, packet.pos().z);
      BlockUpdateEvent.Type type = BlockUpdateEvent.Type.UNLOAD;
      MiniMap.getInstance().onBlockUpdate(chunk, type, BlockPos.ORIGIN);
      EventManager.callEvent(new BlockUpdateEvent(List.of(), type, packet.pos()));
   }

   @Inject(
      method = {"sendChatMessage(Ljava/lang/String;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendChatMessage(String string, CallbackInfo ci) {
      ChatEvent event = new ChatEvent(string);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Unique
   private void scanChunk(WorldChunk worldChunk, BlockUpdateEvent.Type type) {
      int startX = worldChunk.getPos().getStartX();
      int startZ = worldChunk.getPos().getStartZ();
      List<BlockUpdateEvent.BlockData> list = new ArrayList<>();
      var trackedBlocks = Main.getInstance().getBoxESPRepository().blocks;

      for(int sectionIndex = 0; sectionIndex <= worldChunk.getHighestNonEmptySection(); ++sectionIndex) {
         ChunkSection section = worldChunk.getSection(sectionIndex);
         if (section.isEmpty()) {
            continue;
         }
         int bottomY = sectionIndex + (worldChunk.getBottomY() >> 4) << 4;

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = 0; y < 16; ++y) {
                  BlockState state = section.getBlockState(x, y, z);
                  if (state.isOf(Blocks.NETHER_WART) || trackedBlocks.containsKey(state.getBlock())) {
                     BlockPos pos = new BlockPos(startX | x, bottomY | y, startZ | z);
                     list.add(new BlockUpdateEvent.BlockData(pos, state, 0));
                  }
               }
            }
         }
      }

      if (!list.isEmpty()) {
         EventManager.callEvent(new BlockUpdateEvent(list, type, worldChunk.getPos()));
      }
   }
}
