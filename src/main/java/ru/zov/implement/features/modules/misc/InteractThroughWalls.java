package ru.zov.implement.features.modules.misc;

import java.util.Comparator;
import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.world.BlockFinder;
import ru.zov.implement.events.player.CrosshairTargetEvent;

public class InteractThroughWalls extends Module {
   private final ValueSetting range = (new ValueSetting("Range", "Maximum distance to the block")).setValue(4.5F).range(4.5F, 9.0F);

   public InteractThroughWalls() {
      super("InteractThroughWalls", "Interact Through Walls", ModuleCategory.MISC);
      this.setup(new Setting[]{this.range});
   }

   @EventHandler
   public void onCrosshairTarget(CrosshairTargetEvent e) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      if (camera != null && !((ClientPlayerEntity)Objects.requireNonNull(mc.player)).isSneaking()) {
         Vec3d eye = camera.getCameraPos();
         BlockFinder.INSTANCE.blockEntities.stream().filter((block) -> block.getPos().toCenterPos().distanceTo(eye) < (double)(this.range.getValue() + 2.0F)).map((block) -> block.getCachedState().getCullingShape().raycast(eye, eye.add(mc.player.getRotationVector().multiply((double)this.range.getValue())), block.getPos())).filter(Objects::nonNull).min(Comparator.comparingDouble((block) -> block.getBlockPos().toCenterPos().distanceTo(eye))).ifPresent((result) -> e.setResult(new BlockHitResult(result.getPos(), result.getSide(), result.getBlockPos(), result.isInsideBlock(), true)));
      }

   }
}
