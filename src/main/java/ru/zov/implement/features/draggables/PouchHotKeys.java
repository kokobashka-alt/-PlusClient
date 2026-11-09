
package ru.zov.implement.features.draggables;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.module.Module;
import ru.zov.common.util.other.StringUtil;

public class PouchHotKeys extends AbstractDraggable {

    private List<Module> keysList = List.of();

    public PouchHotKeys() {
        super("PouchHotKeys", 300, 10, 100, 25, true);
    }

    @Override
    public void tick() {
        keysList = main.getModuleProvider()
                .getModules()
                .stream()
                .filter(module -> module.getAnimation().getOutput().floatValue() != 0.0F
                        && module.getKey() != -1)
                .toList();
    }

    @Override
    public boolean visible() {
        return !keysList.isEmpty();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        int x = (int) getX();
        int y = (int) getY();

        int width = 100;
        int height = 22 + keysList.size() * 13;

        // Background
        context.fill(
                x, y,
                x + width, y + height,
                0xAA101010
        );

        // Accent
        context.fill(
                x, y,
                x + 2, y + height,
                0xFF8B5CF6
        );

        // Header
        context.drawText(
                mc.textRenderer,
                "Hot Keys",
                x + 7,
                y + 6,
                0xFF8B5CF6,
                false
        );

        int offset = 22;
        int maxWidth = width;

        for (Module module : keysList) {
            String name = module.getName();
            String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";

            context.drawText(
                    mc.textRenderer,
                    name,
                    x + 7,
                    y + offset,
                    0xFFFFFFFF,
                    false
            );

            int bindWidth = mc.textRenderer.getWidth(bind);

            context.drawText(
                    mc.textRenderer,
                    bind,
                    x + width - bindWidth - 7,
                    y + offset,
                    0xFF8B5CF6,
                    false
            );

            maxWidth = Math.max(
                    maxWidth,
                    mc.textRenderer.getWidth(name)
                            + bindWidth + 20
            );

            offset += 13;
        }

        setWidth(maxWidth);
        setHeight(height);
    }
}
