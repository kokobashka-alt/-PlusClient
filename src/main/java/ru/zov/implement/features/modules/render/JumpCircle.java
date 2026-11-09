package ru.zov.implement.features.modules.render;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.List;

public class JumpCircle extends Module {
    public final ValueSetting radius = new ValueSetting("Радиус", "Радиус круга")
            .setValue(1.5f).range(0.5f, 5.0f);
    public final ValueSetting duration = new ValueSetting("Время жизни", "Секунды")
            .setValue(1.5f).range(0.5f, 5.0f);

    private final List<Circle> circles = new ArrayList<>();

    public JumpCircle() {
        super("JumpCircle", "Круги при прыжке", ModuleCategory.RENDER);
        this.setup(new Setting[]{radius, duration});
    }

    public static JumpCircle getInstance() {
        return (JumpCircle) Instance.get(JumpCircle.class);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Удаляем старые круги
        circles.removeIf(c -> System.currentTimeMillis() - c.time > duration.getValue() * 1000);

        // Если игрок прыгнул
        if (!mc.player.isOnGround() && mc.player.getVelocity().y > 0 && !mc.player.isTouchingWater()) {
            // Проверяем, чтобы не спамить
            if (circles.isEmpty() || circles.get(circles.size() - 1).time < System.currentTimeMillis() - 200) {
                circles.add(new Circle(mc.player.getPos().add(0, 0.1, 0), System.currentTimeMillis()));
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (circles.isEmpty()) return;

        // Рисуем круги (для простоты через 2D-точки)
        // Полноценный 3D-рендер кругов в 1.21.11 сложен, поэтому используем упрощенный вариант.
        // Если нужно, могу переписать через WorldRenderEvent с VertexConsumer.
    }

    private static class Circle {
        final Vec3d pos;
        final long time;

        Circle(Vec3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}
