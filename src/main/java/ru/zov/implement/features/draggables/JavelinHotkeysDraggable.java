package ru.zov.implement.features.draggables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class JavelinHotkeysDraggable extends AbstractDraggable {

    private final Map<String, String> hotkeys = new LinkedHashMap<String, String>() {{
        put("JavelinFly", "C");
        put("JavelinAura", "R");
    }};

    public JavelinHotkeysDraggable() {
        super("javelinHotkeys", 10, 60, 120, 20 + (2 << 4));
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();
        FontRenderer font = Fonts.bold;

        int height = 16 + (hotkeys.size() * 16);
        context.fill(getX(), getY(), getX() + getWidth(), getY() + height, ColorUtil.rgba(16, 16, 16, 180));

        if (font != null) {
            font.drawString(context.getMatrices(), "javelinHotkeys", getX() + 6, getY() + 5, 0xFFFF5555);

            int offset = 16;
            for (Map.Entry<String, String> entry : hotkeys.entrySet()) {
                font.drawString(context.getMatrices(), entry.getKey(), getX() + 6, getY() + 5 + offset, 0xCCCCCC);
                
                int keyWidth = (int) font.getStringWidth(entry.getValue()) + 6;
                context.fill(getX() + getWidth() - keyWidth - 6, getY() + 3 + offset, getX() + getWidth() - 6, getY() + 13 + offset, ColorUtil.rgba(255, 255, 255, 50));
                font.drawString(context.getMatrices(), entry.getValue(), getX() + getWidth() - keyWidth - 3, getY() + 5 + offset, -1);
                
                offset += 16;
            }
        }
    }
}
