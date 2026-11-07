package ru.zov.implement.features.modules.player;

import java.util.Comparator;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;

public class Nuker extends Module {
   public BlockPos pos;
   private VoxelShape shape;
   private final BooleanSetting rotateSetting = (new BooleanSetting("Rotate", "Break blocks lower than the player")).setValue(true);
   private final BooleanSetting downSetting = (new BooleanSetting("Down", "Break blocks lower than the player")).setValue(true);
   private final ValueSetting radiusSetting = (new ValueSetting("Radius", "Breaks blocks in a radius around you")).setValue(3.0F).range(1, 6);

   public Nuker() {
      super("Nuker", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.rotateSetting, this.downSetting, this.radiusSetting});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.pos != null && this.shape != null && !this.shape.isEmpty()) {
         Render3DUtil.drawShape(this.pos, this.shape, ColorUtil.getClientColor(), 2.0F);
      }

   }

   @EventHandler(1)
   public void onTick(TickEvent e) {
      this.pos = (BlockPos)PlayerIntersectionUtil.getCube(mc.player.getBlockPos(), (float)this.radiusSetting.getInt(), (float)this.radiusSetting.getInt(), this.downSetting.isValue()).stream().filter(this::validBlock).min(Comparator.comparingDouble(this::blockPriority)).orElse(null);
      if (this.pos != null) {
         if (this.rotateSetting.isValue()) {
            RotationController.INSTANCE.rotateTo(AngleUtil.calculateAngle(this.pos.toCenterPos()), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
         }

         this.shape = mc.world.getBlockState(this.pos).getOutlineShape(mc.world, this.pos);
         mc.interactionManager.updateBlockBreakingProgress(this.pos, Direction.UP);
         mc.player.swingHand(Hand.MAIN_HAND);
      }

   }

   private double blockPriority(BlockPos pos) {
      double var10000;
      switch (mc.world.getBlockState(pos).getBlock().getTranslationKey().replace("block.minecraft.", "")) {
         case "ancient_debris" -> var10000 = (double)0.0F;
         case "diamond_ore" -> var10000 = (double)1.0F;
         case "emerald_ore" -> var10000 = (double)2.0F;
         case "gold_ore" -> var10000 = (double)3.0F;
         case "iron_ore" -> var10000 = (double)4.0F;
         case "lapis_ore" -> var10000 = (double)5.0F;
         case "redstone_ore" -> var10000 = (double)6.0F;
         default -> var10000 = mc.player.squaredDistanceTo(pos.toCenterPos());
      }

      return var10000;
   }

   private boolean validBlock(BlockPos pos) {
      BlockState state = ((ClientWorld)Objects.requireNonNull(mc.world)).getBlockState(pos);
      return !PlayerIntersectionUtil.isAir(state) && state.getBlock() != Blocks.WATER && state.getBlock() != Blocks.LAVA && state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.BARRIER;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public VoxelShape getShape() {
      return this.shape;
   }

   public BooleanSetting getRotateSetting() {
      return this.rotateSetting;
   }

   public BooleanSetting getDownSetting() {
      return this.downSetting;
   }

   public ValueSetting getRadiusSetting() {
      return this.radiusSetting;
   }
}
