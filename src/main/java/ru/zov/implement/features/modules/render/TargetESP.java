
package ru.zov.implement.features.modules.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class TargetESP extends Module {

    public final BooleanSetting chains =
            new BooleanSetting("Цепи", "Рисовать кольца вокруг цели")
                    .setValue(true);

    public final ValueSetting radius =
            new ValueSetting("Радиус", "Размер колец")
                    .setValue(0.7f)
                    .range(0.3f, 2.0f);

    public final ValueSetting speed =
            new ValueSetting("Скорость", "Скорость вращения")
                    .setValue(1.0f)
                    .range(0.1f, 3.0f);

    public final ValueSetting height =
            new ValueSetting("Высота", "Высота колец")
                    .setValue(0.5f)
                    .range(0.1f, 1.5f);

    private LivingEntity target;
    private long lastHitTime;
    private float fade;
    private float rotation;

    public TargetESP() {
        super("TargetESP", "Target ESP", ModuleCategory.RENDER);
        this.setup(new Setting[]{chains, radius, speed, height});
    }

    public static TargetESP getInstance() {
        return (TargetESP) Instance.get(TargetESP.class);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            target = null;
            fade = 0;
            return;
        }

        if (target != null && (!target.isAlive() || target.isRemoved())) {
            target = null;
        }

        boolean shouldShow = target != null
                && System.currentTimeMillis() - lastHitTime < 3000;

        if (shouldShow) {
            fade += (1.0f - fade) * 0.15f;
        } else {
            fade -= fade * 0.15f;

            if (fade < 0.03f) {
                fade = 0;
            }
        }

        rotation += speed.getValue() * 0.05f;
    }

    public void onHit(Entity entity) {
        if (entity instanceof LivingEntity living && living.isAlive()) {
            target = living;
            lastHitTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!chains.isValue() || fade <= 0.03f || target == null) {
            return;
        }

        if (mc.player == null || mc.world == null) {
            return;
        }

        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();

        Vec3d center = target.getLerpedPos(e.partialTicks())
                .add(0, target.getHeight() * height.getValue(), 0)
                .subtract(camera);

        float r = radius.getValue();

        drawRing(center, r, rotation, -0.3f, 0xFF00BFFF);
        drawRing(center, r * 0.8f, -rotation * 0.7f, 0.0f, 0xFFFFFFFF);
        drawRing(center, r * 0.6f, rotation * 1.3f, 0.3f, 0xFF8A2BE2);
    }

    private void drawRing(
            Vec3d center,
            float ringRadius,
            float angle,
            float yOffset,
            int color
    ) {
        int segments = 60;

        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments + angle;
            double a2 = Math.PI * 2 * (i + 1) / segments + angle;

            Vec3d start = center.add(
                    Math.cos(a1) * ringRadius,
                    yOffset,
                    Math.sin(a1) * ringRadius
            );

            Vec3d end = center.add(
                    Math.cos(a2) * ringRadius,
                    yOffset,
                    Math.sin(a2) * ringRadius
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

    public LivingEntity getTarget() {
        return target;
    }
}
