package ru.zov.api.repository;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class BoxESPRepository implements QuickImports, QuickLogger {
   private final Map<BlockPos, Pair<VoxelShape, Integer>> boxes = new HashMap();
   public final Map<EntityType<?>, Integer> entities = new HashMap();
   public final Map<Block, Integer> blocks = new HashMap();
   public boolean drawFill = true;

   public BoxESPRepository(EventManager eventManager) {
      eventManager.register(this);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.boxes.forEach((pos, pair) -> {
         HitResult patt0$temp = mc.crosshairTarget;
         if (patt0$temp instanceof BlockHitResult hitResult) {
            if (hitResult.getBlockPos().equals(pos)) {
               return;
            }
         }

         if (this.drawFill) {
            Render3DUtil.drawShape(pos, (VoxelShape)pair.getLeft(), (Integer)pair.getRight(), 1.0F);
         } else {
            Render3DUtil.drawShapeAlternative(pos, (VoxelShape)pair.getLeft(), (Integer)pair.getRight(), 1.0F, false, false);
         }

      });
      PlayerIntersectionUtil.streamEntities().filter((ent) -> this.entities.containsKey(ent.getType()) && ent != mc.player).forEach((ent) -> {
         int entityColor = (Integer)this.entities.get(ent.getType());
         int color = entityColor == 0 ? ColorUtil.getClientColor() : entityColor;
         Box box = ent.getBoundingBox().offset(MathUtil.interpolate(ent).subtract(ent.getEntityPos()));
         if (ProjectionUtil.canSee(box)) {
            Render3DUtil.drawBox(box, color, 1.0F);
         }

      });
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.boxes.clear();
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent e) {
      switch (e.type()) {
         case LOAD -> e.list().forEach((data) -> {
   BlockState state = data.state();
   Block block = state.getBlock();
   BlockPos pos = data.pos();
   if (this.blocks.containsKey(block)) {
      this.putBox(pos, state, block);
   }

});
         case UPDATE -> e.list().forEach((data) -> {
   BlockState state = data.state();
   Block block = state.getBlock();
   BlockPos pos = data.pos();
   if (this.boxes.containsKey(pos) && (state.isAir() || !((VoxelShape)((Pair)this.boxes.get(pos)).getLeft()).equals(state.getOutlineShape(mc.world, pos)))) {
      this.boxes.remove(pos);
   }

   if (this.blocks.containsKey(block) && !this.boxes.containsKey(pos)) {
      this.putBox(pos, state, block);
   }

});
         case UNLOAD -> e.list().forEach((data) -> this.boxes.remove(data.pos()));
      }
      if (e.type() == BlockUpdateEvent.Type.UNLOAD && e.chunkPos() != null) {
         this.boxes.keySet().removeIf(pos -> pos.getX() >> 4 == e.chunkPos().x && pos.getZ() >> 4 == e.chunkPos().z);
      }

   }

   private void putBox(BlockPos pos, Block block) {
      this.putBox(pos, block.getDefaultState(), block);
   }

   private void putBox(BlockPos pos, BlockState state, Block block) {
      VoxelShape shape = state.getOutlineShape(mc.world, pos);
      int blockColor = (Integer)this.blocks.get(block);
      int color = blockColor == 0 ? ColorUtil.replAlpha(state.getMapColor(mc.world, pos).color, 1.0F) : blockColor;
      this.boxes.put(pos, new Pair(shape, color));
   }
}
