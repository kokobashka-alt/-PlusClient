package ru.zov.implement.features.modules.combat.killaura.module;

import ru.zov.implement.features.modules.combat.killaura.ui.ModuleTab;
import ru.zov.implement.features.modules.combat.AutoTotem;

public final class AutoTotemModule extends Module {
    public static final AutoTotemModule INSTANCE = new AutoTotemModule();

    private AutoTotemModule() {
        super(ModuleTab.COMBAT, "Auto Totem Bridge");
    }

    private AutoTotem delegate() {
        if (main.getModuleRepository() == null) return null;
        return main.getModuleRepository().modules().stream()
                .filter(AutoTotem.class::isInstance)
                .map(AutoTotem.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean isState() {
        AutoTotem delegate = delegate();
        return delegate != null && delegate.isState();
    }

    public boolean trigger() {
        AutoTotem delegate = delegate();
        return delegate != null && delegate.trigger();
    }
}
