package ru.zov.implement.features.modules.movement;

import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.implement.events.player.MoveEvent;

public class AbilitiesFly extends Module {
   private final ValueSetting speedSetting = new ValueSetting("Speed", "Select fly speed").setValue(2.0F).range(0.5F, 4.0F);

   public AbilitiesFly() {
      super("AbilitiesFly", "Abilities Fly", ModuleCategory.MOVEMENT);
      this.setup(this.speedSetting);
   }

   @EventHandler
   public void onMove(MoveEvent e) {
      if (mc.player != null && mc.player.getAbilities().flying) {
         float speed = this.speedSetting.getValue();
         float y = mc.player.isSneaking() ? -speed : (mc.player.jumping ? speed : 0.0F);
         double[] motion = MovingUtil.calculateDirection(speed);
         e.setMovement(new Vec3d(motion[0], y, motion[1]));
      }
   }
}
