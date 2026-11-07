package ru.zov.api.repository;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.SimulatedPlayer;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.StringUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.DrawEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.modules.combat.AutoArmor;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.PointFinder;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.features.modules.player.AutoUse;
import ru.zov.implement.features.modules.render.Hud;

public class AutoFarmRepository implements QuickImports, QuickLogger {
   public final StopWatch stopWatch = new StopWatch();
   public final StopWatch workWatch = new StopWatch();
   private final Map<BlockPos, BlockState> netherWarts = new ConcurrentHashMap();
   private final List<BlockPos> readyWarts = new ArrayList();
   private final PointFinder pointFinder = new PointFinder();
   public final List<BlockPos> points = new ArrayList();
   public boolean run;
   public boolean start;
   public boolean repair;
   public boolean updated;
   public int point;
   public int count;

   public AutoFarmRepository(EventManager eventManager) {
      eventManager.register(this);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.player != null && this.points.size() > 1 && ServerUtil.isHolyWorld()) {
         Render3DUtil.drawBox(mc.player.getBoundingBox(EntityPose.STANDING).offset(((BlockPos)this.points.get(this.point)).toCenterPos().add((double)0.0F, (double)-0.5F, (double)0.0F)), ColorUtil.getClientColor(), 1.5F);
         IntStream.range(0, this.points.size() - 1).forEach((i) -> Render3DUtil.drawLine(((BlockPos)this.points.get(i)).toCenterPos().add((double)0.0F, -0.4, (double)0.0F), ((BlockPos)this.points.get(i + 1)).toCenterPos().add((double)0.0F, -0.4, (double)0.0F), ColorUtil.getClientColor(), 2.0F, false));
      }

   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (!this.points.isEmpty() && ((BlockPos)this.points.getFirst()).toCenterPos().distanceTo(mc.player.getEntityPos()) < (double)150.0F) {
         MathUtil.multAlpha((float)this.workWatch.elapsedInt() / 500.0F, () -> {
            int multiply = Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Boss Bars") ? 22 : 19;
            int x = mc.getWindow().getScaledWidth() / 2;
            int y = mc.inGameHud.getBossBarHud().bossBars.size() * multiply + 10;
            org.joml.Matrix3x2fStack matrix = e.context().getMatrices();
            FontRenderer font = Fonts.getSize(15);
            String var10002 = String.valueOf(Formatting.RED);
            font.drawCenteredString(matrix, "Незер нароста готово к сбору: " + var10002 + this.readyWarts.size() + String.valueOf(Formatting.RESET) + "/" + String.valueOf(Formatting.RED) + this.netherWarts.size(), (double)x, (double)y, ColorUtil.getText());
            font.drawCenteredString(matrix, "Собрано нароста за все время: " + String.valueOf(Formatting.RED) + this.count, (double)x, (double)(y + 9), ColorUtil.getText());
            font.drawCenteredString(matrix, "Время работы: " + String.valueOf(Formatting.RED) + StringUtil.getDurationMS(this.workWatch.elapsedInt() / 100, true), (double)x, (double)(y + 18), ColorUtil.getText());
         });
      }

   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof ScreenHandlerSlotUpdateS2CPacket update) {
         if (update.getSlot() == 44 && update.getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
            this.stopWatch.reset();
            this.updated = true;
         }
      } else if (var2 instanceof OpenScreenS2CPacket) {
         this.stopWatch.reset();
         this.updated = true;
      }
   }

   @EventHandler(4)
   public void onInput(InputEvent e) {
      if (this.run) {
         e.setForward(true);
         e.setSprint(true);
      }

   }

   @EventHandler(1)
   @Native
   public void onTick(TickEvent e) {
      this.run = false;
      Slot hoe = PlayerInventoryUtil.getSlot((Predicate<Slot>)((s) -> s.getStack().getItem() instanceof HoeItem && ItemUtil.getEnchantmentLvl(s.getStack(), Enchantments.MENDING) != 0));
      RotationController controller = RotationController.INSTANCE;
      int protocol = PlayerIntersectionUtil.getProtocolVersion();
      int windowId = mc.player.currentScreenHandler.syncId;
      AutoArmor autoArmor = AutoArmor.getInstance();
      if (this.start && ServerUtil.isHolyWorld()) {
         if (this.points.size() < 2) {
            Notifications.getInstance().addListIfEmpty("[Auto Farm] Нужно указать как минимум " + String.valueOf(Formatting.RED) + "2" + String.valueOf(Formatting.RESET) + " позиции", 3000L);
         } else if (protocol >= 755 && protocol <= 766) {
            if (hoe == null) {
               Notifications.getInstance().addListIfEmpty("[Auto Farm] Нужна " + String.valueOf(Formatting.RED) + "мотыга" + String.valueOf(Formatting.RESET) + " на починку с посевом", 3000L);
            } else {
               ItemStack itemStack = hoe.getStack();
               if (PlayerInventoryUtil.mainHandSlot() != hoe) {
                  PlayerInventoryUtil.swapHand(hoe, Hand.MAIN_HAND, true);
               } else {
                  this.repair = autoArmor.isBroken(itemStack) || !autoArmor.isFully(itemStack) && this.repair;
                  if (AutoUse.getInstance().script.isFinished() && this.repair) {
                     PlayerInventoryUtil.getSlots().stream().filter((s) -> !PlayerInventoryUtil.isServerScreen() && !ItemUtil.hasCooldown(s.getStack()) && s.getStack().isOf(Items.EXPERIENCE_BOTTLE) && ((NbtComponent)s.getStack().getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).toString().contains("полностью ремонтирует")).findFirst().ifPresentOrElse((s) -> PlayerInventoryUtil.swapAndUse(s, controller.getRotation(), Hand.OFF_HAND), () -> {
                        if (!PlayerInventoryUtil.offHandSlot().getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                           Slot exp = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE, (Predicate<Slot>)((s) -> !PlayerInventoryUtil.isServerScreen() || s.id > 54));
                           if (exp == null) {
                              if (PlayerInventoryUtil.getSlots().stream().allMatch((s) -> s.id >= 44 || List.of(Items.EXPERIENCE_BOTTLE, Items.BARRIER).contains(s.getStack().getItem())) && !this.stopWatch.finished((double)5000.0F)) {
                                 if (this.updated && this.stopWatch.every((double)300.0F)) {
                                    if (!((NbtComponent)((Slot)PlayerInventoryUtil.getSlots().get(52)).getStack().getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).toString().contains("\"italic\":false,\"color\":\"gold\",\"text\":\"Сначала дешевые за ед. товара\"")) {
                                       PlayerInventoryUtil.clickSlot(windowId, 52, 0, SlotActionType.PICKUP, true);
                                    } else {
                                       List<Slot> buy = new ArrayList();

                                       for(Slot slot : PlayerInventoryUtil.getSlots()) {
                                          if (slot.id <= 44 && !buy.contains(slot) && slot.getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                                             if (!buy.isEmpty()) {
                                                break;
                                             }

                                             mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(windowId, 0, (short)slot.id, (byte)0, SlotActionType.QUICK_MOVE, Int2ObjectMaps.emptyMap(), ItemStackHash.EMPTY));
                                             mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(windowId + 1, 0, (short)0, (byte)0, SlotActionType.QUICK_MOVE, Int2ObjectMaps.emptyMap(), ItemStackHash.EMPTY));
                                             buy.add(slot);
                                          }
                                       }

                                       if (buy.isEmpty()) {
                                          PlayerInventoryUtil.clickSlot(windowId, 47, 0, SlotActionType.PICKUP, true);
                                       }
                                    }

                                    this.updated = false;
                                 }
                              } else if (this.stopWatch.every((double)1100.0F)) {
                                 mc.player.networkHandler.sendChatCommand("ah search опыт");
                              }
                           } else if (PlayerInventoryUtil.isServerScreen() && this.stopWatch.finished((double)200.0F)) {
                              PlayerInventoryUtil.closeScreen(false);
                           } else if (this.stopWatch.every((double)300.0F)) {
                              PlayerInventoryUtil.swapHand(exp, Hand.OFF_HAND, true, true);
                           }
                        } else {
                           if (controller.getServerAngle().getPitch() != 90.0F) {
                              PlayerIntersectionUtil.moveBypass$$$(AngleUtil.pitch(90.0F));
                           }

                           PlayerIntersectionUtil.interactItem(Hand.OFF_HAND, AngleUtil.pitch(90.0F));
                        }

                     });
                  } else if (PlayerInventoryUtil.isServerScreen()) {
                     if (!this.repair) {
                        PlayerInventoryUtil.closeScreen(false);
                     }

                  } else {
                     BlockPos blockPos = BlockPos.ofFloored(SimulatedPlayer.simulateLocalPlayer(3).pos);
                     BlockPos pointBlock = (BlockPos)this.points.get(this.point);
                     if (pointBlock.equals(blockPos) || pointBlock.down().equals(blockPos)) {
                        ++this.point;
                        if (this.point >= this.points.size()) {
                           this.point = 0;
                        }
                     }

                     controller.clear();
                     controller.rotateTo(AngleUtil.calculateAngle(pointBlock.toCenterPos()), new RotationConfig(true, false), TaskPriority.HIGH_IMPORTANCE_2, (new Module("AutoFarm", ModuleCategory.PLAYER)).silentEnable());
                     this.run = true;
                     Vec3d eye = mc.player.getEyePos();
                     List<BlockPos> warts = new ArrayList();
                     this.readyWarts.stream().filter((pos) -> this.pointFinder.getClosestDistance(eye, this.getBox(pos)) < (double)4.5F).forEach((pos) -> {
                        if (warts.isEmpty()) {
                           Vec3d vec3d = this.pointFinder.getClosestVec(eye, this.getBox(pos).expand(-0.001));
                           BlockHitResult hitResult = Box.raycast(List.of(Box.from(Vec3d.ZERO)), eye, vec3d, pos);
                           if (hitResult != null) {
                              this.rotate(AngleUtil.calculateAngle(vec3d), 0.2F);
                              PlayerIntersectionUtil.sendSequencedPacket((i) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, hitResult.getSide(), i));
                              mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                              warts.add(pos);
                           }
                        }

                     });
                     if (!warts.isEmpty()) {
                        this.count += warts.size();
                        List var10001 = this.readyWarts;
                        Objects.requireNonNull(var10001);
                        warts.forEach(var10001::remove);
                        this.rotate(controller.getRotation(), 0.2F);
                     }

                  }
               }
            }
         } else {
            Notifications.getInstance().addListIfEmpty("[Auto Farm] Нужна версия " + String.valueOf(Formatting.RED) + "1.17-1.20.6", 3000L);
         }
      }
   }

   @EventHandler
   @Native
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.netherWarts.clear();
      this.readyWarts.clear();
   }

   public void onBlockUpdate(BlockUpdateEvent e) {
      switch (e.type()) {
         case LOAD:
         case UPDATE:
            e.list().forEach(this::put);
            break;
         case UNLOAD:
            e.list().forEach((data) -> {
               this.netherWarts.remove(data.pos());
               this.readyWarts.remove(data.pos());
            });
      }
      if (e.type() == BlockUpdateEvent.Type.UNLOAD && e.chunkPos() != null) {
         this.netherWarts.keySet().removeIf(pos -> pos.getX() >> 4 == e.chunkPos().x && pos.getZ() >> 4 == e.chunkPos().z);
         this.readyWarts.removeIf(pos -> pos.getX() >> 4 == e.chunkPos().x && pos.getZ() >> 4 == e.chunkPos().z);
      }

   }

   private void put(BlockUpdateEvent.BlockData data) {
      BlockState state = data.state();
      Block block = state.getBlock();
      BlockPos pos = data.pos();
      if (block.equals(Blocks.NETHER_WART)) {
         if ((Integer)state.get(NetherWartBlock.AGE) >= 3) {
            this.readyWarts.add(pos);
         }

         this.netherWarts.put(pos, state);
      } else {
         this.netherWarts.remove(pos);
         this.readyWarts.remove(pos);
      }

   }

   private Box getBox(BlockPos pos) {
      return this.pointFinder.getGrimExtendedBox(new Box(pos), 0.7);
   }

   private void rotate(Angle angle, float f) {
      Angle prev = RotationController.INSTANCE.getServerAngle();

      for(float i = f; i < 1.0F; i += f) {
         PlayerIntersectionUtil.moveBypass$$$((new Angle(MathHelper.lerp(i, prev.getYaw(), angle.getYaw()), MathHelper.lerp(i, prev.getPitch(), angle.getPitch()))).random(6.0F));
      }

      PlayerIntersectionUtil.moveBypass$$$(angle.random(1.0E-4F));
   }
}
