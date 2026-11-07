package ru.zov.implement.features.modules.combat.killaura.module;

import ru.zov.implement.features.modules.combat.killaura.ui.ModuleTab;

public final class AutoJumpModule extends Module {
    public static final AutoJumpModule INSTANCE = new AutoJumpModule();

    private AutoJumpModule() {
        super(ModuleTab.MOVEMENT, "Auto Jump");
    }
}
