package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;

public class Atmosphere extends Module {
   public final SelectSetting modeSetting = new SelectSetting("Mode", "Atmosphere preset")
      .value("Dawn", "Day", "Dusk", "Night", "Dust", "Snow", "Ash");
   public final ValueSetting densitySetting = new ValueSetting("Density", "Density").setValue(1.0F).range(0.0F, 3.0F);
   public final ValueSetting scatterHeightSetting = new ValueSetting("Scatter Height", "Scatter height").setValue(64.0F).range(0.0F, 320.0F);
   public final ValueSetting godRaysSetting = new ValueSetting("God Rays", "God rays strength").setValue(1.0F).range(0.0F, 3.0F);
   public final ValueSetting softnessSetting = new ValueSetting("Softness", "Softness").setValue(1.0F).range(0.0F, 3.0F);
   public final ColorSetting dawnColorSetting = new ColorSetting("Dawn Color", "Dawn color").value(0xFFFFB060);
   public final ValueSetting amountSetting = new ValueSetting("Amount", "Amount").setValue(1.0F).range(0.0F, 3.0F);
   public final ValueSetting speedSetting = new ValueSetting("Speed", "Speed").setValue(1.0F).range(0.0F, 5.0F);
   public final ValueSetting hazeSetting = new ValueSetting("Haze", "Haze").setValue(1.0F).range(0.0F, 3.0F);
   public final ValueSetting gradeSetting = new ValueSetting("Grade", "Color grade").setValue(1.0F).range(0.0F, 3.0F);
   public final ValueSetting vignetteSetting = new ValueSetting("Vignette", "Vignette").setValue(1.0F).range(0.0F, 3.0F);

   public static Atmosphere getInstance() {
      return Instance.get(Atmosphere.class);
   }

   public Atmosphere() {
      super("Atmosphere", "Atmosphere", ModuleCategory.RENDER);
      this.setup(
         this.modeSetting, this.densitySetting, this.scatterHeightSetting, this.godRaysSetting, this.softnessSetting,
         this.dawnColorSetting, this.amountSetting, this.speedSetting, this.hazeSetting, this.gradeSetting, this.vignetteSetting
      );
   }
}
