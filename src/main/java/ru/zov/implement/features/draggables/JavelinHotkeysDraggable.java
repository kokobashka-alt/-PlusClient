package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class JavelinHotkeysDraggable extends AbstractDraggable {

    private final Map<String, String> hotkeys = new LinkedHashMap<>();

    public JavelinHotkeysDraggable() {
        super("javelinHotkeys", 10, 60, 120, 52, true);
        hotkeys.put("JavelinFly", "C");
        hotkeys.put("JavelinAura", "R");
    }

    @Override
    public void drawDraggable(DrawContext context) {
        Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.BOLD);

        int height = 16 + hotkeys.size() * 16;
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), height)
                .round(3.0F).color(ColorUtil.getColor(16, 16, 16, 0.7F)).build());

        font.drawString(matrix, "javelinHotkeys", getX() + 6, getY() + 5, 0xFFFF5555);

        int offset = 16;
        for (Map.Entry<String, String> entry : hotkeys.entrySet()) {
            font.drawString(matrix, entry.getKey(), getX() + 6, getY() + 5 + offset, 0xFFCCCCCC);

            int keyWidth = (int) font.getStringWidth(entry.getValue()) + 6;
            rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - keyWidth - 6, getY() + 3 + offset, keyWidth, 10)
                    .round(2.0F).color(ColorUtil.getColor(255, 255, 255, 0.2F)).build());
            font.drawString(matrix, entry.getValue(), getX() + getWidth() - keyWidth - 3, getY() + 5 + offset, -1);

            offset += 16;
        }
    }
}
