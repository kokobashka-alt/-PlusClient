package ru.zov.implement.features.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Quaternionf;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.EntityStory;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.render.DrawEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.RaytracingUtil;
import ru.zov.common.util.rotation.RotationController;

public class ProjectilePrediction extends Module {
   private final List<Point> points = new ArrayList();

   public static ProjectilePrediction getInstance() {
      return (ProjectilePrediction)Instance.get(ProjectilePrediction.class);
   }

   public ProjectilePrediction() {
      super("ProjectilePrediction", "Projectile Prediction", ModuleCategory.RENDER);
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      DrawContext context = e.context();
      org.joml.Matrix3x2fStack matrix = context.getMatrices();

      for(Point point : this.points) {
         Vec3d vec3d = ProjectionUtil.worldToScreen(point.pos);
         int ticks = point.ticks;
         if (ProjectionUtil.canSee(point.pos)) {
            FontRenderer font = Fonts.getSize(13);
            double time = (double)(ticks * 50) / (double)1000.0F;
            Object[] var10001 = new Object[]{time};
            String text = String.format("%.1f", var10001) + " сек";
            float textWidth = font.getStringWidth(text);
            float posX = (float)(vec3d.getX() + (double)(textWidth / 2.0F) - (double)6.0F) - textWidth;
            float posY = (float)(vec3d.getY() + (double)4.0F);
            float padding = 3.0F;
            float iconSize = 8.0F;
            matrix.pushMatrix();
            matrix.translate(posX, posY);
            blur.render(ShapeProperties.create(matrix, (double)(iconSize + padding), (double)(-padding), (double)(padding + textWidth + padding), (double)10.0F).round(1.5F).color(ColorUtil.HALF_BLACK).build());
            font.drawString(matrix, text, (double)(iconSize + padding * 2.0F), (double)0.5F, -1);
            matrix.translate(0.0F, 0.0F);
            Render2DUtil.defaultDrawStack(context, point.stack, -padding + 2.0F, -padding, true, false, 0.5F);
            matrix.popMatrix();
         }
      }

   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         MatrixStack matrix = e.stack();
         this.points.clear();
         List.of(mc.player.getMainHandStack(), mc.player.getOffHandStack()).forEach((stack) -> this.drawPredictionItem(matrix, stack, RotationController.INSTANCE.getInterpolatedRotation()));
         PlayerIntersectionUtil.streamEntities().filter((ent) -> (ent instanceof PersistentProjectileEntity || ent instanceof ThrownItemEntity || ent instanceof ItemEntity) && !this.visible(ent)).forEach((entity) -> {
            Vec3d motion = entity.getVelocity();
            Vec3d pos = entity.getEntityPos();

            for(int i = 0; i < 300; ++i) {
               Vec3d prevPos = pos;
               pos = pos.add(motion);
               motion = this.calculateMotion(entity, prevPos, motion);
               HitResult result = RaytracingUtil.raycast(prevPos, pos, ShapeType.COLLIDER, entity);
               if (!result.getType().equals(Type.MISS)) {
                  pos = result.getPos();
               }

               Render3DUtil.drawLine(prevPos, pos, ColorUtil.multAlpha(ColorUtil.fade(i), MathHelper.clamp((float)i / 25.0F, 0.0F, 1.0F)), 2.0F, false);
               if (!result.getType().equals(Type.MISS) || pos.y < (double)-128.0F) {
                  this.addPoint(entity, pos, i);
                  break;
               }
            }

         });
         this.points.forEach((point) -> {
            if (!(point.entity instanceof ItemEntity)) {
               ItemStack stack = point.stack;
               Vec3d pos = point.pos;
               if (stack.getItem() instanceof SplashPotionItem) {
                  Render3DUtil.drawRadius(pos, 3.0F, 0.3F, ColorUtil.getClientColor());
               } else if (stack.getItem() instanceof LingeringPotionItem) {
                  Render3DUtil.drawBox(new Box(pos.add((double)-3.0F, 0.01, (double)-3.0F), pos.add((double)3.0F, (double)0.5F, (double)3.0F)), ColorUtil.getClientColor(), 1.0F, true, true, true);
               } else if (this.isSnowballFreeze(stack)) {
                  Render3DUtil.drawRadius(pos, 7.0F, 0.3F, ColorUtil.getClientColor());
               }
            }

         });
      }
   }

   public void drawPredictionItem(MatrixStack matrix, ItemStack stack, Angle angle) {
      this.renderProjectileResults(matrix, this.getTrajectoryResult(stack, angle), stack);
   }

   public List<HitResult> getTrajectoryResult(ItemStack stack, Angle angle) {
      Item activeItem = mc.player.getActiveItem().getItem();
      Item item = stack.getItem();
      Objects.requireNonNull(item);
      if (item instanceof ExperienceBottleItem) {
         return this.checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8, angle);
      } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return this.checkTrajectory(new SplashPotionEntity(mc.world, mc.player, stack), (double)0.5F + (double)(angle.getPitch() / 360.0F), angle);
      } else if (item instanceof TridentItem) {
         if (item.equals(activeItem) && mc.player.getItemUseTime() >= 10) {
            return this.checkTrajectory(new TridentEntity(mc.world, mc.player, stack), (double)2.5F, angle);
         }
      } else if (item instanceof SnowballItem) {
         return this.checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), (double)1.5F, angle);
      } else if (item instanceof EggItem) {
         return this.checkTrajectory(new EggEntity(mc.world, mc.player, stack), (double)1.5F, angle);
      } else if (item instanceof EnderPearlItem) {
         return this.checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), (double)1.5F, angle);
      } else if (item instanceof BowItem) {
         if (item.equals(activeItem) && mc.player.isUsingItem()) {
            return this.checkTrajectory(new ArrowEntity(mc.world, mc.player, stack, stack), (double)(3.0F * MathHelper.clamp(((float)mc.player.getItemUseTime() + tickCounter.getTickProgress(true)) / 20.0F, 0.0F, 1.0F)), angle);
         }
      } else if (item instanceof CrossbowItem) {
         if (!CrossbowItem.isCharged(stack)) {
            return new ArrayList();
         }

         ChargedProjectilesComponent component = (ChargedProjectilesComponent)stack.get(DataComponentTypes.CHARGED_PROJECTILES);
         List<HitResult> list = new ArrayList();
         if (component != null) {
            float velocity = ((ItemStack)component.getProjectiles().getFirst()).isOf(Items.FIREWORK_ROCKET) ? 100.0F : 3.0F;
            list.add(this.checkTrajectory(angle.toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), (double)velocity));
            if (component.getProjectiles().size() > 2) {
               float pitchAbs = angle.getPitch() / 90.0F;
               float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
               float yaw = (float)MathHelper.lerp(Math.abs(delta), 10, 90);
               float pitch = (float)MathHelper.lerp(delta, 0, 10);
               list.add(this.checkTrajectory(angle.addYaw(-yaw).addPitch(-pitch).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), (double)velocity));
               list.add(this.checkTrajectory(angle.addYaw(yaw).addPitch(-pitch).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), (double)velocity));
            }
         }

         return list;
      }

      return new ArrayList();
   }

   public void renderProjectileResults(MatrixStack matrix, List<HitResult> results, ItemStack stack) {
      for(HitResult result : results) {
         if (stack.getItem() instanceof SplashPotionItem) {
            Render3DUtil.drawRadius(result.getPos(), 3.0F, 0.3F, ColorUtil.getClientColor());
         } else if (this.isSnowballFreeze(stack)) {
            Render3DUtil.drawRadius(result.getPos(), 7.0F, 0.3F, ColorUtil.getClientColor());
         } else if (stack.getItem() instanceof LingeringPotionItem) {
            Render3DUtil.drawBox(new Box(result.getPos().add((double)-3.0F, 0.01, (double)-3.0F), result.getPos().add((double)3.0F, (double)0.5F, (double)3.0F)), ColorUtil.getClientColor(), 1.0F, true, true, true);
         } else {
            Direction direction;
            int var10000;
            label41: {
               direction = this.getDirection(result);
               if (result instanceof BlockHitResult) {
                  BlockHitResult blockHitResult = (BlockHitResult)result;
                  if (blockHitResult.getBlockPos().equals(BlockPos.ORIGIN)) {
                     var10000 = Color.RED.getRGB();
                     break label41;
                  }
               }

               var10000 = ColorUtil.getClientColor();
            }

            int color = var10000;
            double width = 0.3;
            Quaternionf var15;
            switch (direction) {
               case WEST:
               case EAST:
                  var15 = RotationAxis.POSITIVE_Z.rotationDegrees(90.0F);
                  break;
               case SOUTH:
               case NORTH:
                  var15 = RotationAxis.POSITIVE_X.rotationDegrees(90.0F);
                  break;
               default:
                  var15 = new Quaternionf();
            }

            Quaternionf quaternionf = var15;
            matrix.push();
            matrix.translate(result.getPos().subtract(mc.getEntityRenderDispatcher().camera.getCameraPos()));
            matrix.multiply(quaternionf);
            MatrixStack.Entry entry = matrix.peek().copy();
            matrix.pop();
            int i = 0;

            for(int size = 90; i <= size; ++i) {
               Render3DUtil.drawLine(entry, MathUtil.cosSin(i, size, width), MathUtil.cosSin(i + 1, size, width), color, color, 1.25F, false);
            }

            Render3DUtil.drawLine(entry, new Vec3d((double)0.0F, (double)0.0F, -width / (double)2.0F), new Vec3d((double)0.0F, (double)0.0F, width / (double)2.0F), color, color, 1.25F, false);
            Render3DUtil.drawLine(entry, new Vec3d(-width / (double)2.0F, (double)0.0F, (double)0.0F), new Vec3d(width / (double)2.0F, (double)0.0F, (double)0.0F), color, color, 1.25F, false);
         }
      }

   }

   public List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity, Angle angle) {
      return List.of(this.checkTrajectory(angle.toVector(), entity, velocity, true));
   }

   public HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
      return this.checkTrajectory(lookVec, entity, velocity, true);
   }

   public HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity, boolean velo) {
      return this.checkTrajectory(mc.player.getEyePos(), lookVec, entity, velocity, velo);
   }

   public HitResult checkTrajectory(Vec3d eye, Vec3d lookVec, ProjectileEntity entity, double velocity, boolean velo) {
      Vec3d look = lookVec.multiply(velocity / (double)MathHelper.sqrt(lookVec.toVector3f().lengthSquared()));
      if (velo) {
         look = look.add(this.getMotion(entity));
         eye = eye.add(MathUtil.interpolate(mc.player).subtract(mc.player.getEntityPos()));
      }

      return this.traceTrajectory(eye, look, entity);
   }

   public HitResult calcTrajectory(ProjectileEntity e) {
      return this.traceTrajectory(e.getEntityPos(), e.getVelocity(), e);
   }

   public HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
      Box entityBox = entity.getBoundingBox().expand(0.201);
      List<Box> boxes = PlayerIntersectionUtil.streamEntities().filter((ent) -> {
         boolean var10000;
         if (ent instanceof LivingEntity living) {
            if (living != entity.getOwner() && living.isAlive()) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }).map(Entity::getBoundingBox).toList();
      int i = 0;

      while(true) {
         if (i < 300) {
            Vec3d prevPos = pos;
            pos = pos.add(motion);
            motion = this.calculateMotion(entity, prevPos, motion);
            BlockHitResult result = RaytracingUtil.raycast(prevPos, pos, ShapeType.COLLIDER, entity);
            Box finalEntityBox = entityBox;
            Vec3d finalMotion = motion;
            Box motionBox = (Box)IntStream.rangeClosed(1, 4).mapToObj((i2) -> finalEntityBox.offset(finalMotion.multiply((double)0.25F * (double)i2))).filter((box) -> boxes.stream().anyMatch((box2) -> box2.intersects(box))).findAny().orElse(null);
            if (motionBox != null) {
               return new BlockHitResult(motionBox.getCenter(), result.getSide(), BlockPos.ORIGIN, result.isInsideBlock());
            }

            if (!result.getType().equals(Type.MISS)) {
               return result;
            }

            entityBox = entityBox.offset(motion);
            if (!(pos.y < (double)-128.0F)) {
               ++i;
               continue;
            }
         }

         return new BlockHitResult(BlockPos.ORIGIN.toCenterPos(), Direction.DOWN, BlockPos.ORIGIN.down(999), true);
      }
   }

   public Vec3d calculateMotion(Entity entity, Vec3d prev, Vec3d motion) {
      boolean isInWater = mc.world.getBlockState(BlockPos.ofFloored(prev)).getFluidState().isIn(FluidTags.WATER);
      Objects.requireNonNull(entity);
      double multiply;
      if (entity instanceof TridentEntity) {
         multiply = 0.99;
      } else if (entity instanceof PersistentProjectileEntity && isInWater) {
         multiply = 0.6;
      } else {
         multiply = isInWater ? 0.8 : 0.99;
      }

      return motion.add((double)0.0F, -entity.getFinalGravity(), (double)0.0F).multiply(multiply);
   }

   public Vec3d getMotion(ProjectileEntity entity) {
      int version = PlayerIntersectionUtil.getProtocolVersion();
      if (entity instanceof ArrowEntity arrow) {
         if (arrow.getItemStack().isOf(Items.CROSSBOW)) {
            return Vec3d.ZERO;
         }
      }

      if (version <= 754 || version >= 767) {
         Vec3d[] objects = EntityStory.INSTANCE.getEntities(mc.player, 2).map(Entity::getEntityPos).toList().toArray(Vec3d[]::new);
         return MathUtil.interpolate(objects[1].subtract(objects[0]), objects[2].subtract(objects[1]));
      } else {
         return Vec3d.ZERO;
      }
   }

   private void addPoint(Entity entity, Vec3d pos, int ticks) {
      Objects.requireNonNull(entity);
      if (entity instanceof ItemEntity item) {
         this.points.add(new Point(entity, item.getStack(), pos, ticks));
      } else if (entity instanceof ThrownItemEntity thrown) {
         this.points.add(new Point(entity, thrown.getStack(), pos, ticks));
      } else if (entity instanceof PersistentProjectileEntity persistent) {
         this.points.add(new Point(entity, persistent.getItemStack(), pos, ticks));
      }
   }

   public Direction getDirection(HitResult result) {
      if (result instanceof BlockHitResult blockHitResult) {
         if (!blockHitResult.getBlockPos().equals(BlockPos.ORIGIN)) {
            return blockHitResult.getSide();
         }
      }

      return Direction.getFacing(result.getPos().subtract(mc.player.getEyePos()).normalize());
   }

   private boolean visible(Entity entity) {
      boolean posChange = entity.getX() == entity.lastX && entity.getY() == entity.lastY && entity.getZ() == entity.lastZ;
      boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || PlayerIntersectionUtil.isBoxInBlock(entity.getBoundingBox().expand((double)2.0F), Blocks.WATER));
      return posChange || itemEntityCheck;
   }

   private boolean isSnowballFreeze(ItemStack stack) {
      return stack.getItem() instanceof SnowballItem && ServerUtil.isCopyTime() && !((NbtComponent)stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).copyNbt().getString("don-item").isEmpty();
   }

   private static record Point(Entity entity, ItemStack stack, Vec3d pos, int ticks) {
   }
}
