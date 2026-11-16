package ru.zov.implement.features.draggables;

import net.minecraft.entity.EntityLivingBase;
import ru.zov.implement.utils.RenderUtils;
import java.awt.Color;

public class DeluxeTargetHud extends DeluxeDraggable {

    public DeluxeTargetHud() {
        super("DeluxeTargetHud", 380, 320, 135, 38);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (!enabled) return;
        updateDrag(mouseX, mouseY);

        EntityLivingBase target = (mc.pointedEntity instanceof EntityLivingBase)
                ? (EntityLivingBase) mc.pointedEntity
                : (dragging ? mc.thePlayer : null);

        if (target == null) return;

        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float hpPercent = Math.min(1.0f, Math.max(0.0f, hp / maxHp));

        RenderUtils.drawRect(x, y, x + width, y + height, new Color(16, 16, 20, 210).getRGB());
        RenderUtils.drawRect(x, y, x + width, y + 2, new Color(255, 65, 85).getRGB());

        RenderUtils.drawRect(x + 5, y + 6, x + 31, y + 32, new Color(35, 35, 42, 255).getRGB());

        mc.fontRendererObj.drawStringWithShadow(target.getName(), x + 36, y + 7, Color.WHITE.getRGB());
        mc.fontRendererObj.drawStringWithShadow(String.format("HP: %.1f", hp), x + 36, y + 18, new Color(180, 180, 180).getRGB());

        int barX = x + 36;
        int barY = y + 29;
        int barWidth = width - 42;
        RenderUtils.drawRect(barX, barY, barX + barWidth, barY + 4, new Color(40, 40, 45, 255).getRGB());
        RenderUtils.drawRect(barX, barY, barX + (int) (barWidth * hpPercent), barY + 4, new Color(255, 65, 85).getRGB());
    }
}
