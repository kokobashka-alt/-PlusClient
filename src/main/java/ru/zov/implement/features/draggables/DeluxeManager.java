package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public class DeluxeManager {
    public final List<DeluxeDraggable> elements = new ArrayList<>();

    public void init() {
        elements.add(new DeluxeWatermark());
        // Добавьте остальные элементы по аналогии
    }

    public void drawAll(DrawContext context, int mouseX, int mouseY) {
        for (DeluxeDraggable element : elements) {
            element.draw(context, mouseX, mouseY);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (DeluxeDraggable element : elements) {
            element.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        for (DeluxeDraggable element : elements) {
            element.mouseReleased(mouseX, mouseY, state);
        }
    }
}
