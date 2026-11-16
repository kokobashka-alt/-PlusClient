package ru.zov.implement.features.draggables;

import net.minecraft.client.Minecraft;

public abstract class DeluxeDraggable {
    protected final Minecraft mc = Minecraft.getMinecraft();

    public String name;
    public int x, y, width, height;
    public boolean dragging;
    public boolean enabled = true;
    private int dragX, dragY;

    public DeluxeDraggable(String name, int x, int y, int width, int height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(int mouseX, int mouseY);

    protected void updateDrag(int mouseX, int mouseY) {
        if (dragging) {
            this.x = mouseX - dragX;
            this.y = mouseY - dragY;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            dragging = false;
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
