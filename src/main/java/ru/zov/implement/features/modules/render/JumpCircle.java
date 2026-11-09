package ru.zov.implement.features.modules.render;

import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.utils.render.Render3DUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JumpCircle extends Module {

    public final ValueSetting radius = new ValueSetting(
            "Радиус", "Радиус круга"
    ).setValue(1.5f).range(0.5f, 5.0f);

    public final ValueSetting duration = new ValueSetting(
            "Время жизни", "Время отображения в секундах"
    ).setValue(1.5f).range(0.5f, 5.0f);

    private final List<Circle> circles = new ArrayList<>();

    private boolean wasOnGround = true;

    public JumpCircle() {
        super("JumpCircle", "JumpCircles", ModuleCategory.RENDER);
        this.setup(new Setting[]{radius, duration});
    }

    public static JumpCircle getInstance() {
        return (JumpCircle) Instance.get(JumpCircle.class);
    }

    @Override
    public void onEnable() {
        circles.clear();
        wasOnGround = true;
        super.onEnable();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            circles.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long lifetime = (long) (duration.getValue() * 1000);

        circles.removeIf(circle -> now - circle.time >= lifetime);

        boolean onGround = mc.player.isOnGround();

        if (wasOnGround && !onGround
                && !mc.player.isTouchingWater()) {

            Vec3d position = mc.player.getEntityPos()
                    .add(0.0, 0.05, 0.0);

            circles.add(new Circle(position, now));
        }

        wasOnGround = onGround;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (circles.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lifetime = (long) (duration.getValue() * 1000);

        Iterator<Circle> iterator = circles.iterator();

        while (iterator.hasNext()) {
            Circle circle = iterator.next();

            float progress = (float) (now - circle.time) / lifetime;

            if (progress >= 1.0f) {
                iterator.remove();
                continue;
            }

            float currentRadius = radius.getValue() * (0.5f + progress * 0.5f);
            float alpha = 1.0f - progress;

            int color = ((int) (alpha * 255) << 24) | 0x00A855F7;

            int segments = 64;

            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI * 2.0 * i / segments;
                double angle2 = Math.PI * 2.0 * (i + 1) / segments;

                Vec3d start = circle.position.add(
                        Math.cos(angle1) * currentRadius,
                        0,
                        Math.sin(angle1) * currentRadius
                );

                Vec3d end = circle.position.add(
                        Math.cos(angle2) * currentRadius,
                        0,
                        Math.sin(angle2) * currentRadius
                );

                Render3DUtil.drawLineSmooth(
                        start,
                        end,
                        color,
                        2.0f,
                        false
                );
            }
        }
    }

    private static class Circle {
        private final Vec3d position;
        private final long time;

        private Circle(Vec3d position, long time) {
            this.position = position;
            this.time = time;
        }
    }
}
