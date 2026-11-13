package ru.zov.implement.features.draggables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;

public class JavelinInfoDraggable extends AbstractDraggable {

    public JavelinInfoDraggable() {
        super("javelinInfo", 10, 35, 170, 20);
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String text = String.format("cords x: %d y: %d z: %d  |  ⏱ 20.0 tps", 
                MathHelper.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()),
                MathHelper.floor(mc.player.getZ()));

        FontRenderer font = Fonts.bold;

        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColorUtil.rgba(16, 16, 16, 180));

        if (font != null) {
            font.drawString(context.getMatrices(), text, getX() + 8, getY() + 6, -1);
        }
    }
}
