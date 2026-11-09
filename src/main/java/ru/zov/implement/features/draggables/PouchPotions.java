package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;

import java.util.ArrayList;
import java.util.List;

public class PouchPotions extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<StatusEffectInstance> effects = new ArrayList<>();

    public PouchPotions() {
        super("PouchPotions", 10, 60, 120, 25, true);
    }

    @Override
    public void tick() {
        effects.clear();
        if (mc.player != null) {
            effects.addAll(mc.player.getStatusEffects());
        }
    }

    @Override
    public boolean visible() {
        // Если открыт чат, принудительно показываем элемент для перетаскивания
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return !effects.isEmpty();
    }

    private String getEffectDuration(StatusEffectInstance effect) {
        int ticks = effect.getDuration();
        if (ticks == Integer.MAX_VALUE || effect.isInfinite()) return "∞";
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        return String.format("%d:%02d", minutes, seconds % 60);
    }

    private String getEffectName(StatusEffectInstance effect) {
        return effect.getEffectType().value().getName().getString();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // Заголовок
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), 17.5F)
                .round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        // Основной фон
        blur.render(ShapeProperties.create(matrix, this.getX(), (float) this.getY() + 17.0F, this.getWidth(), this.getHeight() - 17)
                .round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        // Заголовок по центру
        float centerX = this.getX() + this.getWidth() / 2.0F;
        String header = "Potions";
        font.drawString(matrix, header, (int) (centerX - font.getStringWidth(header) / 2.0F), this.getY() + 7, ColorUtil.getText());

        // Полоска акцента
        rectangle.render(ShapeProperties.create(matrix, this.getX() + 5, this.getY() + 16.5F, this.getWidth() - 10, 1.0F)
                .color(ColorUtil.getClientColor()).build());

        int offset = 23;
        int maxWidth = 120;

        // Если открыт чат и эффектов нет, показываем фейковый эффект для перетаскивания
        if (effects.isEmpty() && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            // Рисуем иконку SPEED (как заглушку)
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    net.minecraft.client.gui.hud.InGameHud.getEffectTexture(StatusEffects.SPEED),
                    (int) this.getX() + 5, (int) this.getY() + offset - 2, 8, 8
            );
            fontPotion.drawString(matrix, "Скорость", this.getX() + 18, this.getY() + offset + 1, ColorUtil.getText(0.5F));
            fontPotion.drawString(matrix, "1:00", this.getX() + this.getWidth() - (int) fontPotion.getStringWidth("1:00") - 5, this.getY() + offset + 1, ColorUtil.getText(0.5F));
            offset += 12;
        } else {
            for (StatusEffectInstance effect : effects) {
                String name = getEffectName(effect);
                String time = getEffectDuration(effect);

                int amplifier = effect.getAmplifier();
                String level = amplifier > 0 ? " " + (amplifier + 1) : "";

                // Иконка эффекта
                context.drawGuiTexture(
                        RenderPipelines.GUI_TEXTURED,
                        net.minecraft.client.gui.hud.InGameHud.getEffectTexture(effect.getEffectType()),
                        (int) this.getX() + 5, (int) this.getY() + offset - 2, 8, 8
                );

                // Название эффекта (слева)
                fontPotion.drawString(matrix, name + level, this.getX() + 18, this.getY() + offset + 1, ColorUtil.getText());

                // Время (справа)
                int timeWidth = (int) fontPotion.getStringWidth(time);
                fontPotion.drawString(matrix, time, this.getX() + this.getWidth() - timeWidth - 5, this.getY() + offset + 1, ColorUtil.getText(0.7F));

                int width = (int) fontPotion.getStringWidth(name + level + time) + 30;
                maxWidth = Math.max(maxWidth, width);
                offset += 12;
            }
        }

        this.setWidth(maxWidth);
        this.setHeight(offset + 5);
    }
}
