package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityRenderDispatcherMixin implements QuickImports {
   @ModifyExpressionValue(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;isInvisible()Z"
      )
   )
   private boolean renderInvisibleHitbox(boolean original, @Local Entity entity) {
      return entity instanceof ArmorStandEntity;
   }

   @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true)
   private void renderCustomHitbox(Entity entity, float tickDelta, boolean serverEntity, CallbackInfo ci) {
      if (!serverEntity && !main.getBoxESPRepository().entities.containsKey(entity.getType())) {
         renderBox(entity);
      }

      ci.cancel();
   }

   private static void renderBox(Entity entity) {
      int color = FriendUtils.isFriend(entity) ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
      Vec3d offset = MathUtil.interpolate(entity).subtract(entity.getEntityPos());
      Box box = entity.getBoundingBox().offset(offset);
      if (!ProjectionUtil.canSee(box)) {
         return;
      }

      if (entity instanceof LivingEntity living) {
         color = ColorUtil.multRed(color, 1 + living.hurtTime);
      }

      int fillColor = ColorUtil.multAlpha(color, 0.1F);
      GizmoDrawing.box(box, DrawStyle.filledAndStroked(color, 2.0F, fillColor));

      if (entity instanceof LivingEntity) {
         float width = entity.getWidth();
         Vec3d eyeMin = entity.getEyePos().add(offset).add(-width / 2.0F, 0.0, -width / 2.0F);
         Vec3d eyeMaxX = eyeMin.add(width, 0.0, 0.0);
         Vec3d eyeMaxZ = eyeMin.add(0.0, 0.0, width);
         Vec3d eyeCorner = eyeMin.add(width, 0.0, width);
         GizmoDrawing.line(eyeMin, eyeMaxX, ColorUtil.RED, 2.0F);
         GizmoDrawing.line(eyeMaxX, eyeCorner, ColorUtil.RED, 2.0F);
         GizmoDrawing.line(eyeMin, eyeMaxZ, ColorUtil.RED, 2.0F);
         GizmoDrawing.line(eyeMaxZ, eyeCorner, ColorUtil.RED, 2.0F);
      }
   }
}
