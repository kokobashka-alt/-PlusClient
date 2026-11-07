package ru.zov.implement.features.modules.render;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.implement.events.render.EntityColorEvent;

public class SeeInvisible extends Module {
   private final ValueSetting alphaSetting = new ValueSetting("Alpha", "Player Alpha").setValue(0.5F).range(0.1F, 1.0F);

   public SeeInvisible() {
      super("SeeInvisible", "See Invisible", ModuleCategory.RENDER);
      this.setup(this.alphaSetting);
   }

   @EventHandler
   public void onEntityColor(EntityColorEvent e) {
      e.setColor(ColorUtil.multAlpha(e.getColor(), this.alphaSetting.getValue()));
      e.cancel();
   }
}
