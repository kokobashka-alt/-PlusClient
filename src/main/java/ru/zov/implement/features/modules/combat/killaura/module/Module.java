package ru.zov.implement.features.modules.combat.killaura.module;

import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.event.Event;
import ru.zov.implement.features.modules.combat.killaura.event.EventCallback;
import ru.zov.implement.features.modules.combat.killaura.type.EventPriority;
import ru.zov.implement.features.modules.combat.killaura.ui.ModuleTab;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;

public class Module extends ru.zov.api.feature.module.Module {
    public final ModuleTab moduleTab;

    public Module(ModuleTab moduleTab, String name) {
        super(name, mapCategory(moduleTab));
        this.moduleTab = moduleTab;
    }

    public void addSettings(Setting... settings) {
        setup(settings);
    }

    public Module setStateSilent(boolean state) {
        if (state != isState()) {
            toggleSilent(state);
        }
        return this;
    }

    public final <T extends Event> void register(Class<T> type, EventCallback<T> callback) {
        AuraRuntime.INSTANCE.eventDispatcher().register(type, callback);
    }

    public final <T extends Event> void register(Class<T> type, EventCallback<T> callback, EventPriority priority) {
        AuraRuntime.INSTANCE.eventDispatcher().register(type, callback, priority);
    }

    private static ModuleCategory mapCategory(ModuleTab tab) {
        return switch (tab) {
            case COMBAT -> ModuleCategory.COMBAT;
            case MOVEMENT -> ModuleCategory.MOVEMENT;
            case PLAYER -> ModuleCategory.PLAYER;
            case RENDER -> ModuleCategory.RENDER;
            case MISC -> ModuleCategory.MISC;
        };
    }
}
