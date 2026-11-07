package ru.zov.implement.features.modules.movement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.modules.combat.AutoWeb;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.PointFinder;

public class Flight extends Module {
   private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
   private final Map<Box, Animation> lastPlace = new HashMap<>();
   private final PointFinder pointFinder = new PointFinder();
   private final SelectSetting mode = new SelectSetting("Mode", "Selects the type of mode").value("Grim Blocks").selected("Grim Blocks");

   public Flight() {
      super("Flight", ModuleCategory.MOVEMENT);
      this.setup(this.mode);
   }

   @EventHandler(1)
   @Native
   public void onTick(TickEvent e) {
      AutoWeb autoWeb = AutoWeb.getInstance();
      BlockPos pos = mc.player.getBlockPos().down();
      Vec3d eye = mc.player.getEyePos();
      int version = PlayerIntersectionUtil.getProtocolVersion();
      if (version < 755 || version > 766) {
         Notifications.getInstance().addListIfEmpty("[Flight] Нужна версия " + Formatting.RED + "1.17-1.20.6", 2000L);
      } else if (mc.world.getBlockState(pos).isAir()) {
         PlayerInventoryUtil.hotbarSlotsStream(i -> mc.player.inventory.getStack(i).getItem() instanceof BlockItem)
            .ifPresent(
               slotId -> Arrays.stream(Direction.values())
                  .map(
                     direction -> {
                        BlockPos placed = pos.add(direction.getVector());
                        Vec3d rotate = this.pointFinder.getClosestVec(eye, autoWeb.getBoxPlaced(pos, placed, direction));
                        BlockHitResult hitResult = Box.raycast(List.of(Box.from(Vec3d.ZERO)), eye, rotate, placed);
                        return hitResult != null
                              && direction.equals(hitResult.getSide().getOpposite())
                              && !mc.world.getBlockState(placed).getOutlineShape(mc.world, placed).isEmpty()
                              && eye.distanceTo(rotate) < 4.4F
                           ? hitResult
                           : null;
                     }
                  )
                  .filter(Objects::nonNull)
                  .findFirst()
                  .ifPresentOrElse(hitResult -> {
                     PlayerIntersectionUtil.packetRotate(AngleUtil.calculateAngle(hitResult.getPos()).random(1.0F), 0.1F, 10.0F);
                     this.place(slotId, hitResult);
                     PlayerIntersectionUtil.packetRotate(AngleUtil.cameraAngle().random(0.001F), 0.1F, 10.0F);
                  }, () -> {})
            );
      }
   }

   private void place(int slotId, BlockHitResult hitResult) {
      ItemStack stack = mc.player.inventory.getStack(slotId);
      if (stack.getItem() instanceof BlockItem bi) {
         BlockPos placed = hitResult.getBlockPos().add(hitResult.getSide().getVector());
         PlayerIntersectionUtil.interactBlock(Hand.MAIN_HAND, hitResult);
         this.lastPlace.put(new Box(placed), new DecelerateAnimation().setMs(150).setValue(1.0));
         mc.world.setBlockState(placed, bi.getBlock().getDefaultState());
         stack.setCount(stack.getCount() - 1);
      }
   }

   public List<Packet<?>> getPackets() {
      return this.packets;
   }

   public Map<Box, Animation> getLastPlace() {
      return this.lastPlace;
   }

   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   public SelectSetting getMode() {
      return this.mode;
   }
}
