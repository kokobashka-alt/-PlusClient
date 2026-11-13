package com.javelinhud.ui;

import com.javelinhud.ui.elements.*;
import net.minecraft.client.gui.ScaledResolution;

public class JavelinHUD {
    private final JavelinWatermark javelinWatermark = new JavelinWatermark();
    private final JavelinInfo javelinInfo = new JavelinInfo();
    private final JavelinHotkeys javelinHotkeys = new JavelinHotkeys();
    private final JavelinPotions javelinPotions = new JavelinPotions();
    private final JavelinTargetHUD javelinTargetHUD = new JavelinTargetHUD();

    public void render(ScaledResolution sr) {
        javelinWatermark.draw(10, 10);
        javelinInfo.draw(10, 32);
        
        javelinHotkeys.draw(10, 60);
        javelinPotions.draw(140, 60);
        
        // Пример вызова таргет-худа (передавай цель из киллауры)
        // javelinTargetHUD.draw(sr.getScaledWidth() / 2 - 65, sr.getScaledHeight() / 2 + 20, target);
    }
}
