package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;

public class PouchHud extends Module {

    private static PouchHud instance;

    public static PouchHud getInstance() {
        return instance;
    }

    public final MultiSelectSetting elements = new MultiSelectSetting(
            "PouchHud Elements",
            "Choose which PouchHud elements are visible"
    ).value(
            "PouchWatermark",
            "PouchHotKeys",
            "PouchPotions",
            "PouchStaffList",
            "PouchTargetInfo",
            "PouchArmor",
            "PouchCooldowns"
    );

    public PouchHud() {
        super("PouchHud", ModuleCategory.RENDER);
        instance = this;
        setup(elements);
    }
}
