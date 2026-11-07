package ru.zov.mixins;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.player.PlayerCollisionEvent;

@Mixin({AbstractBlock.AbstractBlockState.class})
public abstract class AbstractBlockStateMixin implements QuickImports {
   @Shadow
   public abstract Block getBlock();

   @Inject(
      method = {"onEntityCollision"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onEntityCollision(World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean defaultHandler, CallbackInfo ci) {
      if (entity == mc.player) {
         PlayerCollisionEvent event = new PlayerCollisionEvent(this.getBlock());
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
         }

      }
   }
}
