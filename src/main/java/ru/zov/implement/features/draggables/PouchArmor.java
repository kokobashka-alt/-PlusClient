package ru.zov.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class PouchArmor extends AbstractDraggable {

    private final EquipmentSlot[] slots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public PouchArmor() {
        super("PouchArmorStatus", 10, 150, 20, 100, true);
    }

    @Override
    public boolean visible() {
        return mc.player != null;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);

        float startX = this.getX();
        float startY = this.getY();
        float width = 20f;
        float height = 20f;
        float gap = 2f;

        int index = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            float y = startY + index * (height + gap);

            // Фон (черный с прозрачностью)
            blur.render(ShapeProperties.create(matrix, startX, y, width, height)
                    .round(3.0F).color(ColorUtil.getRect(0.7F)).build());

            // Иконка предмета
            context.drawItem(stack, (int)startX + 2, (int)y + 2);

            // Прочность (полоска снизу)
            if (stack.isDamageable()) {
                float durability = 1.0f - (float)stack.getDamage() / stack.getMaxDamage();
                int barColor = durability > 0.5f ? ColorUtil.getColor(80, 220, 80) : 
                               durability > 0.25f ? ColorUtil.getColor(255, 220, 50) : 
                               ColorUtil.getColor(220, 50, 50);

                // Фон полоски
                rectangle.render(ShapeProperties.create(matrix, startX + 2, y + height - 2, width - 4, 1.5f)
                        .color(ColorUtil.getRectDarker(0.5F)).build());
                // Заполнение
                rectangle.render(ShapeProperties.create(matrix, startX + 2, y + height - 2, (width - 4) * durability, 1.5f)
                        .color(barColor).build());
            }
            index++;
        }
        this.setWidth((int) width);
        this.setHeight((int) (index * (height + gap)));
    }
}
