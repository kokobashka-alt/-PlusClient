package ru.zov.implement.features.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.TriggerBot;

public class PouchTargetInfo extends AbstractDraggable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private LivingEntity target;
    private LivingEntity lastTarget; // Запоминаем цель для чата
    private float animatedHealth;

    public PouchTargetInfo() {
        super("PouchTargetInfo", 140, 110, 130, 42, true); // Поставил x=140, y=110 (по центру)
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        LivingEntity triggerTarget = TriggerBot.getInstance().getTarget();

        if (auraTarget != null && auraTarget.isAlive()) {
            target = auraTarget;
        } else if (triggerTarget != null && triggerTarget.isAlive()) {
            target = triggerTarget;
        } else {
            target = null;
        }

        // Если цель есть, запоминаем её
        if (target != null) {
            lastTarget = target;
            animatedHealth += (target.getHealth() - animatedHealth) * 0.2f;
        }
    }

    @Override
    public boolean visible() {
        // Если открыт чат — показываем элемент, чтобы его можно было двигать
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return target != null;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontSmall = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // Если цель пропала (например, открыт чат), используем последнюю запомненную
        LivingEntity drawTarget = target != null ? target : lastTarget;
        if (drawTarget == null) return;

        // Если открыт чат, рисуем "фейковую" цель (себя), чтобы можно было подвигать
        if (target == null && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            drawTarget = mc.player;
        }

        float maxHealth = Math.max(1.0f, drawTarget.getMaxHealth());
        float health = MathHelper.clamp(animatedHealth / maxHealth, 0.0f, 1.0f);

        // Фон с блюром
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), this.getHeight())
                .round(5.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        // Иконка головы игрока (слева)
        Render2DUtil.drawTexture(matrix, Render2DUtil.getHeadId(drawTarget), this.getX() + 5, this.getY() + 5, 32.0F, 3.0F, 1, 1, 8, ColorUtil.getRect(1.0F));

        // Ник (справа от головы)
        font.drawString(matrix, drawTarget.getName().getString(), this.getX() + 45, this.getY() + 8, ColorUtil.getText());

        // Хп текст (под ником)
        String hpText = String.format("%.1f", drawTarget.getHealth());
        fontSmall.drawString(matrix, "HP: " + hpText, this.getX() + 45, this.getY() + 21, ColorUtil.getText(0.7F));

        // Полоска HP (желто-фиолетовая, как на скрине)
        float barWidth = 75.0F;
        float barX = this.getX() + 45;
        float barY = this.getY() + 32;

        // Задний фон полоски
        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth, 4.0F)
                .round(2.0F).color(ColorUtil.getRectDarker(0.5F)).build());

        // Заполнение полоски (цвет зависит от хп)
        int healthColor = health > 0.6f ? ColorUtil.getColor(112, 220, 112) : // Зеленый
                          health > 0.3f ? ColorUtil.getColor(255, 212, 92) :  // Желтый
                          ColorUtil.getColor(255, 80, 80);                     // Красный

        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth * health, 4.0F)
                .round(2.0F).color(healthColor).build());

        this.setWidth(130);
        this.setHeight(42);
    }
}
