package ru.zov.implement.features.modules.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.Box;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.world.BlockFinder;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class HighJump extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final SelectSetting mode = new SelectSetting("Mode", "Selects the type of mode").value("Grim Shulker");

   public HighJump() {
      super("HighJump", "High Jump", ModuleCategory.MOVEMENT);
      this.setup(this.mode);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      BlockFinder.INSTANCE.blockEntities.forEach(blockEntity -> {
         BlockState state = blockEntity.getCachedState();
         if (blockEntity instanceof ShulkerBoxBlockEntity shulker && state.get(ShulkerBoxBlock.FACING).getOffsetY() != 0 && shulker.animationProgress > 0.0F) {
            Render3DUtil.drawBox(shulker.getBoundingBox(state).offset(shulker.getPos()), ColorUtil.LIGHT_RED, 1.5F, true, true, true);
         }
      });
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.stopWatch.finished(1000.0)) {
         BlockFinder.INSTANCE
            .blockEntities
            .stream()
            .filter(
               be -> be instanceof ShulkerBoxBlockEntity shulker
                  && shulker.getCachedState().get(ShulkerBoxBlock.FACING).getOffsetY() != 0
                  && shulker.animationProgress > 0.0F
                  && mc.player.getBoundingBox().expand(0.7, 1.0, 0.7).intersects(new Box(shulker.getPos()))
            )
            .findFirst()
            .ifPresent(blockEntity -> this.stopWatch.reset());
      }

      if (!this.stopWatch.finished(199.0)) {
         mc.player.velocity.y += 0.899;
      }
   }
}
