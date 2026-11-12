package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;

public class CelestialHud extends Module {
   private static CelestialHud instance;

   public final MultiSelectSetting elements = new MultiSelectSetting("CelestialHud Elements", "Какие элементы CelestialHud показывать")
      .value("CelestialWatermark", "CelestialArrayList", "CelestialKeybinds", "CelestialCooldowns", "CelestialPotions", "CelestialStaff", "CelestialTarget")
      .selected("CelestialWatermark", "CelestialArrayList", "CelestialKeybinds", "CelestialCooldowns", "CelestialPotions", "CelestialStaff", "CelestialTarget");
   public final BooleanSetting watermarkLower = new BooleanSetting("Watermark: маленькие буквы", "Писать водяной знак строчными").setValue(false);
   public final ValueSetting watermarkSize = new ValueSetting("Watermark: размер текста", "Размер текста водяного знака").setValue(8.0F).range(6, 14);
   public final BooleanSetting arrayLower = new BooleanSetting("ArrayList: маленькие буквы", "Писать названия модулей строчными").setValue(false);
   public final BooleanSetting arrayGlow = new BooleanSetting("ArrayList: свечение", "Свечение за строками списка").setValue(true);
   public final BooleanSetting targetTest = new BooleanSetting("Target: тестовый режим", "Показывать Target всегда").setValue(false);

   public CelestialHud() {
      super("CelestialHud", ModuleCategory.RENDER);
      instance = this;
      this.setup(this.elements, this.watermarkLower, this.watermarkSize, this.arrayLower, this.arrayGlow, this.targetTest);
   }

   public static CelestialHud getInstance() {
      return instance;
   }
}
