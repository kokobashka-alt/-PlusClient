package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;

public class OldCelestialHud extends Module {
   private static OldCelestialHud instance;

   public final MultiSelectSetting elements = new MultiSelectSetting("OldCelestialHud Elements", "Какие элементы OldCelestial Hud показывать")
      .value("OldCelestialWatermark", "OldCelestialKeybinds", "OldCelestialPotions", "OldCelestialTarget", "OldCelestialInfo", "OldCelestialStats")
      .selected("OldCelestialWatermark", "OldCelestialKeybinds", "OldCelestialPotions", "OldCelestialTarget", "OldCelestialInfo", "OldCelestialStats");
   public final ValueSetting uid = new ValueSetting("UID", "Число в водяном знаке (только отображение)").setValue(4760.0F).range(0, 99999);

   public OldCelestialHud() {
      super("OldCelestialHud", "OldCelestial Hud", ModuleCategory.RENDER);
      instance = this;
      this.setup(this.elements, this.uid);
   }

   public static OldCelestialHud getInstance() {
      return instance;
   }
}
