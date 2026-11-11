package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
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
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.TriggerBot;

/**
 * Target HUD in the AspectDLC style (Aspect TargetHud, "DefaultStyle"): a 32 px high dark panel with an
 * 18 px head, the name, "HP: value (+absorption)" and a thin health bar along the bottom edge; armor and
 * hand items sit in small tinted plates above the panel at half scale. Not reproduced from the original:
 * head particles, the gradient on the health bar (a solid client-color fill is used) and the FtHelper
 * enemy cooldown plates. The name is cut to fit instead of fading out at the edge.
 */
public class PouchTargetInfo extends AbstractDraggable {
    private static final float PANEL_HEIGHT = 32.0F;
    private static final float MIN_WIDTH = 90.0F;
    private static final float HEAD_SIZE = 18.0F;
    private static final float PAD_LEFT = 3.0F;
    private static final float PAD_TEXT = 5.0F;
    private static final float PAD_RIGHT = 5.0F;
    private static final float BAR_HEIGHT = 5.9F;
    private static final float ITEM_SLOT = 10.0F;
    private static final float ITEM_PLATE_HEIGHT = 11.0F;
    private static final float ITEM_SCALE = 0.5F;

    private final Animation fadeAnim = new DecelerateAnimation().setMs(200).setValue(1.0);
    private LivingEntity lastTarget;

    private float animatedHealth;
    private float animatedTrailHealth;

    public PouchTargetInfo() {
        super("PouchTargetHud", 140, 110, 90, 32, true);
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

    private static int tintedPlateColor(int accent, float alpha) {
        int r = Math.min(12 + (int) (((accent >> 16) & 255) * 0.08F), 255);
        int g = Math.min(12 + (int) (((accent >> 8) & 255) * 0.08F), 255);
        int b = Math.min(14 + (int) ((accent & 255) * 0.08F), 255);
        return ColorUtil.getColor(r, g, b, (int) (210.0F * alpha));
    }

    private void drawItem(DrawContext context, ItemStack stack, float x, float y, float scale) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        matrix.pushMatrix();
        matrix.translate(x, y);
        matrix.scale(scale, scale);
        context.drawItem(stack, 0, 0);
        matrix.popMatrix();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        LivingEntity drawTarget = lastTarget;
        if (drawTarget == null && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            drawTarget = mc.player;
        }
        if (drawTarget == null) return;

        float anim = fadeAnim.getOutput().floatValue();
        if (anim <= 0.05f) return;

        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer nameFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer hpFont = Fonts.getSize(12, Fonts.Type.DEFAULT);

        float x = this.getX();
        float y = this.getY();
        int accent = ColorUtil.getClientColor();

        float maxHealth = Math.max(1.0f, drawTarget.getMaxHealth());
        float health = drawTarget.getHealth();
        float absorption = drawTarget.getAbsorptionAmount();

        animatedHealth += (health - animatedHealth) * 0.15f;
        if (animatedTrailHealth < animatedHealth) {
            animatedTrailHealth = animatedHealth;
        } else {
            animatedTrailHealth += (animatedHealth - animatedTrailHealth) * 0.05f;
        }
        float healthPercent = MathHelper.clamp(animatedHealth / maxHealth, 0.0f, 1.0f);
        float trailPercent = MathHelper.clamp(animatedTrailHealth / maxHealth, 0.0f, 1.0f);

        String name = drawTarget.getName().getString();
        String hpLabel = "HP: ";
        String hpValue = String.format("%.1f", health);
        String absText = absorption > 0.0f ? " (+" + String.format("%.0f", absorption) + ")" : "";
        float hpWidth = hpFont.getStringWidth(hpLabel + hpValue + absText);
        float textBlockWidth = Math.max(nameFont.getStringWidth(name), hpWidth);
        float width = Math.max(MIN_WIDTH, PAD_LEFT + HEAD_SIZE + PAD_TEXT + textBlockWidth + PAD_RIGHT);
        float textX = x + PAD_LEFT + HEAD_SIZE + PAD_TEXT;

        // Items above the panel: armor (helmet first), then main hand and off hand, each group on its own plate.
        float plateY = y - ITEM_PLATE_HEIGHT - 2.5F;
        float itemScale = ITEM_SCALE * anim;
        float itemInset = (ITEM_SLOT - 16.0F * itemScale) * 0.5F;
        float itemInsetY = (ITEM_PLATE_HEIGHT - 16.0F * itemScale) * 0.5F;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int armorCount = 0;
        for (EquipmentSlot slot : armorSlots) {
            if (!drawTarget.getEquippedStack(slot).isEmpty()) {
                armorCount++;
            }
        }
        ItemStack mainHand = drawTarget.getMainHandStack();
        ItemStack offHand = drawTarget.getOffHandStack();
        int handCount = (mainHand.isEmpty() ? 0 : 1) + (offHand.isEmpty() ? 0 : 1);

        if (armorCount > 0) {
            rectangle.render(ShapeProperties.create(matrix, x, plateY, armorCount * ITEM_SLOT, ITEM_PLATE_HEIGHT)
                    .round(3.0f).color(tintedPlateColor(accent, anim)).build());
            int slotIndex = 0;
            for (EquipmentSlot slot : armorSlots) {
                ItemStack stack = drawTarget.getEquippedStack(slot);
                if (stack.isEmpty()) continue;
                drawItem(context, stack, x + slotIndex * ITEM_SLOT + itemInset, plateY + itemInsetY, itemScale);
                slotIndex++;
            }
        }
        if (handCount > 0) {
            float handX = x + armorCount * ITEM_SLOT + (armorCount > 0 ? 2.5F : 0.0F);
            rectangle.render(ShapeProperties.create(matrix, handX, plateY, handCount * ITEM_SLOT, ITEM_PLATE_HEIGHT)
                    .round(3.0f).color(tintedPlateColor(accent, anim)).build());
            int slotIndex = 0;
            if (!mainHand.isEmpty()) {
                drawItem(context, mainHand, handX + slotIndex * ITEM_SLOT + itemInset, plateY + itemInsetY, itemScale);
                slotIndex++;
            }
            if (!offHand.isEmpty()) {
                drawItem(context, offHand, handX + slotIndex * ITEM_SLOT + itemInset, plateY + itemInsetY, itemScale);
            }
        }

        // Panel
        rectangle.render(ShapeProperties.create(matrix, x, y, width, PANEL_HEIGHT)
                .round(4.0f).color(ColorUtil.getColor(7, 7, 9, (int) (255.0F * anim))).build());

        // Head
        float headX = x + PAD_LEFT;
        float headY = y + (PANEL_HEIGHT - HEAD_SIZE) * 0.5F - 3.5F;
        if (drawTarget instanceof PlayerEntity) {
            Render2DUtil.drawTexture(matrix, Render2DUtil.getHeadId(drawTarget), headX, headY, HEAD_SIZE, 2.0f, 1, 1, 8, ColorUtil.getRect(anim));
        } else {
            rectangle.render(ShapeProperties.create(matrix, headX, headY, HEAD_SIZE, HEAD_SIZE)
                    .round(2.0f).color(ColorUtil.getColor(30, 30, 38, (int) (200.0F * anim))).build());
            nameFont.drawCenteredString(matrix, "?", headX + HEAD_SIZE / 2f, headY + 3.0F, ColorUtil.getColor(220, 220, 220, (int) (255.0F * anim)));
        }

        // Name, cut to the width left of the panel edge
        float nameMaxWidth = Math.max(8.0F, width - PAD_LEFT - HEAD_SIZE - PAD_TEXT - PAD_RIGHT);
        while (name.length() > 1 && nameFont.getStringWidth(name) > nameMaxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        nameFont.drawString(matrix, name, textX, y + 5.5F, ColorUtil.getColor(255, 255, 255, (int) (255.0F * anim)));

        // HP text
        float hpY = y + 14.5F;
        int hpColor = ColorUtil.getColor(232, 232, 232, (int) (255.0F * anim));
        hpFont.drawString(matrix, hpLabel, textX, hpY, hpColor);
        float hpValueX = textX + hpFont.getStringWidth(hpLabel);
        hpFont.drawString(matrix, hpValue, hpValueX, hpY, hpColor);
        if (!absText.isEmpty()) {
            hpFont.drawGradientString(matrix, absText, hpValueX + hpFont.getStringWidth(hpValue), hpY,
                    ColorUtil.getColor(236, 183, 39, (int) (255.0F * anim)), ColorUtil.getColor(147, 108, 16, (int) (255.0F * anim)));
        }

        // Health bar along the bottom edge
        float barX = x + 2.0F;
        float barY = y + PANEL_HEIGHT - 7.5F;
        float barWidth = width - 4.0F;
        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth, BAR_HEIGHT)
                .round(2.0f).color(ColorUtil.getColor(30, 30, 38, (int) (200.0F * anim))).build());
        if (trailPercent > healthPercent + 0.01F && barWidth * trailPercent > 1.0F) {
            rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth * trailPercent, BAR_HEIGHT)
                    .round(2.0f).color(ColorUtil.multAlpha(ColorUtil.getClientColorSecondary(), 0.55F * anim)).build());
        }
        if (barWidth * healthPercent > 1.0F) {
            rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth * healthPercent, BAR_HEIGHT)
                    .round(2.0f).color(ColorUtil.multAlpha(accent, anim)).build());
        }

        this.setWidth((int) Math.ceil(width));
        this.setHeight((int) PANEL_HEIGHT);
    }
}
