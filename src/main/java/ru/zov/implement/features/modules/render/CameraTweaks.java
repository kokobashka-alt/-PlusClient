package ru.zov.implement.features.modules.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.keyboard.HotBarScrollEvent;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.keyboard.MouseRotationEvent;
import ru.zov.implement.events.render.CameraEvent;
import ru.zov.implement.events.render.FovEvent;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;

public class CameraTweaks extends Module {
   private float fov = 110.0F;
   private float smoothFov = 110.0F;
   private float lastChangedFov = 30.0F;
   private Perspective perspective;
   private Angle angle;
   public final ValueSetting ratio = new ValueSetting("Ratio", "Aspect Ratio value setting").setValue(1.0F).range(0.1F, 2.0F);
   public final GroupSetting ratioGroup = new GroupSetting("Aspect Ratio", "Change screen resolution").settings(this.ratio).setValue(true);
   private final BooleanSetting clip = new BooleanSetting("Camera Clip", "The camera passes through the blocks").setValue(true);
   private final ValueSetting distance = new ValueSetting("Camera Distance", "Camera distance value setting").setValue(3.0F).range(2.0F, 5.0F);
   private final BindSetting zoom = new BindSetting("Zoom", "Key to zoom in camera");
   private final BindSetting freeLook = new BindSetting("Free Look", "Key to free look");

   public static CameraTweaks getInstance() {
      return Instance.get(CameraTweaks.class);
   }

   public CameraTweaks() {
      super("CameraTweaks", "Camera Tweaks", ModuleCategory.RENDER);
      this.setup(this.ratioGroup, this.clip, this.distance, this.zoom, this.freeLook);
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.zoom)) {
         this.fov = Math.min(this.lastChangedFov, mc.options.getFov().getValue() - 20);
      }

      if (e.isKeyDown(this.freeLook)) {
         this.perspective = mc.options.getPerspective();
      }

      if (e.isKeyReleased(this.zoom.getKey())) {
         this.lastChangedFov = this.fov;
         this.fov = mc.options.getFov().getValue().intValue();
      }
   }

   @EventHandler
   public void onHotBarScroll(HotBarScrollEvent e) {
      if (PlayerIntersectionUtil.isKey(this.zoom)) {
         this.fov = MathHelper.clamp(MathHelper.floor(this.fov - e.getVertical() * 10.0), 10, mc.options.getFov().getValue());
         e.cancel();
      }
   }

   @EventHandler
   public void onFov(FovEvent e) {
      if (PlayerIntersectionUtil.isKey(this.freeLook)) {
         if (mc.options.getPerspective().isFirstPerson()) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         }
      } else if (this.perspective != null) {
         mc.options.setPerspective(this.perspective);
         this.perspective = null;
      }

      e.setFov(
         MathHelper.clamp(MathHelper.floor(this.smoothFov = MathUtil.interpolateSmooth(1.6, this.smoothFov, this.fov)) + 1, 10, mc.options.getFov().getValue())
      );
   }

   @EventHandler
   public void onMouseRotation(MouseRotationEvent e) {
      if (PlayerIntersectionUtil.isKey(this.freeLook)) {
         this.angle = this.angle.add(e.getCursorDeltaX() * 0.15F, e.getCursorDeltaY() * 0.15F);
         e.cancel();
      } else {
         this.angle = AngleUtil.cameraAngle();
      }
   }

   @EventHandler
   public void onCamera(CameraEvent e) {
      e.setCameraClip(this.clip.isValue());
      e.setDistance(this.distance.getValue());
      e.setAngle(this.angle);
      e.cancel();
   }
}
