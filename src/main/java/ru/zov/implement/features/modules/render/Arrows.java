package ru.zov.implement.features.modules.render;

import net.minecraft.client.option.Perspective;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.EntityStory;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.events.render.DrawEvent;

public class Arrows extends Module {
   private final ValueSetting radiusSetting = (new ValueSetting("Radius", "Radius of arrows")).setValue(50.0F).range(30, 100);
   private final ValueSetting sizeSetting = (new ValueSetting("Size", "Size of arrows")).setValue(16.0F).range(8, 20);

   public Arrows() {
      super("Arrows", "Arrows", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.radiusSetting, this.sizeSetting});
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (mc.player != null && !mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) {
         EntityStory.INSTANCE.getEntities().stream().filter((ce) -> ce.entity() != mc.player && ce.entity().isPlayer()).forEach((ent) -> Render2DUtil.drawArrow(ent, this.sizeSetting.getInt(), this.radiusSetting.getInt(), ColorUtil.getClientColor()));
      }

   }
}
