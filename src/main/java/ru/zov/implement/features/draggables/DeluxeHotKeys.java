package ru.zov.implement.features.draggables;

import org.lwjgl.input.Keyboard;
import ru.zov.PlusClient;
import ru.zov.implement.features.modules.Module;
import ru.zov.implement.utils.RenderUtils;
import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class DeluxeHotKeys extends DeluxeDraggable {

    public DeluxeHotKeys() {
        super("DeluxeHotKeys", 720, 140, 115, 22);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (!enabled) return;
        updateDrag(mouseX, mouseY);

        List<Module> boundModules = PlusClient.instance.moduleManager.getModules().stream()
                .filter(m -> m.isToggled() && m.getKey() != Keyboard.KEY_NONE)
                .collect(Collectors.toList());

        if (boundModules.isEmpty() && !dragging) return;

        int count = Math.max(1, boundModules.size());
        this.height = 18 + count * 12;

        RenderUtils.drawRect(x, y, x + width, y + height, new Color(16, 16, 20, 200).getRGB());
        RenderUtils.drawRect(x, y, x + width, y + 2, new Color(145, 70, 255).getRGB());
        mc.fontRendererObj.drawStringWithShadow("Keybinds", x + 6, y + 5, Color.WHITE.getRGB());

        int offsetY = y + 18;
        if (boundModules.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow("§7Нет биндов", x + 6, offsetY, Color.GRAY.getRGB());
        } else {
            for (Module m : boundModules) {
                String keyName = Keyboard.getKeyName(m.getKey());
                mc.fontRendererObj.drawStringWithShadow(m.getName() + " §7[" + keyName + "]", x + 6, offsetY, Color.WHITE.getRGB());
                offsetY += 12;
            }
        }
    }
}
