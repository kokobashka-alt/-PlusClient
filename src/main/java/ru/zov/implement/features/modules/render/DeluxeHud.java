package ru.zov.implement.features.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import ru.zov.implement.features.draggables.DeluxeManager;
import ru.zov.implement.features.modules.Module;

public class DeluxeHud extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Менеджер, управляющий всеми вашими виджетами (Watermark, TargetHud, Staff и т.д.)
    public static DeluxeManager deluxeManager;

    public DeluxeHud() {
        super("DeluxeHud", "Красивый настраиваемый интерфейс в стиле Deluxe", Category.RENDER);
        deluxeManager = new DeluxeManager();
        deluxeManager.init();
    }

    /**
     * Вызывайте этот метод в вашем ивенте отрисовки 2D (Render2DEvent / RenderGameOverlayEvent)
     */
    public void drawHud(int mouseX, int mouseY) {
        if (!this.isToggled() || mc.gameSettings.showDebugInfo) return;

        // Отрисовываем все элементы через менеджер
        if (deluxeManager != null) {
            deluxeManager.drawAll(mouseX, mouseY);
        }
    }

    // Методы для проброса кликов мыши (чтобы элементы можно было перетаскивать)
    public static void handleMouseClick(int mouseX, int mouseY, int mouseButton) {
        if (deluxeManager != null) {
            deluxeManager.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public static void handleMouseRelease(int mouseX, int mouseY, int state) {
        if (deluxeManager != null) {
            deluxeManager.mouseReleased(mouseX, mouseY, state);
        }
    }
}
