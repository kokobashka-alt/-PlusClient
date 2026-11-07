package ru.zov.common.util.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.RotationController;

public final class PlayerIntersectionUtil implements QuickImports, QuickLogger {
   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
   }

   public static void interactItem(Hand hand) {
      interactItem(hand, false);
   }

   public static void interactItem(Hand hand, boolean swing) {
      interactItem(hand, RotationController.INSTANCE.getRotation(), swing);
   }

   public static void interactItem(Hand hand, Angle angle) {
      interactItem(hand, angle, false);
   }

   public static void interactItem(Hand hand, Angle angle, boolean swing) {
      sendSequencedPacket((i) -> new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch()));
      if (swing) {
         mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
      }

   }

   public static void interactBlock(Hand hand, Vec3d vec3d, Direction direction, BlockPos blockPos) {
      interactBlock(hand, new BlockHitResult(vec3d, direction, blockPos, false));
   }

   public static void interactBlock(Hand hand, BlockHitResult result) {
      sendSequencedPacket((i) -> new PlayerInteractBlockC2SPacket(hand, result, i));
   }

   public static void startFallFlying() {
      mc.player.startGliding();
      mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
   }

   public static void sendPacketWithOutEvent(Packet<?> packet) {
      mc.getNetworkHandler().getConnection().send(packet, null);
   }

   public static void packetRotate(Angle angle, float step, float random) {
      Angle prev = RotationController.INSTANCE.getServerAngle();

      for(float i = step; i < 1.0F; i += step) {
         moveBypass$$$((new Angle(MathHelper.lerp(i, prev.getYaw(), angle.getYaw()), MathHelper.lerp(i, prev.getPitch(), angle.getPitch()))).random(random));
      }

      moveBypass$$$(angle.random(0.001F));
   }

   public static void moveBypass$$$(Angle angle) {
      moveBypass$$$((double)0.0F, angle, mc.player.isOnGround());
   }

   public static void moveBypass$$$(double y, Angle angle, boolean ground) {
      moveBypass$$$(mc.player.getEntityPos().add((double)0.0F, y, (double)0.0F), angle, ground);
   }

   public static void moveBypass$$$(Vec3d vec3d, Angle angle, boolean ground) {
      mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.getX(), vec3d.getY(), vec3d.getZ(), angle.getYaw(), angle.getPitch(), ground, mc.player.horizontalCollision));
      mc.player.setPosition(vec3d);
   }

   public static int getProtocolVersion() {
      if (!main.isViaLoaded()) {
         return 769;
      } else {
         try {
            Class<?> clazz = Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
            Object impl = clazz.getMethod("getImpl").invoke((Object)null);
            Object targetVersion = impl.getClass().getMethod("getTargetVersion").invoke(impl);
            return (Integer)targetVersion.getClass().getMethod("getVersion").invoke(targetVersion);
         } catch (ReflectiveOperationException var3) {
            return 769;
         }
      }
   }

   public static String getHealthString(LivingEntity entity) {
      return getHealthString(getHealth(entity));
   }

   public static String getHealthString(float hp) {
      int tenths = Math.round(hp * 10.0F);
      return tenths % 10 == 0 ? Integer.toString(tenths / 10) : tenths / 10 + "." + Math.abs(tenths % 10);
   }

   public static float getHealth(LivingEntity entity) {
      float hp = entity.getHealth() + entity.getAbsorptionAmount();
      if (entity != mc.player && entity instanceof PlayerEntity) {
         PlayerEntity player = (PlayerEntity)entity;
         switch (ServerUtil.server) {
            case "FunTime":
            case "ReallyWorld":
               ScoreboardObjective scoreBoard = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
               if (scoreBoard != null) {
                  MutableText text2 = ReadableScoreboardScore.getFormattedScore(mc.world.getScoreboard().getScore(player, scoreBoard), scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));

                  try {
                     hp = Float.parseFloat(ColorUtil.removeFormatting(text2.getString()));
                  } catch (NumberFormatException var8) {
                  }
               }
         }
      }

      return MathHelper.clamp(hp, 0.0F, entity.getMaxHealth() + entity.getMaxAbsorption());
   }

   public static void jump() {
      jump(0.2);
   }

   public static void jump(double motion) {
      if (mc.player.isSprinting()) {
         float g = mc.player.getYaw() * ((float)Math.PI / 180F);
         mc.player.addVelocityInternal(new Vec3d((double)(-MathHelper.sin(g)) * motion, (double)0.0F, (double)MathHelper.cos(g) * motion));
      }

      mc.player.velocityDirty = true;
   }

   public static List<BlockPos> getCube(BlockPos center, float radius) {
      return getCube(center, radius, radius, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
      return getCube(center, radiusXZ, radiusY, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
      List<BlockPos> positions = new ArrayList();
      int centerX = center.getX();
      int centerY = center.getY();
      int centerZ = center.getZ();
      int posY = down ? centerY - (int)radiusY : centerY;

      for(int x = centerX - (int)radiusXZ; (float)x <= (float)centerX + radiusXZ; ++x) {
         for(int z = centerZ - (int)radiusXZ; (float)z <= (float)centerZ + radiusXZ; ++z) {
            for(int y = posY; (float)y <= (float)centerY + radiusY; ++y) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
      List<BlockPos> positions = new ArrayList();

      for(int x = start.getX(); x <= end.getX(); ++x) {
         for(int z = start.getZ(); z <= end.getZ(); ++z) {
            for(int y = start.getY(); y <= end.getY(); ++y) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static InputUtil.Type getKeyType(int key) {
      return key < 8 ? Type.MOUSE : Type.KEYSYM;
   }

   public static Stream<Entity> streamEntities() {
      return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
   }

   public static boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
      return mc.player.getEntityWorld().isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
   }

   public static boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
      return mc.player.getActiveStatusEffects().containsKey(statusEffect);
   }

   public static boolean isPlayerInBlock(Block... block) {
      return isBoxInBlock(mc.player.getBoundingBox().expand(-0.02), block);
   }

   public static boolean isBoxInBlock(Box box, Block... blocks) {
      return isBox(box, pos -> {
         Block current = mc.world.getBlockState(pos).getBlock();
         for(Block block : blocks) {
            if (current == block) {
               return true;
            }
         }
         return false;
      });
   }

   public static boolean isBlocks(Box box, Predicate<BlockState> state) {
      return BlockPos.stream(box).map((pos) -> mc.world.getBlockState(pos)).anyMatch(state);
   }

   public static boolean isBox(Box box, Predicate<BlockPos> pos) {
      return BlockPos.stream(box).anyMatch(pos);
   }

   public static boolean isKey(BindSetting setting) {
      int key = setting.getKey();
      return mc.currentScreen == null && setting.isVisible() && isKey(getKeyType(key), key);
   }

   public static boolean isKey(KeyBinding key) {
      return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
   }

   public static boolean isKey(InputUtil.Type type, int keyCode) {
      if (keyCode != -1) {
         switch (type) {
            case KEYSYM -> {
               return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            }
            case MOUSE -> {
               return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
            }
         }
      }

      return false;
   }

   public static boolean isAir(BlockPos blockPos) {
      return isAir(mc.world.getBlockState(blockPos));
   }

   public static boolean isAir(BlockState state) {
      return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
   }

   public static void setAngle(Angle angle) {
      mc.player.setYaw(angle.getYaw());
      mc.player.setPitch(angle.getPitch());
   }

   public static boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.world == null;
   }

   private PlayerIntersectionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
