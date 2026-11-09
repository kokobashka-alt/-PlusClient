package ru.zov.implement.features.draggables;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.module.Module;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.other.StringUtil;

public class PouchHotKeys extends AbstractDraggable {

    private List<Module> keysList = List.of();

    public PouchHotKeys() {
        super("PouchHotKeys", 140, 60, 120, 23, true);
    }

    @Override
    public void tick() {
        keysList = main.getModuleProvider()
                .getModules()
                .stream()
                .filter(module -> module.getAnimation().getOutput().floatValue() != 0.0F
                        && module.getKey() != -1)
                .toList();
    }

    @Override
    public boolean visible() {
        // Если открыт чат, принудительно показываем элемент для перетаскивания
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return true;
        }
        return !keysList.isEmpty();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontItem = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // Заголовок с блюром
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), 17.5F)
                .round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        // Основной фон
        blur.render(ShapeProperties.create(matrix, this.getX(), (float) this.getY() + 17.0F, this.getWidth(), this.getHeight() - 17)
                .round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        // Заголовок по центру
        float centerX = this.getX() + this.getWidth() / 2.0F;
        String header = "Hotkeys";
        font.drawString(matrix, header, (int) (centerX - font.getStringWidth(header) / 2.0F), this.getY() + 7, ColorUtil.getText());

        // Полоска акцента
        rectangle.render(ShapeProperties.create(matrix, this.getX() + 5, this.getY() + 16.5F, this.getWidth() - 10, 1.0F)
                .color(ColorUtil.getClientColor()).build());

        int offset = 23;
        int maxWidth = 120;

        // Если открыт чат и нет биндов, показываем пример, чтобы можно было двигать
        List<Module> drawList = keysList;
        if (drawList.isEmpty() && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            // Рисуем временный текст-заглушку
            fontItem.drawString(matrix, "Пример бинда", this.getX() + 5, this.getY() + offset, ColorUtil.getText(0.5F));
            fontItem.drawString(matrix, "[Z]", this.getX() + this.getWidth() - (int) fontItem.getStringWidth("[Z]") - 5, this.getY() + offset, ColorUtil.getClientColor());
            offset += 13;
        } else {
            for (Module module : drawList) {
                String name = module.getName();
                String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";

                // Название модуля (слева)
                fontItem.drawString(matrix, name, this.getX() + 5, this.getY() + offset, ColorUtil.getText());

                // Бинд (справа)
                int bindWidth = (int) fontItem.getStringWidth(bind);
                fontItem.drawString(matrix, bind, this.getX() + this.getWidth() - bindWidth - 5, this.getY() + offset, ColorUtil.getClientColor());

                int width = (int) fontItem.getStringWidth(name) + bindWidth + 20;
                maxWidth = Math.max(maxWidth, width);
                offset += 13;
            }
        }

        this.setWidth(maxWidth);
        this.setHeight(offset + 5);
    }
}
