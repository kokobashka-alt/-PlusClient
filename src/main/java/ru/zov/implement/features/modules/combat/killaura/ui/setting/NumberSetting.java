package ru.zov.implement.features.modules.combat.killaura.ui.setting;

import ru.zov.implement.features.modules.combat.killaura.model.Translation;
import ru.zov.implement.features.modules.combat.killaura.type.SettingUnit;
import java.util.function.Supplier;

public class NumberSetting extends ru.zov.api.feature.module.setting.implement.ValueSetting {
    private float step = 0.05F;
    private SettingUnit valueUnit = SettingUnit.UNITS;

    public NumberSetting(Translation name, Translation description) {
        super(name.effective(), description == null ? "" : description.effective());
    }

    public NumberSetting(Translation name) {
        this(name, null);
    }

    public NumberSetting currentValue(float value) {
        setValue(value);
        return this;
    }

    public float currentValue() {
        return getValue();
    }

    @Override
    public NumberSetting range(float min, float max) {
        super.range(min, max);
        return this;
    }

    public NumberSetting step(float step) {
        this.step = step;
        return this;
    }

    public NumberSetting unit(SettingUnit unit) {
        this.valueUnit = unit;
        return this;
    }

    @Override
    public NumberSetting visible(Supplier<Boolean> visible) {
        super.visible(visible);
        return this;
    }

    public float step() {
        return step;
    }

    public SettingUnit valueUnit() {
        return valueUnit;
    }
}
