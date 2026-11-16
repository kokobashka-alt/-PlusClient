package ru.zov.implement.features.draggables;

import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import ru.zov.implement.utils.RenderUtils;
import java.awt.Color;
import java.util.Collection;

public class DeluxePotions extends DeluxeDraggable {

    public DeluxePotions() {
        super("DeluxePotions", 10, 210, 115, 22);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (!enabled || mc.thePlayer == null) return;
        updateDrag(mouseX, mouseY);

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects.isEmpty() && !dragging) return;

        int count = Math.max(1, effects.size());
        this.height = 18 + count * 12;

        RenderUtils.drawRect(x, y, x + width, y + height, new Color(16, 16, 20, 200).getRGB());
        RenderUtils.drawRect(x, y, x + width, y + 2, new Color(145, 70, 255).getRGB());
        mc.fontRendererObj.drawStringWithShadow("Potions", x + 6, y + 5, Color.WHITE.getRGB());

        int offsetY = y + 18;
        if (effects.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow("§7Нет эффектов", x + 6, offsetY, Color.GRAY.getRGB());
        } else {
            for (PotionEffect effect : effects) {
                Potion potion = Potion.potionTypes[effect.getPotionID()];
                String name = I18n.format(potion.getName());
                String duration = Potion.getDurationString(effect);
                mc.fontRendererObj.drawStringWithShadow(name + " §7" + duration, x + 6, offsetY, Color.WHITE.getRGB());
                offsetY += 12;
            }
        }
    }
}
