package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.other.Instance;

/**
 * Флаг для GameRendererMixin. Если модуль включён — renderVignette отменяется.
 */
public class NoVignette extends Module {

    public static NoVignette getInstance() {
        return (NoVignette) Instance.get(NoVignette.class);
    }

    public NoVignette() {
        super("NoVignette", "Убирает затемнение по краям экрана", ModuleCategory.RENDER);
    }
}
