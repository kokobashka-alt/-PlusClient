package ru.zov.common.util.math;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import ru.zov.common.QuickImports;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;

public final class ProjectionUtil implements QuickImports {
   public static Frustum frustum;
   private static final int[] VIEWPORT = new int[4];

   public static @NotNull Vec3d worldToScreen(Vec3d pos) {
      int[] viewport = viewport();
      Vector3f target = new Vector3f();
      // lastWorldSpaceMatrix already contains the inverse camera translation.
      // Feeding camera-relative coordinates here would subtract the camera twice
      // and is what made ESP elements drift far away from their entities.
      Vector4f transformedCoordinates = (new Vector4f((float)pos.x, (float)pos.y, (float)pos.z, 1.0F)).mul(Render3DUtil.lastWorldSpaceMatrix.getPositionMatrix());
      Render3DUtil.lastProjMat.project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3d((double)target.x / mc.getWindow().getScaleFactor(), (double)((float)mc.getWindow().getHeight() - target.y) / mc.getWindow().getScaleFactor(), (double)target.z);
   }

   public static Vector4d getVector4D(Entity ent) {
      if (ent == null) {
         return null;
      }

      Vec3d interpolated = MathUtil.interpolate(ent);
      Box entityBox = ent.getBoundingBox();
      double minX = entityBox.minX - ent.getX() + interpolated.x - 0.1;
      double minY = entityBox.minY - ent.getY() + interpolated.y - 0.1;
      double minZ = entityBox.minZ - ent.getZ() + interpolated.z - 0.1;
      double maxX = entityBox.maxX - ent.getX() + interpolated.x + 0.1;
      double maxY = entityBox.maxY - ent.getY() + interpolated.y + 0.1;
      double maxZ = entityBox.maxZ - ent.getZ() + interpolated.z + 0.1;
      int[] viewport = viewport();
      Vector3f projected = new Vector3f();
      Vector4f transformed = new Vector4f();
      Matrix4f worldMatrix = Render3DUtil.lastWorldSpaceMatrix.getPositionMatrix();
      double scale = mc.getWindow().getScaleFactor();
      double screenHeight = mc.getWindow().getHeight();
      Vector4d position = null;

      for(int corner = 0; corner < 8; ++corner) {
         float x = (float)((corner & 1) == 0 ? minX : maxX);
         float y = (float)((corner & 2) == 0 ? minY : maxY);
         float z = (float)((corner & 4) == 0 ? minZ : maxZ);
         transformed.set(x, y, z, 1.0F).mul(worldMatrix);
         Render3DUtil.lastProjMat.project(transformed.x, transformed.y, transformed.z, viewport, projected);
         if (projected.z > 0.0F && projected.z < 1.0F) {
            double screenX = projected.x / scale;
            double screenY = (screenHeight - projected.y) / scale;
            if (position == null) {
               position = new Vector4d(screenX, screenY, screenX, screenY);
            } else {
               position.x = Math.min(screenX, position.x);
               position.y = Math.min(screenY, position.y);
               position.z = Math.max(screenX, position.z);
               position.w = Math.max(screenY, position.w);
            }
         }
      }

      return position;
   }

   private static int[] viewport() {
      VIEWPORT[2] = mc.getWindow().getFramebufferWidth();
      VIEWPORT[3] = mc.getWindow().getFramebufferHeight();
      return VIEWPORT;
   }

   public static @NotNull Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + pos.x - (double)0.1F, axisAlignedBB2.minY - ent.getY() + pos.y - (double)0.1F, axisAlignedBB2.minZ - ent.getZ() + pos.z - (double)0.1F, axisAlignedBB2.maxX - ent.getX() + pos.x + (double)0.1F, axisAlignedBB2.maxY - ent.getY() + pos.y + (double)0.1F, axisAlignedBB2.maxZ - ent.getZ() + pos.z + (double)0.1F);
      return new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
   }

   public static boolean canSee(Vec3d vec3d) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Angle angle = AngleUtil.fromVec3d(vec3d.subtract(camera.getCameraPos()));
      return Math.abs(MathHelper.wrapDegrees(angle.getYaw() - camera.getYaw())) < 90.0F && Math.abs(MathHelper.wrapDegrees(angle.getPitch() - camera.getPitch())) < 60.0F || canSee(new Box(BlockPos.ofFloored(vec3d)));
   }

   public static boolean canSee(Box box) {
      return box != null && frustum != null && frustum.isVisible(box);
   }

   public static boolean cantSee(Vector4d vec) {
      return vec == null || vec.x < (double)0.0F && vec.z < (double)1.0F || vec.y < (double)0.0F && vec.w < (double)1.0F;
   }

   public static double centerX(Vector4d vec) {
      return vec.x + (vec.z - vec.x) / (double)2.0F;
   }

   private ProjectionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
