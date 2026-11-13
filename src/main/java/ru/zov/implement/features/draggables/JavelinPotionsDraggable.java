package ru.zov.implement.features.draggables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;

public class JavelinPotionsDraggable extends AbstractDraggable {

    public JavelinPotionsDraggable() {
        super("javelinPotions", 140, 60, 130, 40);
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getStatusEffects().isEmpty()) return;

        FontRenderer font = Fonts.bold;
        int activeCount = mc.player.getStatusEffects().size();
        int height = 16 + (activeCount * 16);

        context.fill(getX(), getY(), getX() + getWidth(), getY() + height, ColorUtil.rgba(16, 16, 16, 180));

        if (font != null) {
            font.drawString(context.getMatrices(), "javelinPotions", getX() + 6, getY() + 5, 0xFFFF5555);

            int offset = 16;
            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
                String name = Text.translatable(effect.getTranslationKey()).getString() + " " + (effect.getAmplifier() + 1);
                
                font.drawString(context.getMatrices(), name, getX() + 6, getY() + 5 + offset, -1);
                offset += 16;
            }
        }
    }
}
