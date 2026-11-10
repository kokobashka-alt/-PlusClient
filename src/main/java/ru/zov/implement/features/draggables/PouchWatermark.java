package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.module.Module;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PouchWatermark extends AbstractDraggable {

    private final Map<Module, Long> appearAnimations = new HashMap<>();
    private final Set<Module> activeModules = new HashSet<>();
    private long lastRenderTime = System.currentTimeMillis();
    private float previewAlpha = 0.0F;
    private float currentMaxTextWidth = 0.0F;

    public PouchWatermark() {
        // Координаты: x=10, y=10 (как в оригинале)
        super("PouchWatermark", 10, 10, 120, 16, true);
    }

    @Override
    public boolean visible() {
        return true; // Всегда видно
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null) return;

        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer iconFont = Fonts.getSize(14, Fonts.Type.DEFAULT);

        long time = System.currentTimeMillis();
        float delta = (time - lastRenderTime) / 1000.0F;
        lastRenderTime = time;

        // Собираем активные модули (для уведомлений)
        activeModules.clear();
        List<Module> modules = main.getModuleProvider().getModules();
        for (Module m : modules) {
            if (m.isState()) activeModules.add(m);
        }

        boolean isChat = PlayerIntersectionUtil.isChat(mc.currentScreen);
        
        // Если нет активных модулей и чат закрыт, но есть анимация — плавно скрываем
        float targetAlpha = (isChat || !activeModules.isEmpty()) ? 1.0F : 0.0F;
        previewAlpha += (targetAlpha - previewAlpha) * Math.min(1.0f, 8.0f * delta);

        if (previewAlpha < 0.01f && !isChat) {
            this.setWidth(0);
            this.setHeight(0);
            return;
        }

        float x = this.getX();
        float y = this.getY();
        
        // --- 1. ОСНОВНАЯ ПАНЕЛЬ (КАК НА СКРИНЕ) ---
        String clientName = "Pouch"; // Имя клиента
        String username = mc.player.getName().getString();
        String fps = mc.getCurrentFps() + "fps";
        String ping = "0ms";
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency() + "ms";
        }

        // Считаем ширину
        String fullText = clientName + " | " + username + " | " + ping + " | " + fps;
        float mainWidth = font.getStringWidth(fullText) + 20;
        float mainHeight = 16f;

        // Фон
        blur.render(ShapeProperties.create(matrix, x, y, mainWidth, mainHeight)
                .round(4.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        // Текст (градиент)
        float currentX = x + 5;
        font.drawGradientString(matrix, clientName, currentX, y + 5, ColorUtil.fade(0), ColorUtil.fade(100));
        currentX += font.getStringWidth(clientName);

        font.drawString(matrix, " | ", currentX, y + 5, ColorUtil.getText(0.5F));
        currentX += font.getStringWidth(" | ");

        font.drawString(matrix, username, currentX, y + 5, ColorUtil.getText());
        currentX += font.getStringWidth(username);

        font.drawString(matrix, " | ", currentX, y + 5, ColorUtil.getText(0.5F));
        currentX += font.getStringWidth(" | ");

        font.drawString(matrix, ping, currentX, y + 5, ColorUtil.getClientColor());
        currentX += font.getStringWidth(ping);

        font.drawString(matrix, " | ", currentX, y + 5, ColorUtil.getText(0.5F));
        currentX += font.getStringWidth(" | ");

        font.drawString(matrix, fps, currentX, y + 5, ColorUtil.getClientColor());

        // --- 2. УВЕДОМЛЕНИЯ (если модуль включен/выключен) ---
        // В оригинале это отдельный стек. Мы сделаем его прямо под ватермаркой.
        float notifyY = y + mainHeight + 2f;
        float maxNotifyWidth = mainWidth;

        // Анимация появления модулей
        for (Module m : activeModules) {
            appearAnimations.putIfAbsent(m, time);
        }
        appearAnimations.keySet().removeIf(m -> !activeModules.contains(m));

        for (Map.Entry<Module, Long> entry : appearAnimations.entrySet()) {
            Module m = entry.getKey();
            long startTime = entry.getValue();
            float life = (time - startTime) / 1000.0F;
            
            // Модуль висит 2.5 секунды, потом исчезает
            if (life > 2.5f) continue;

            float anim = Math.min(1.0f, life * 4.0f); // Плавное появление
            if (life > 2.0f) {
                anim = 1.0f - (life - 2.0f) * 2.0f; // Плавное исчезновение
            }
            if (anim <= 0.01f) continue;

            String text = "\"" + m.getVisibleName() + "\" был включен";
            float textWidth = font.getStringWidth(text) + 10;
            maxNotifyWidth = Math.max(maxNotifyWidth, textWidth);

            // Фон уведомления
            blur.render(ShapeProperties.create(matrix, x, notifyY, textWidth, 12)
                    .round(3.0F).color(ColorUtil.getRect(0.7F)).build());

            // Иконка (зеленый кружок)
            rectangle.render(ShapeProperties.create(matrix, x + 4, notifyY + 4, 4, 4)
                    .round(2.0F).color(ColorUtil.getColor(80, 220, 80)).build());

            // Текст
            font.drawString(matrix, text, x + 12, notifyY + 2, ColorUtil.getText());

            notifyY += (12 + 2) * anim;
        }

        this.setWidth(maxNotifyWidth);
        this.setHeight(notifyY - y);
    }
}
