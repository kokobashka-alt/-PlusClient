package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.TriggerBot;

public class PouchTargetInfo extends AbstractDraggable {

    private final Animation fadeAnim = new DecelerateAnimation().setMs(200).setValue(1.0);
    private LivingEntity lastTarget;
    
    // Анимации для полоски HP
    private float animatedHealth;
    private float animatedTrailHealth;

    public PouchTargetInfo() {
        super("PouchTargetHud", 140, 110, 120, 40, true);
    }

    @Override
    public boolean visible() {
        return lastTarget != null || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        LivingEntity triggerTarget = TriggerBot.getInstance().getTarget();
        
        LivingEntity currentTarget = auraTarget != null ? auraTarget : triggerTarget;

        if (currentTarget != null && currentTarget.isAlive()) {
            lastTarget = currentTarget;
            fadeAnim.setDirection(Direction.FORWARDS);
        } else {
            fadeAnim.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        // Если открыт чат, показываем себя, чтобы можно было двигать
        LivingEntity drawTarget = lastTarget;
        if (drawTarget == null && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            drawTarget = mc.player;
        }
        if (drawTarget == null) return;

        float anim = fadeAnim.getOutput().floatValue();
        if (anim <= 0.05f) return;

        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontSmall = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // Размеры панели
        float width = 120f;
        float height = 38f;
        float x = this.getX();
        float y = this.getY();

        // Данные цели
        float maxHealth = Math.max(1.0f, drawTarget.getMaxHealth());
        float health = drawTarget.getHealth();
        float absorption = drawTarget.getAbsorptionAmount();
        
        // Плавная анимация HP
        animatedHealth += (health - animatedHealth) * 0.15f;
        if (animatedTrailHealth < animatedHealth) {
            animatedTrailHealth = animatedHealth;
        } else {
            animatedTrailHealth += (animatedHealth - animatedTrailHealth) * 0.05f;
        }

        float healthPercent = MathHelper.clamp(animatedHealth / maxHealth, 0.0f, 1.0f);
        float trailPercent = MathHelper.clamp(animatedTrailHealth / maxHealth, 0.0f, 1.0f);

        // --- ОТРИСОВКА ---
        
        // 1. Фон с блюром
        blur.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(5.0f).softness(1.0f).thickness(2.0f)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7f)).build());

        // 2. Голова игрока (слева)
        float headSize = 30f;
        float headX = x + 4f;
        float headY = y + (height - headSize) / 2f;
        
        if (drawTarget instanceof PlayerEntity p) {
            Render2DUtil.drawTexture(matrix, Render2DUtil.getHeadId(drawTarget), headX, headY, headSize, 3.0f, 1, 1, 8, ColorUtil.getRect(1.0f));
        } else {
            // Плейсхолдер для мобов (квадрат)
            rectangle.render(ShapeProperties.create(matrix, headX, headY, headSize, headSize)
                    .round(3.0f).color(ColorUtil.getRectDarker(0.5f)).build());
            font.drawCenteredString(matrix, "?", headX + headSize / 2f, headY + 8f, ColorUtil.getText());
        }

        // 3. Ник
        String name = drawTarget.getName().getString();
        float textX = headX + headSize + 6f;
        float nameMaxWidth = width - headSize - 15f;
        while (name.length() > 1 && font.getStringWidth(name) > nameMaxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        font.drawString(matrix, name, textX, y + 6f, ColorUtil.getText());

        // 4. HP текст
        String hpText = String.format("%.1f", health);
        float hpTextX = textX;
        float hpTextY = y + 20f;
        
        fontSmall.drawString(matrix, "HP: ", hpTextX, hpTextY, ColorUtil.getText(0.7f));
        fontSmall.drawString(matrix, hpText, hpTextX + fontSmall.getStringWidth("HP: "), hpTextY, ColorUtil.getText());
        
        // Золотые сердца (поглощение)
        if (absorption > 0) {
            String absText = " +" + String.format("%.1f", absorption);
            float absX = hpTextX + fontSmall.getStringWidth("HP: " + hpText);
            fontSmall.drawGradientString(matrix, absText, absX, hpTextY, ColorUtil.getColor(236, 183, 39), ColorUtil.getColor(147, 108, 16));
        }

        // 5. Полоска HP
        float barWidth = width - headSize - 15f;
        float barX = textX;
        float barY = y + height - 8f;
        float barHeight = 3.5f;

        // Фон полоски (темный)
        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth, barHeight)
                .round(1.5f).color(ColorUtil.getRectDarker(0.5f)).build());

        // Трейл (белая полоска, которая плавно исчезает при получении урона)
        if (trailPercent > healthPercent) {
            rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth * trailPercent, barHeight)
                    .round(1.5f).color(ColorUtil.getColor(255, 255, 255, 100)).build());
        }

        // Основная полоска (цвет зависит от HP)
        int barColor = healthPercent > 0.6f ? ColorUtil.getColor(112, 220, 112) : // Зеленый
                       healthPercent > 0.3f ? ColorUtil.getColor(255, 212, 92) :  // Желтый
                       ColorUtil.getColor(255, 80, 80);                          // Красный

        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth * healthPercent, barHeight)
                .round(1.5f).color(barColor).build());

        // 6. Броня и предметы в руках (снизу)
        float itemSize = 12f;
        float itemY = y + height + 2f;
        float itemX = x;
        
        ScissorManager scissor = main.getScissorManager();
        scissor.push(matrix, x, itemY, width, itemSize + 2f);

        // Броня
        for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = drawTarget.getEquippedStack(slot);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, (int)itemX, (int)itemY);
                    context.drawStackOverlay(mc.textRenderer, stack, (int)itemX, (int)itemY);
                    itemX += itemSize + 2f;
                }
            }
        }
        
        // Руки
        ItemStack mainHand = drawTarget.getMainHandStack();
        if (!mainHand.isEmpty()) {
            context.drawItem(mainHand, (int)itemX, (int)itemY);
            context.drawStackOverlay(mc.textRenderer, mainHand, (int)itemX, (int)itemY);
            itemX += itemSize + 2f;
        }
        
        ItemStack offHand = drawTarget.getOffHandStack();
        if (!offHand.isEmpty()) {
            context.drawItem(offHand, (int)itemX, (int)itemY);
            context.drawStackOverlay(mc.textRenderer, offHand, (int)itemX, (int)itemY);
        }

        scissor.pop();
    }
}
