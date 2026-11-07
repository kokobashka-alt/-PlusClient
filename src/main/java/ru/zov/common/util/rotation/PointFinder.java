package ru.zov.common.util.rotation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.common.QuickImports;

public class PointFinder implements QuickImports {
   public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      return this.computeVector(new GroupSetting("", ""), entity, maxDistance, ignoreWalls);
   }

   public Pair<Vec3d, Box> computeVector(GroupSetting boxGroup, LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Vec3d eye = mc.player.getEyePos();
      Pair<List<Vec3d>, Box> candidatePoints = this.generateCandidatePoints(boxGroup, eye, entity, maxDistance, ignoreWalls);
      return new Pair<>(this.findBestVector(eye, entity, candidatePoints.getLeft()), candidatePoints.getRight());
   }

   public Pair<List<Vec3d>, Box> generateCandidatePoints(GroupSetting boxGroup, Vec3d eye, LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box entityBox = this.getExtendedHitbox(boxGroup, entity);
      Box box = entityBox.expand(-0.03);
      Vec3d step = box.getMaxPos().subtract(box.getMinPos()).multiply(0.1);
      List<Vec3d> list = Stream.<Double>iterate(box.minX, x -> x <= box.maxX, x -> x + step.x)
         .flatMap(
            x -> Stream.<Double>iterate(box.minY, y -> y <= box.maxY, y -> y + step.y)
               .flatMap(y -> Stream.<Double>iterate(box.minZ, z -> z <= box.maxZ, z -> z + step.z).map(z -> new Vec3d(x, y, z)))
         )
         .filter(point -> this.isValidPoint(eye, point, maxDistance, ignoreWalls))
         .toList();
      return new Pair<>(list, entityBox);
   }

   public boolean hasValidPoint(GroupSetting boxGroup, Vec3d eye, LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      return this.isValidPoint(eye, this.getClosestVec(eye, this.getExtendedHitbox(boxGroup, entity).expand(-0.03)), maxDistance, ignoreWalls);
   }

   private boolean isValidPoint(Vec3d start, Vec3d end, float maxDistance, boolean ignoreWalls) {
      return start.distanceTo(end) <= maxDistance && (ignoreWalls || !RaytracingUtil.raycast(start, end, ShapeType.COLLIDER).getType().equals(Type.BLOCK));
   }

   private Vec3d findBestVector(Vec3d eye, Entity entity, List<Vec3d> candidatePoints) {
      return candidatePoints.stream().min(Comparator.comparing(eye::distanceTo)).orElse(entity.getEyePos());
   }

   private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Angle initialAngle) {
      Angle targetAngle = AngleUtil.fromVec3d(endPoint.subtract(startPoint));
      Angle delta = AngleUtil.calculateDelta(initialAngle, targetAngle);
      return Math.hypot(delta.getYaw(), delta.getPitch());
   }

   private Box getExtendedHitbox(GroupSetting boxGroup, Entity entity) {
      return this.getExtendedHitbox(boxGroup, entity.getBoundingBox(), entity);
   }

   private Box getExtendedHitbox(GroupSetting boxGroup, Box box, Entity entity) {
      return boxGroup.isValue() ? this.getGrimExtendedBox(box, entity.getDimensions(EntityPose.STANDING).height()) : box;
   }

   public Box getGrimExtendedBox(Box box, double height) {
      double lengthY = Objects.requireNonNull(mc.player).getBoundingBox().getLengthY();
      return box.withMaxY(box.minY + height + MathHelper.clamp(lengthY - 0.601, 0.0, 2.0)).withMinY(box.minY + MathHelper.clamp(lengthY - 1.799, -2.0, 0.0));
   }

   public double getClosestDistance(Vec3d eye, Box box) {
      return eye.distanceTo(this.getClosestVec(eye, box));
   }

   public Vec3d getClosestVec(Vec3d eye, Box box) {
      return new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX), MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ));
   }
}
