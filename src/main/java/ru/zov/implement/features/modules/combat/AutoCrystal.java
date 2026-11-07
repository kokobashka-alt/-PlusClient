package ru.zov.implement.features.modules.combat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.EntityUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.PointFinder;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.common.util.rotation.angle.BlockSmoothMode;

public class AutoCrystal extends Module {
   private final Map<BlockPos, StopWatch> crystals = new HashMap<>();
   private final PointFinder pointFinder = new PointFinder();
   private final Script script = new Script();

   public AutoCrystal() {
      super("AutoCrystal", "Auto Crystal", ModuleCategory.COMBAT);
      this.setup();
   }

   @Override
   public void activate() {
      this.crystals.clear();
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.crystals.clear();
   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof PlayerInteractBlockC2SPacket interact
         && interact.getSequence() != 0
         && mc.world != null
         && this.script.isFinished()
         && PlayerInventoryComponent.script.isFinished()) {
         this.script
            .addTickStep(
               0,
               () -> {
                  BlockPos interactPos = interact.getBlockHitResult().getBlockPos();
                  BlockPos spawnPos = interactPos.offset(interact.getBlockHitResult().getSide());
                  BlockPos blockPos = mc.world.getBlockState(spawnPos).getBlock().equals(Blocks.OBSIDIAN)
                     ? spawnPos
                     : (mc.world.getBlockState(interactPos).getBlock().equals(Blocks.OBSIDIAN) ? interactPos : null);
                  Slot crystal = PlayerInventoryUtil.getSlot(Items.END_CRYSTAL);
                  if (mc.crosshairTarget instanceof BlockHitResult hitResult && blockPos != null && crystal != null) {
                     PlayerInventoryComponent.addTask(() -> {
                        this.crystals.put(blockPos, new StopWatch().setMs(-500));
                        PlayerInventoryUtil.swapHand(crystal, Hand.MAIN_HAND, false);
                        PlayerIntersectionUtil.sendSequencedPacket(i -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, i));
                        PlayerInventoryUtil.swapHand(crystal, Hand.MAIN_HAND, false, true);
                     });
                  }
               }
            );
      }
   }

   @EventHandler
   public void onEntityUpdate(EntityUpdateEvent e) {
      if (e.type().equals(EntityUpdateEvent.Type.ADD)
         && e.entity() instanceof EndCrystalEntity crystal
         && this.crystals.containsKey(crystal.getBlockPos().down())
         && mc.player != null
         && mc.interactionManager != null) {
         this.script
            .addTickStep(
               0,
               () -> {
                  Vec3d eye = mc.player.getEyePos();
                  Vec3d closest = this.pointFinder.getClosestVec(mc.player.getEyePos(), crystal.getBoundingBox().expand(-0.1));
                  if (eye.distanceTo(closest) < 4.5) {
                     RotationController.INSTANCE
                        .rotateTo(
                           Angle.VecRotation.of(AngleUtil.calculateAngle(closest).random(1.0F)),
                           mc.player,
                           1,
                           new RotationConfig(new BlockSmoothMode(), true, true),
                           TaskPriority.HIGH_IMPORTANCE_3,
                           this
                        );
                     mc.interactionManager.attackEntity(mc.player, crystal);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.crystals.remove(crystal.getBlockPos().down());
                  }
               }
            );
      }
   }

   @EventHandler(0)
   public void onTick(TickEvent e) {
      this.crystals.values().removeIf(StopWatch::finished);
      this.script.update().cleanupIfFinished();
   }

   public Map<BlockPos, StopWatch> getCrystals() {
      return this.crystals;
   }

   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   public Script getScript() {
      return this.script;
   }
}
