package ru.zov.implement.features.draggables;

import ru.zov.implement.utils.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeluxeNotifications extends DeluxeDraggable {

    private static final List<NotificationItem> notifications = new ArrayList<>();

    public DeluxeNotifications() {
        super("DeluxeNotifications", 700, 260, 140, 24);
    }

    public static void send(String title, String message, long durationMs) {
        notifications.add(new NotificationItem(title, message, System.currentTimeMillis() + durationMs));
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (!enabled) return;
        updateDrag(mouseX, mouseY);

        long now = System.currentTimeMillis();
        Iterator<NotificationItem> it = notifications.iterator();
        int offsetY = y;

        while (it.hasNext()) {
            NotificationItem item = it.next();
            if (now > item.expireTime) {
                it.remove();
                continue;
            }

            RenderUtils.drawRect(x, offsetY, x + width, offsetY + 24, new Color(16, 16, 20, 215).getRGB());
            RenderUtils.drawRect(x, offsetY, x + 2, offsetY + 24, new Color(145, 70, 255).getRGB());

            mc.fontRendererObj.drawStringWithShadow(item.title, x + 6, offsetY + 3, Color.WHITE.getRGB());
            mc.fontRendererObj.drawStringWithShadow("§7" + item.message, x + 6, offsetY + 13, Color.LIGHT_GRAY.getRGB());

            offsetY -= 28;
        }
    }

    private static class NotificationItem {
        String title, message;
        long expireTime;

        NotificationItem(String title, String message, long expireTime) {
            this.title = title;
            this.message = message;
            this.expireTime = expireTime;
        }
    }
}
