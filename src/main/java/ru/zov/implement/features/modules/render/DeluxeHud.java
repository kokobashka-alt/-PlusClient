package ru.zov.implement.features.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import ru.zov.PlusClient; // Путь к вашему главному классу клиента (при необходимости измените)
import ru.zov.implement.features.modules.Module; // Путь к базовому классу модулей
import ru.zov.implement.utils.RenderUtils; // Путь к утилитам рендеринга (при необходимости измените)

import java.awt.Color;
import java.util.Comparator;

public class DeluxeHud {
    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Основной метод отрисовки HUD, вызываемый каждый кадр (например, при событии Render2DEvent)
     */
    public void drawHud() {
        if (mc.gameSettings.showDebugInfo) return; // Не отрисовывать при включенном отладочном экране F3

        ScaledResolution sr = new ScaledResolution(mc);

        // 1. Отрисовка стильной Ватермарки (Watermark)
        drawWatermark();

        // 2. Отрисовка списка активных модулей (ArrayList)
        drawArrayList(sr);

        // 3. Отрисовка информационной панели (FPS, Координаты)
        drawInfoPanel(sr);
    }

    /**
     * Отрисовка логотипа и информации о клиенте в левом верхнем углу
     */
    private void drawWatermark() {
        String watermarkText = "DeluxeHud §7| §f" + PlusClient.NAME + " §7[§f" + PlusClient.VERSION + "§7] §7| §f" + mc.DebugFPS + " FPS";
        
        int x = 4;
        int y = 4;
        int padding = 6;

        int textWidth = mc.fontRendererObj.getStringWidth(watermarkText);
        
        // Полупрозрачная подложка
        RenderUtils.drawRect(x - 2, y - 2, x + textWidth + padding + 2, y + 12 + 2, new Color(20, 20, 20, 180).getRGB());
        // Неоновая верхняя линия ватермарки
        RenderUtils.drawRect(x - 2, y - 3, x + textWidth + padding + 2, y - 2, new Color(138, 43, 226).getRGB());

        mc.fontRendererObj.drawStringWithShadow(watermarkText, x + 3, y + 2, Color.WHITE.getRGB());
    }

    /**
     * Отрисовка списка активных модулей клиента в правом верхнем углу
     */
    private void drawArrayList(ScaledResolution sr) {
        int yOffset = 4;

        if (PlusClient.instance != null && PlusClient.instance.moduleManager != null) {
            PlusClient.instance.moduleManager.getModules().stream()
                    .filter(Module::isToggled)
                    .sorted(Comparator.comparingDouble(m -> -mc.fontRendererObj.getStringWidth(m.getName())))
                    .forEach(module -> {
                        String name = module.getName();
                        int width = mc.fontRendererObj.getStringWidth(name);
                        int x = sr.getScaledWidth() - width - 6;

                        RenderUtils.drawRect(x - 4, yOffset, sr.getScaledWidth() - 2, yOffset + 12, new Color(15, 15, 15, 150).getRGB());
                        RenderUtils.drawRect(sr.getScaledWidth() - 3, yOffset, sr.getScaledWidth() - 2, yOffset + 12, new Color(138, 43, 226).getRGB());

                        mc.fontRendererObj.drawStringWithShadow(name, x - 2, yOffset + 2, Color.WHITE.getRGB());
                        
                        // Если элементы накладываются друг на друга, раскомментируйте строку ниже:
                        // yOffset += 12; 
                    });
        }
    }

    /**
     * Отрисовка мини-панели с информацией игрока (в левом нижнем углу)
     */
    private void drawInfoPanel(ScaledResolution sr) {
        int x = 4;
        int y = sr.getScaledHeight() - 35;

        String coords = String.format("XYZ: §f%d, %d, %d", (int) mc.thePlayer.posX, (int) mc.thePlayer.posY, (int) mc.thePlayer.posZ);
        String fps = String.format("FPS: §f%d", mc.DebugFPS);

        mc.fontRendererObj.drawStringWithShadow(coords, x, y, new Color(200, 200, 200).getRGB());
        mc.fontRendererObj.drawStringWithShadow(fps, x, y + 10, new Color(200, 200, 200).getRGB());
    }
}
