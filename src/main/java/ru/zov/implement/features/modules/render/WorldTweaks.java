package ru.zov.implement.features.modules.render;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.render.FogEvent;

public class WorldTweaks extends Module {
   public final MultiSelectSetting modeSetting = new MultiSelectSetting("World Setting", "Allows you to customize world").value("Bright", "Time", "Fog");
   public final ValueSetting brightSetting = new ValueSetting("Bright", "Sets the value of the maximum bright")
      .setValue(1.0F)
      .range(0.0F, 1.0F)
      .visible(() -> this.modeSetting.isSelected("Bright"));
   public final ValueSetting timeSetting = new ValueSetting("Time", "Sets the value of the time")
      .setValue(12.0F)
      .range(0, 24)
      .visible(() -> this.modeSetting.isSelected("Time"));
   public final ValueSetting distanceSetting = new ValueSetting("Fog Distance", "Sets the value of the time")
      .setValue(100.0F)
      .range(20, 200)
      .visible(() -> this.modeSetting.isSelected("Fog"));

   public static WorldTweaks getInstance() {
      return Instance.get(WorldTweaks.class);
   }

   public WorldTweaks() {
      super("WorldTweaks", "World Tweaks", ModuleCategory.RENDER);
      this.setup(this.modeSetting, this.brightSetting, this.timeSetting, this.distanceSetting);
   }

   @EventHandler
   public void onFog(FogEvent e) {
      if (this.modeSetting.isSelected("Fog")) {
         e.setDistance(this.distanceSetting.getValue());
         e.setColor(ColorUtil.getClientColor());
         e.cancel();
      }
   }
}
