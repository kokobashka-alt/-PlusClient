package ru.zov.common.util.rotation;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ru.zov.common.QuickImports;

public final class RaytracingUtil implements QuickImports {
   public static BlockHitResult raycast(float delta, double range, Angle angle, boolean includeFluids) {
      return raycast(mc.player.getCameraPosVec(delta), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3d vec, double range, Angle angle, boolean includeFluids) {
      return mc.world
         .raycast(
            new RaycastContext(
               vec, vec.add(angle.toVector().multiply(range)), ShapeType.OUTLINE, includeFluids ? FluidHandling.ANY : FluidHandling.NONE, mc.player
            )
         );
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
      return raycast(start, end, shapeType, mc.player);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, Entity entity) {
      return raycast(start, end, shapeType, FluidHandling.NONE, entity);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
      return mc.world.raycast(new RaycastContext(start, end, shapeType, fluidHandling, entity));
   }

   public static EntityHitResult raytraceEntity(double range, Angle angle, Predicate<Entity> filter) {
      Entity entity = mc.player;
      if (entity == null) {
         return null;
      }

      Vec3d cameraVec = entity.getCameraPosVec(1.0F);
      Vec3d rotationVec = angle.toVector();
      Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
      Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0);
      return ProjectileUtil.raycast(entity, cameraVec, vec3d3, box, e -> !e.isSpectator() && filter.test(e), range * range);
   }

   public static boolean rayTrace(double range, Box box) {
      return rayTrace(RotationController.INSTANCE.getRotation().toVector(), range, box);
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
      Vec3d eyePos = Objects.requireNonNull(mc.player).getEyePos();
      return box.contains(eyePos) || box.raycast(eyePos, eyePos.add(clientVec.multiply(range))).isPresent();
   }

   public static boolean can(Vec3d clientVec, double range, Box box) {
      Vec3d eyePos = Objects.requireNonNull(mc.player).getEyePos();
      return box.contains(eyePos) || box.raycast(eyePos, eyePos.add(clientVec.multiply(range))).isPresent();
   }

   private RaytracingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
