package ru.zov.implement.features.draggables;

import ru.zov.api.feature.draggable.AbstractDraggable;

import java.util.ArrayList;
import java.util.List;

public class JavelinHUD {
    
    private final List<AbstractDraggable> javelinModules = new ArrayList<>();

    public JavelinHUD() {
        // Регистрация всех твоих модулей
        javelinModules.add(new JavelinWatermarkDraggable());
        javelinModules.add(new JavelinInfoDraggable());
        javelinModules.add(new JavelinHotkeysDraggable());
        javelinModules.add(new JavelinPotionsDraggable());
        javelinModules.add(new JavelinTargetHudDraggable());
    }

    public List<AbstractDraggable> getModules() {
        return javelinModules;
    }
}
