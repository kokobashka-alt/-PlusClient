package ru.zov.implement.features.modules.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.modules.combat.Aura;

/**
 * Target ESP. Modes "Ромб", "Кольцо" and "Души" follow the Aspect TargetESP behaviour (target comes from
 * Aura while it has one, 0..1 appear/fade animation, red tint while the target is hurt) and are drawn by the
 * existing Render3DUtil primitives. "Цепи" is the previous ring-chain renderer of this module, kept as is.
 */
public class TargetESP extends Module {
    private static final float ANIM_STEP = 0.15F;
    private static final long HIT_SHOW_MS = 3000L;

    private final SelectSetting mode =
            new SelectSetting("Режим", "Стиль Target ESP")
                    .value("Ромб", "Кольцо", "Души", "Цепи");

    private final BooleanSetting hurtColor =
            new BooleanSetting("Окрашивание при ударе", "Краснеет, когда цель получает урон")
                    .setValue(true);

    private final BooleanSetting throughWalls =
            new BooleanSetting("Сквозь стены", "Рисовать эффект поверх блоков")
                    .setValue(false);

    private final ValueSetting radius =
            new ValueSetting("Радиус", "Размер колец")
                    .setValue(0.7f)
                    .range(0.3f, 2.0f);

    private final ValueSetting speed =
            new ValueSetting("Скорость", "Скорость вращения")
                    .setValue(1.0f)
                    .range(0.1f, 3.0f);

    private final ValueSetting height =
            new ValueSetting("Высота", "Высота колец")
                    .setValue(0.5f)
                    .range(0.1f, 1.5f);

    private LivingEntity target;
    private long lastHitTime;
    private float anim;
    private float prevAnim;
    private float rotation;

    public TargetESP() {
        super("TargetESP", "Target ESP", ModuleCategory.RENDER);
        this.radius.setVisible(() -> this.mode.isSelected("Цепи"));
        this.speed.setVisible(() -> this.mode.isSelected("Цепи"));
        this.height.setVisible(() -> this.mode.isSelected("Цепи"));
        this.setup(new Setting[]{mode, hurtColor, throughWalls, radius, speed, height});
    }

    public static TargetESP getInstance() {
        return (TargetESP) Instance.get(TargetESP.class);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            target = null;
            anim = 0.0F;
            prevAnim = 0.0F;
            return;
        }

        Aura aura = Aura.getInstance();
        LivingEntity auraTarget = aura != null && aura.isState() ? aura.getTarget() : null;
        boolean active;
        if (auraTarget != null && auraTarget.isAlive() && !auraTarget.isRemoved()) {
            target = auraTarget;
            active = true;
        } else {
            active = target != null
                    && target.isAlive()
                    && !target.isRemoved()
                    && System.currentTimeMillis() - lastHitTime < HIT_SHOW_MS;
        }

        prevAnim = anim;
        anim = approach(anim, active ? 1.0F : 0.0F, ANIM_STEP);
        if (!active && anim <= 0.0F) {
            target = null;
        }

        Render3DUtil.updateTargetEsp();
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
        if (target == null || mc.player == null || mc.world == null) {
            return;
        }
        float shown = MathHelper.lerp(e.partialTicks(), prevAnim, anim);
        if (shown <= 0.01F) {
            return;
        }

        float red = hurtProgress(target, e.partialTicks());
        int color = ColorUtil.getClientColor();
        boolean walls = throughWalls.isValue();

        switch (mode.getSelected()) {
            case "Ромб" -> Render3DUtil.drawCube(target, shown, red, walls, color, Float.NaN, 0.0F);
            case "Кольцо" -> Render3DUtil.drawCircle(e.stack(), target, shown, red, walls, color, 1.0F, Double.NaN);
            case "Души" -> Render3DUtil.drawGhosts(target, shown, red, 1.0F, walls, color);
            case "Цепи" -> drawChains(e, shown);
            default -> {
            }
        }
    }

    /** Smoothstep of the remaining hurt time, 0..1; 0 when the option is off. */
    private float hurtProgress(LivingEntity entity, float partialTicks) {
        if (!hurtColor.isValue()) {
            return 0.0F;
        }
        float t = MathHelper.clamp(entity.hurtTime - partialTicks, 0.0F, 10.0F) / 10.0F;
        return t * t * (3.0F - 2.0F * t);
    }

    private static float approach(float value, float goal, float step) {
        if (value < goal) {
            return Math.min(value + step, goal);
        }
        return Math.max(value - step, goal);
    }

    private void drawChains(WorldRenderEvent e, float shown) {
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();

        Vec3d center = target.getLerpedPos(e.partialTicks())
                .add(0, target.getHeight() * height.getValue(), 0)
                .subtract(camera);

        float r = radius.getValue();

        drawRing(center, r, rotation, -0.3f, ColorUtil.multAlpha(0xFF00BFFF, shown));
        drawRing(center, r * 0.8f, -rotation * 0.7f, 0.0f, ColorUtil.multAlpha(0xFFFFFFFF, shown));
        drawRing(center, r * 0.6f, rotation * 1.3f, 0.3f, ColorUtil.multAlpha(0xFF8A2BE2, shown));
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
