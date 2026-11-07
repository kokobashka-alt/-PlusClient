package ru.zov.implement.features.modules.player;

import java.util.Comparator;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.block.BlockBreakingEvent;
import ru.zov.implement.events.keyboard.HotBarScrollEvent;
import ru.zov.implement.events.player.HotBarUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.ItemRendererEvent;

public class AutoTool extends Module {
   private final StopWatch swap = new StopWatch();
   private final StopWatch breaking = new StopWatch();
   private final Script script = new Script();
   private final Script swapBackScript = new Script();
   private ItemStack renderStack;
   private BlockPos lastBreakPos;

   public AutoTool() {
      super("AutoTool", "Auto Tool", ModuleCategory.PLAYER);
   }

   @EventHandler
   public void onItemRenderer(ItemRendererEvent e) {
      if (this.renderStack != null && e.getHand().equals(Hand.MAIN_HAND) && Objects.equals(mc.player, e.getPlayer())) {
         e.setStack(this.renderStack);
      }
   }

   @EventHandler
   public void onHotBarUpdate(HotBarUpdateEvent e) {
      if (!this.swapBackScript.isFinished()) {
         e.cancel();
      }
   }

   @EventHandler
   public void onHotBarScroll(HotBarScrollEvent e) {
      if (!this.swapBackScript.isFinished()) {
         e.cancel();
      }
   }

   @EventHandler
   @Native
   public void onBlockBreaking(BlockBreakingEvent e) {
      this.breaking.reset();
      this.lastBreakPos = e.blockPos();
      if (!mc.player.isCreative() && this.swapBackScript.isFinished() && this.swap.finished(350.0)) {
         Slot currentBestSlot = this.findBestTool(this.lastBreakPos);
         if (currentBestSlot != null && currentBestSlot != PlayerInventoryUtil.mainHandSlot()) {
            this.renderStack = mc.player.getMainHandStack();
            PlayerInventoryUtil.swapHand(currentBestSlot, Hand.MAIN_HAND, true);
            this.swapBackScript.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(currentBestSlot, Hand.MAIN_HAND, true, true));
            this.swap.reset();
         }
      }
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      this.script.update();
      if (!this.swapBackScript.isFinished() && this.swap.finished(350.0)) {
         Slot currentBestSlot = this.findBestTool(this.lastBreakPos);
         if (currentBestSlot != null && currentBestSlot != PlayerInventoryUtil.mainHandSlot() || this.breaking.finished(100.0)) {
            this.script.cleanup().addTickStep(4, () -> this.renderStack = null);
            this.swapBackScript.update();
            this.swap.reset();
         }
      }
   }

   @Native
   private Slot findBestTool(BlockPos blockPos) {
      BlockState state = mc.world.getBlockState(blockPos);
      Slot mainHand = PlayerInventoryUtil.mainHandSlot();
      return PlayerIntersectionUtil.isAir(state)
         ? mainHand
         : PlayerInventoryUtil.slots()
            .max(Comparator.comparingDouble(s -> s.getStack().getMiningSpeedMultiplier(state) + (s.equals(mainHand) ? 1.0E-8 : 0.0)))
            .orElse(null);
   }
}
