package ru.zov.implement.features.modules.combat.killaura.ui.setting;

import ru.zov.implement.features.modules.combat.killaura.model.Translation;
import java.util.function.Supplier;

public class BooleanSetting extends ru.zov.api.feature.module.setting.implement.BooleanSetting {
    public BooleanSetting(Translation name, Translation description) {
        super(name.effective(), description == null ? "" : description.effective());
    }

    public BooleanSetting(Translation name) {
        this(name, null);
    }

    @Override
    public BooleanSetting visible(Supplier<Boolean> visible) {
        super.visible(visible);
        return this;
    }

    @Override
    public BooleanSetting setValue(boolean value) {
        super.setValue(value);
        return this;
    }
}
