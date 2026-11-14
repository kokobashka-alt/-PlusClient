package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.common.util.other.Instance;

/**
 * Управление отключением рендера. Все проверки в миксинах идут через
 * modeSetting.isSelected("...") с АНГЛИЙСКИМИ ключами:
 *
 *  - "Fire"              → InGameOverlayRendererMixin.renderFireOverlay
 *  - "Block Overlay"     → InGameOverlayRendererMixin.renderInWallOverlay
 *  - "Particles"         → ParticleManagerMixin.addParticle
 *  - "Weather"           → WorldRendererMixin.renderWeather
 *  - "Clouds"            → WorldRendererMixin.renderClouds
 *  - "Block Entities"    → WorldRendererMixin.renderBlockEntities
 *  - "Shadows"           → EntityRenderDispatcherMixin.getShadowRadius
 *  - "Totem Animation"   → HeldItemRendererMixin / GameRendererMixin
 *  - "Nausea"            → GameRendererMixin.renderNausea
 *  - "Bad Effects"       → InGameHudMixin.renderStatusEffectOverlay
 *  - "Warden Darkness"   → GameRendererMixin.renderDarkness
 */
public class NoRender extends Module {

    public static NoRender getInstance() {
        return (NoRender) Instance.get(NoRender.class);
    }

    /** Имя поля — modeSetting, потому что миксин InGameOverlayRendererMixin уже использует его. */
    public final MultiSelectSetting modeSetting = new MultiSelectSetting(
            "Элементы", "Что отключать")
            .value(
                    "Fire",
                    "Block Overlay",
                    "Particles",
                    "Weather",
                    "Clouds",
                    "Block Entities",
                    "Shadows",
                    "Totem Animation",
                    "Nausea",
                    "Bad Effects",
                    "Warden Darkness"
            )
            .selected();

    public NoRender() {
        super("NoRender", "Отключение рендера отдельных элементов", ModuleCategory.RENDER);
        this.setup(new Setting[]{modeSetting});
    }

    public boolean isEnabled(String element) {
        return isState() && modeSetting.isSelected(element);
    }

    // Удобные геттеры для миксинов:
    public boolean isFireDisabled()           { return isEnabled("Fire"); }
    public boolean isBlockOverlayDisabled()   { return isEnabled("Block Overlay"); }
    public boolean isParticlesDisabled()      { return isEnabled("Particles"); }
    public boolean isWeatherDisabled()        { return isEnabled("Weather"); }
    public boolean isCloudsDisabled()         { return isEnabled("Clouds"); }
    public boolean isBlockEntitiesDisabled()  { return isEnabled("Block Entities"); }
    public boolean isShadowsDisabled()        { return isEnabled("Shadows"); }
    public boolean isTotemAnimationDisabled() { return isEnabled("Totem Animation"); }
    public boolean isNauseaDisabled()         { return isEnabled("Nausea"); }
    public boolean isBadEffectsDisabled()     { return isEnabled("Bad Effects"); }
    public boolean isWardenDarknessDisabled() { return isEnabled("Warden Darkness"); }
}
