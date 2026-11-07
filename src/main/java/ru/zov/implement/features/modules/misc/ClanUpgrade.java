package ru.zov.implement.features.modules.misc;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;

public class ClanUpgrade extends Module {
   private final StopWatch stopWatch = new StopWatch();

   public ClanUpgrade() {
      super("ClanUpgrade", "Clan Upgrade", ModuleCategory.MISC);
      this.setup(new Setting[0]);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (ServerUtil.getWorldType().equals("lobby") && this.stopWatch.every((double)5000.0F)) {
         Notifications.getInstance().addList("В этом мире нельзя" + String.valueOf(Formatting.RED) + " прокачивать " + String.valueOf(Formatting.RESET) + "клан", 2500L);
      } else {
         int slotId = PlayerInventoryUtil.getHotbarSlotId((s) -> mc.player.getInventory().getStack(s).isOf(Items.TORCH) || mc.player.getInventory().getStack(s).isOf(Items.REDSTONE));
         if (slotId == -1) {
            if (this.stopWatch.every((double)5000.0F)) {
               Notifications.getInstance().addList("Нужен" + String.valueOf(Formatting.RED) + "факел/редстоун " + String.valueOf(Formatting.RESET) + "в хотбаре", 2500L);
            }

         } else if (mc.player.getInventory().selectedSlot != slotId) {
            mc.player.getInventory().selectedSlot = slotId;
         } else {
            BlockPos pos = mc.player.getBlockPos().down();
            if (mc.world.getBlockState(pos).isSolid()) {
               RotationController controller = RotationController.INSTANCE;
               controller.rotateTo(AngleUtil.pitch(90.0F), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_2, this);
               if (controller.getServerAngle().getPitch() >= 89.0F) {
                  PlayerIntersectionUtil.sendSequencedPacket((in) -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getEntityPos(), Direction.UP, pos, false), in));
                  PlayerIntersectionUtil.sendSequencedPacket((in) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos.up(), Direction.UP, in));
               }
            }

         }
      }
   }
}
