package ru.zov.implement.features.modules.render;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.draggable.DraggableRepository;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.core.Main;

public class JavelinHud extends Module {

    private final MultiSelectSetting elements = new MultiSelectSetting("Elements", "Select HUD elements")
            .value("Watermark", "Info", "Hotkeys", "Potions", "TargetHud");

    public JavelinHud() {
        super("JavelinHud", "Javelin HUD", ModuleCategory.RENDER);
        setup(elements);
    }

    @Override
    public void activate() {
        updateDraggables(true);
    }

    @Override
    public void deactivate() {
        updateDraggables(false);
    }

    public void onTick() {
        if (!isState()) return;
        updateDraggables(true);
    }

    private void updateDraggables(boolean moduleEnabled) {
        DraggableRepository repo = Main.getInstance().getDraggableRepository();
        if (repo == null) return;

        setDraggableState(repo, "javelinWatermark", moduleEnabled && elements.isSelected("Watermark"));
        setDraggableState(repo, "javelinInfo", moduleEnabled && elements.isSelected("Info"));
        setDraggableState(repo, "javelinHotkeys", moduleEnabled && elements.isSelected("Hotkeys"));
        setDraggableState(repo, "javelinPotions", moduleEnabled && elements.isSelected("Potions"));
        setDraggableState(repo, "javelinTargetHud", moduleEnabled && elements.isSelected("TargetHud"));
    }

    private void setDraggableState(DraggableRepository repo, String name, boolean state) {
        AbstractDraggable draggable = repo.get(name);
        if (draggable != null) {
            draggable.setVisible(state);
        }
    }
}
