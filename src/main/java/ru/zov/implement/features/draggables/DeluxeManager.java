package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;

public class DeluxeManager {
    public final List<DeluxeDraggable> elements = new ArrayList<>();

    public void init() {
        elements.add(new DeluxeWatermark());
        elements.add(new DeluxeTargetHud());
        elements.add(new DeluxeStaff());
        elements.add(new DeluxePotions());
        elements.add(new DeluxeHotKeys());
        elements.add(new DeluxeNotifications());
    }

    public void drawAll(int mouseX, int mouseY) {
        for (DeluxeDraggable element : elements) {
            element.draw(mouseX, mouseY);
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
