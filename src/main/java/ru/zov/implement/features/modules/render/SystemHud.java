package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.common.util.other.Instance;

public class SystemHud extends Module {
    // Сюда мы будем добавлять галочки для всех элементов нового худа
    public final BooleanSetting useTracker = new BooleanSetting("UseTracker", "Показывать использование предметов").setValue(true);

    public SystemHud() {
        super("SystemHud", "НЕ РАБОТАЕТ!", ModuleCategory.RENDER);
        this.setup(new Setting[]{ useTracker });
    }

    public static SystemHud getInstance() {
        return (SystemHud) Instance.get(SystemHud.class);
    }
}
