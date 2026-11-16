package ru.zov.implement.features.draggables;

import ru.zov.implement.utils.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class DeluxeStaff extends DeluxeDraggable {

    public DeluxeStaff() {
        super("DeluxeStaff", 10, 140, 110, 22);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (!enabled) return;
        updateDrag(mouseX, mouseY);

        List<String> staffOnline = new ArrayList<>();
        if (dragging) staffOnline.add("Admin_1"); // Превью при перетаскивании

        int listSize = Math.max(1, staffOnline.size());
        this.height = 18 + listSize * 12;

        RenderUtils.drawRect(x, y, x + width, y + height, new Color(16, 16, 20, 200).getRGB());
        RenderUtils.drawRect(x, y, x + width, y + 2, new Color(145, 70, 255).getRGB());
        mc.fontRendererObj.drawStringWithShadow("Staff Online", x + 6, y + 5, Color.WHITE.getRGB());

        int offsetY = y + 18;
        if (staffOnline.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow("§7Пусто", x + 6, offsetY, Color.GRAY.getRGB());
        } else {
            for (String staff : staffOnline) {
                mc.fontRendererObj.drawStringWithShadow("§c[MOD] §f" + staff, x + 6, offsetY, Color.WHITE.getRGB());
                offsetY += 12;
            }
        }
    }
}
