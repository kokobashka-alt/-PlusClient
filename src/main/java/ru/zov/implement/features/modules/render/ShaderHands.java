package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;

public class ShaderHands extends Module {

    private final SelectSetting mode =
            new SelectSetting("Режим", "Стиль отрисовки рук с шейдерами")
                    .value("Красивый", "С шейдером", "Только шейдер", "Свечение", "Glass");

    private final SelectSetting colorMode =
            new SelectSetting("Режим цвета", "Цветовая схема шейдеров")
                    .value("Интерфейс", "Один цвет", "Радуга", "Два цвета");

    private final SelectSetting outlineEffect =
            new SelectSetting("Эффект обводки", "Тип контура рук")
                    .value("Выкл", "Контур", "Свечение", "Свечение с контуром");

    private final ValueSetting glowRadius =
            new ValueSetting("Радиус свечения", "Дальность размытия свечения")
                    .setValue(4.0f)
                    .range(1.0f, 10.0f)
                    .visible(() -> outlineEffect.isSelected("Свечение") || outlineEffect.isSelected("Свечение с контуром"));

    private final ValueSetting waveSpeed =
            new ValueSetting("Скорость волн", "Скорость анимации шейдерных волн")
                    .setValue(1.2f)
                    .range(0.1f, 5.0f)
                    .visible(() -> mode.isSelected("С шейдером") || mode.isSelected("Красивый"));

    private final ValueSetting strokeWidth =
            new ValueSetting("Ширина обводки", "Толщина контурной линии")
                    .setValue(1.2f)
                    .range(0.1f, 5.0f)
                    .visible(() -> !outlineEffect.isSelected("Выкл"));

    private final BooleanSetting customColor =
            new BooleanSetting("Цвет предмета", "Использовать кастомный цвет для предмета в руках")
                    .setValue(false);

    private final ValueSetting saturation =
            new ValueSetting("Насыщенность", "Насыщенность цветов шейдера")
                    .setValue(1.4f)
                    .range(0.5f, 3.0f);

    private final BooleanSetting trail =
            new BooleanSetting("Шлейф", "Включить шлейф за движением рук")
                    .setValue(true);

    private final ValueSetting alpha =
            new ValueSetting("Прозрачность", "Общая прозрачность текстуры рук")
                    .setValue(1.0f)
                    .range(0.0f, 1.0f);

    public ShaderHands() {
        super("ShaderHands", "Shader Hands", ModuleCategory.RENDER);
        this.setup(new Setting[]{
                mode, colorMode, outlineEffect, glowRadius,
                waveSpeed, strokeWidth, customColor, saturation, trail, alpha
        });
    }

    public static ShaderHands getInstance() {
        return (ShaderHands) Instance.get(ShaderHands.class);
    }

    public boolean isShaderActive() {
        return this.isState() && (mode.isSelected("С шейдером") || mode.isSelected("Красивый"));
    }

    public boolean isGlowActive() {
        return this.isState() && (outlineEffect.isSelected("Свечение") || outlineEffect.isSelected("Свечение с контуром"));
    }

    public boolean isGlassActive() {
        return this.isState() && mode.isSelected("Glass");
    }
}
