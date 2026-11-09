package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import ru.zov.implement.features.modules.render.Hud;
import ru.zov.implement.features.modules.render.SystemHud;

public class SystemUseTracker extends AbstractDraggable {
    private final List<TrackedPlayer> trackedPlayers = new ArrayList<>();

    public SystemUseTracker() {
        super("System Use Tracker", 200, 100, 100, 23, true);
    }

    public boolean visible() {
        return !this.trackedPlayers.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    public void tick() {
        // Проверяем, включен ли наш новый SystemHud
        if (SystemHud.getInstance() == null || !SystemHud.getInstance().isState()) {
            trackedPlayers.clear();
            return;
        }

        if (mc.world == null || mc.player == null) return;

        List<PlayerEntity> currentUsers = new ArrayList<>();
        
        // Ищем всех игроков в радиусе 30 блоков, кто использует предмет
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isDead()) continue;
            
            if (player.isUsingItem() && !player.getActiveItem().isEmpty()) {
                ItemStack item = player.getActiveItem();
                // Нас интересуют только еда, зелья и щиты
                if (item.isFood() || item.isOf(Items.POTION) || item.isOf(Items.SHIELD) || item.isOf(Items.GOLDEN_APPLE) || item.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                    if (player.distanceTo(mc.player) <= 30.0f) {
                        currentUsers.add(player);
                    }
                }
            }
        }

        // Обновляем список с анимациями
        for (PlayerEntity user : currentUsers) {
            TrackedPlayer tracked = trackedPlayers.stream()
                .filter(t -> t.player.getUuid().equals(user.getUuid()))
                .findFirst().orElse(null);
                
            if (tracked == null) {
                trackedPlayers.add(new TrackedPlayer(user, new DecelerateAnimation().setMs(150).setValue(1.0)));
            } else {
                tracked.player = user;
                tracked.anim.setDirection(Direction.FORWARDS);
            }
        }

        // Убираем тех, кто перестал использовать
        trackedPlayers.removeIf(tracked -> {
            boolean stillUsing = currentUsers.stream().anyMatch(u -> u.getUuid().equals(tracked.player.getUuid()));
            if (!stillUsing) {
                tracked.anim.setDirection(Direction.BACKWARDS);
            }
            return tracked.anim.isFinished(Direction.BACKWARDS);
        });
    }

    public void drawDraggable(DrawContext context) {
        org.joml.Matrix3x2fStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // Отрисовка заголовка
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), 17.5F)
            .round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F)
            .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
            
        blur.render(ShapeProperties.create(matrix, this.getX(), this.getY() + 17, this.getWidth(), this.getHeight() - 17)
            .round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F)
            .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        float centerX = this.getX() + this.getWidth() / 2.0F;
        font.drawCenteredString(matrix, "Use Tracker", centerX, this.getY() + 7, ColorUtil.getText());

        int offset = 23;
        int maxWidth = 100;

        for (TrackedPlayer tracked : trackedPlayers) {
            PlayerEntity player = tracked.player;
            ItemStack item = player.getActiveItem();
            
            float anim = tracked.anim.getOutput().floatValue();
            float centerY = this.getY() + offset;
            
            // Считаем прогресс использования (от 0 до 1)
            float maxUseTime = item.getMaxUseTime(player);
            float useProgress = 1.0f;
            if (maxUseTime > 0) {
                useProgress = 1.0f - ((float)player.getItemUseTimeLeft() / maxUseTime);
            }
            useProgress = MathHelper.clamp(useProgress, 0.0f, 1.0f);

            // Анимация появления
            MathUtil.scale(matrix, centerX, centerY, 1.0F, anim, () -> {
                // Иконка предмета
                Render2DUtil.defaultDrawStack(context, item, this.getX() + 5, centerY - 2.0f, false, true, 0.7F);
                
                // Разделитель
                rectangle.render(ShapeProperties.create(matrix, this.getX() + 15.5F, centerY - 1.0f, 0.5F, 7.0F)
                    .color(ColorUtil.getOutline(0.75F, 0.5F)).build());

                // Имя игрока
                String name = player.getName().getString();
                fontPlayer.drawString(matrix, name, this.getX() + 20, centerY + 1.0f, ColorUtil.getText());

                // Название предмета или действие
                String action = getActionText(item);
                fontPlayer.drawString(matrix, action, this.getX() + 20, centerY + 9.0f, ColorUtil.getText(0.6f));

                // Полоска прогресса использования
                float barWidth = 60.0f;
                float barY = centerY + 16.0f;
                
                // Фон полоски
                rectangle.render(ShapeProperties.create(matrix, this.getX() + 20, barY, barWidth, 1.5F)
                    .round(0.75F).color(ColorUtil.getRectDarker(0.5F)).build());
                    
                // Заполнение полоски
                rectangle.render(ShapeProperties.create(matrix, this.getX() + 20, barY, barWidth * useProgress, 1.5F)
                    .round(0.75F).color(ColorUtil.getClientColor()).build());
            });

            // Рассчитываем ширину для авто-расширения
            int width = (int)Math.max(fontPlayer.getStringWidth(player.getName().getString()) + 35.0F, 100.0F);
            maxWidth = Math.max(width, maxWidth);
            offset += (int)(24.0F * anim); // Высота одного элемента с учетом анимации
        }

        this.setWidth(maxWidth);
        this.setHeight(offset);
    }

    private String getActionText(ItemStack item) {
        if (item.isFood()) {
            if (item.isOf(Items.GOLDEN_APPLE)) return "Ест голд яблоко";
            if (item.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return "Ест зачар. яблоко";
            return "Ест еду";
        }
        if (item.isOf(Items.POTION)) return "Пьет зелье";
        if (item.isOf(Items.SHIELD)) return "Блокирует щитом";
        if (item.isOf(Items.BOW)) return "Натягивает лук";
        if (item.isOf(Items.CROSSBOW)) return "Заряжает арбалет";
        return "Использует предмет";
    }

    private static class TrackedPlayer {
        private PlayerEntity player;
        private final Animation anim;

        private TrackedPlayer(PlayerEntity player, Animation anim) {
            this.player = player;
            this.anim = anim;
        }
    }
}
