package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.common.util.other.Instance;

/**
 * Только флаги. Сами проверки — в миксинах:
 *   - Fire / InBlockOverlay  → InGameHudMixin (renderFireOverlay / renderBlockOverlay)
 *   - Particles              → ParticleManagerMixin
 *   - Weather                → WorldRendererMixin (renderWeather)
 *   - Clouds                 → WorldRendererMixin (renderClouds)
 *   - BlockEntities          → WorldRendererMixin (renderBlockEntities)
 *   - Shadows                → GameRendererMixin / WorldRendererMixin
 *   - TotemAnimation         → InGameHudMixin (renderOverlay)
 *   - Nausea (тряска)        → GameRendererMixin (renderNausea)
 */
public class NoRender extends Module {

    public static NoRender getInstance() {
        return (NoRender) Instance.get(NoRender.class);
    }

    public final MultiSelectSetting elements = new MultiSelectSetting(
            "Элементы", "Что отключать")
            .value("Огонь", "Плохие эффекты", "Оверлей в блоке", "Частицы",
                    "Погода", "Облака", "Блок-сущности", "Тени",
                    "Анимацию тотема", "Тряска")
            .selected();

    public NoRender() {
        super("NoRender", "Отключение рендера отдельных элементов", ModuleCategory.RENDER);
        this.setup(new Setting[]{elements});
    }

    public boolean isEnabled(String element) {
        return isState() && elements.isSelected(element);
    }

    public boolean isTotemAnimationDisabled() {
        return isEnabled("Анимацию тотема");
    }
}
