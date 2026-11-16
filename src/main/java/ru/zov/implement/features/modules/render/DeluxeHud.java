package ru.zov.implement.features.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import ru.zov.implement.features.draggables.DeluxeManager;
import ru.zov.implement.features.modules.Module;

public class DeluxeHud extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    public static DeluxeManager deluxeManager;

    public DeluxeHud() {
        super("DeluxeHud", "DeluxeHud", Category.RENDER);
        deluxeManager = new DeluxeManager();
        deluxeManager.init();
    }

    public void drawHud(DrawContext context, int mouseX, int mouseY) {
        if (!this.isToggled() || mc.options.hudHidden) return;

        if (deluxeManager != null) {
            deluxeManager.drawAll(context, mouseX, mouseY);
        }
    }

    public static void handleMouseClick(int mouseX, int mouseY, int mouseButton) {
        if (deluxeManager != null) deluxeManager.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public static void handleMouseRelease(int mouseX, int mouseY, int state) {
        if (deluxeManager != null) deluxeManager.mouseReleased(mouseX, mouseY, state);
    }
}
