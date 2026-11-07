package ru.zov.implement.features.modules.combat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.entity.SimulatedPlayer;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.PointFinder;

public class AutoWeb extends Module {
   private final Map<Box, Animation> lastPlace = new HashMap();
   private final Map<BlockPos, StopWatch> map = new HashMap();
   private final PointFinder pointFinder = new PointFinder();
   private final SelectSetting mode = (new SelectSetting("Mode", "Selects the type of mode")).value("Grim", "Universal").selected("Grim");
   private final BooleanSetting multiPlace;
   private final BindSetting place;

   public static AutoWeb getInstance() {
      return (AutoWeb)Instance.get(AutoWeb.class);
   }

   public AutoWeb() {
      super("AutoWeb", "Auto Web", ModuleCategory.COMBAT);
      this.multiPlace = (new BooleanSetting("Multi Place [" + String.valueOf(Formatting.RED) + "RISK" + String.valueOf(Formatting.RESET) + "]", "Place several blocks in a row at once")).setValue(true).visible(() -> this.mode.isSelected("Grim"));
      this.place = new BindSetting("Place", "Place Key");
      this.setup(new Setting[]{this.mode, this.multiPlace, this.place});
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent e) {
      if (e.type().equals(BlockUpdateEvent.Type.UPDATE)) {
         e.list().forEach((data) -> this.map.remove(data.pos()));
      }

   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.lastPlace.values().removeIf((da) -> {
         if (da.isFinished(Direction.FORWARDS)) {
            da.setDirection(Direction.BACKWARDS);
         }

         return da.isFinished(Direction.BACKWARDS);
      });
      this.lastPlace.forEach((box, anim) -> Render3DUtil.drawBox(box, ColorUtil.multAlpha(ColorUtil.LIGHT_RED, anim.getOutput().floatValue()), 1.5F));
   }

   @EventHandler(0)
   @Native
   public void onTick(TickEvent e) {
      this.map.values().removeIf(StopWatch::finished);
      if (PlayerIntersectionUtil.isKey(this.place)) {
         int version = PlayerIntersectionUtil.getProtocolVersion();
         if (!this.mode.isSelected("Grim") || version >= 755 && version <= 766) {
            PlayerInventoryUtil.slots().filter((s) -> s.getStack().getCount() > 0 && s.getStack().isOf(Items.COBWEB)).findFirst().ifPresent((slot) -> {
               AtomicInteger integer = new AtomicInteger();
               int taskTime = this.mode.isSelected("Grim") ? 0 : PlayerInventoryComponent.getTaskTime();
               Vec3d eye = SimulatedPlayer.simulateLocalPlayer(taskTime).getEyePos();
               ((ClientWorld)Objects.requireNonNull(mc.world)).getPlayers().stream().filter((player) -> !((ClientPlayerEntity)Objects.requireNonNull(mc.player)).equals(player) && !FriendUtils.isFriend((Entity)player)).map((player) -> SimulatedPlayer.simulateOtherPlayer(player, 3 + taskTime)).sorted(Comparator.comparing((player) -> !PlayerIntersectionUtil.isBoxInBlock(player.boundingBox, Blocks.COBWEB))).forEach((player) -> BlockPos.stream(player.boundingBox.expand(-0.01)).filter((pos) -> mc.world.getBlockState(pos).isAir()).forEach((pos) -> Arrays.stream(net.minecraft.util.math.Direction.values()).map((direction) -> {
                        BlockPos placed = pos.add(direction.getVector());
                        Vec3d rotate = this.pointFinder.getClosestVec(eye, this.getBoxPlaced(pos, placed, direction));
                        BlockHitResult hitResult = Box.raycast(List.of(Box.from(Vec3d.ZERO)), eye, rotate, placed);
                        return !this.map.containsKey(placed) && hitResult != null && direction.equals(hitResult.getSide().getOpposite()) && !mc.world.getBlockState(placed).getOutlineShape(mc.world, placed).isEmpty() && eye.distanceTo(rotate) < (double)4.4F ? hitResult : null;
                     }).filter(Objects::nonNull).findFirst().ifPresent((hitResult) -> {
                        int in = integer.get();
                        if (in < (this.multiPlace.isValue() ? 4 : 1) && PlayerInventoryComponent.script.isFinished()) {
                           if (this.mode.isSelected("Grim")) {
                              PlayerIntersectionUtil.packetRotate(AngleUtil.calculateAngle(hitResult.getPos()).random(1.0F), 0.2F, 1.0F);
                              this.place(slot, hitResult);
                              PlayerInventoryUtil.closeScreen(true);
                              PlayerIntersectionUtil.packetRotate(AngleUtil.cameraAngle(), 0.2F, 1.0F);
                           } else {
                              PlayerInventoryComponent.addTask(AngleUtil.fromVec3d(hitResult.getPos().subtract(eye)), () -> this.place(slot, hitResult));
                           }

                           integer.set(in + 1);
                        }

                     })));
            });
         } else {
            Notifications.getInstance().addListIfEmpty("[Auto Web, Grim] Нужна версия " + String.valueOf(Formatting.RED) + "1.17-1.20.6", 2000L);
         }
      }

   }

   public Box getBoxPlaced(BlockPos pos, BlockPos placed, net.minecraft.util.math.Direction direction) {
      return (new Box(pos)).union(new Box(placed)).expand(-Math.max(0.1, (double)Math.abs(direction.getOffsetX())), -Math.max(0.1, (double)Math.abs(direction.getOffsetY())), -Math.max(0.1, (double)Math.abs(direction.getOffsetZ()))).offset(Vec3d.of(direction.getVector()).multiply(1.0E-6));
   }

   @Native
   public void place(Slot slot, BlockHitResult hitResult) {
      Item var4 = slot.getStack().getItem();
      if (var4 instanceof BlockItem bi) {
         BlockPos placed = hitResult.getBlockPos().add(hitResult.getSide().getVector());
         PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
         PlayerIntersectionUtil.interactBlock(Hand.MAIN_HAND, hitResult);
         this.lastPlace.put(new Box(placed), (new DecelerateAnimation()).setMs(150).setValue((double)1.0F));
         mc.world.setBlockState(placed, bi.getBlock().getDefaultState());
         PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
         slot.getStack().setCount(slot.getStack().getCount() - 1);
         this.map.put(placed, (new StopWatch()).setMs(-400));
      }

   }

   public Map<Box, Animation> getLastPlace() {
      return this.lastPlace;
   }

   public Map<BlockPos, StopWatch> getMap() {
      return this.map;
   }

   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   public SelectSetting getMode() {
      return this.mode;
   }

   public BooleanSetting getMultiPlace() {
      return this.multiPlace;
   }

   public BindSetting getPlace() {
      return this.place;
   }
}
