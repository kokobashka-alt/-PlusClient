package ru.zov.implement.features.modules.render;

import java.util.Arrays;

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

public class TargetESP extends Module {
    private static final float ANIM_STEP = 0.15F;
    private static final long HIT_SHOW_MS = 3000L;

    private final SelectSetting mode =
            new SelectSetting("Режим", "Стиль Target ESP")
                    .value("Ромб", "Кольцо", "Души", "Цепи", "Карты");

    private final BooleanSetting hurtColor =
            new BooleanSetting("Окрашивание при ударе", "Краснеет, когда цель получает урон")
                    .setValue(true);

    private final BooleanSetting throughWalls =
            new BooleanSetting("Сквозь стены", "Рисовать эффект поверх блоков")
                    .setValue(false);

    private final ValueSetting radius =
            new ValueSetting("Радиус", "Размер колец")
                    .setValue(0.7f)
                    .range(0.3f, 2.0f)
                    .visible(() -> mode.isSelected("Цепи") || mode.isSelected("Карты"));

    private final ValueSetting speed =
            new ValueSetting("Скорость", "Скорость вращения")
                    .setValue(1.0f)
                    .range(0.1f, 3.0f)
                    .visible(() -> mode.isSelected("Цепи") || mode.isSelected("Карты"));

    private final ValueSetting height =
            new ValueSetting("Высота", "Высота колец")
                    .setValue(0.5f)
                    .range(0.1f, 1.5f)
                    .visible(() -> mode.isSelected("Цепи") || mode.isSelected("Карты"));

    private LivingEntity target;
    private long lastHitTime;
    private float anim;
    private float prevAnim;
    private float rotation;

    // Карты
    private float cardsElapsed;
    private long cardsLastMs;
    private static final float CARD_HALF_W = 0.31f;
    private static final float CARD_HALF_H = 0.44f;

    public TargetESP() {
        super("TargetESP", "Target ESP", ModuleCategory.RENDER);
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
            case "Карты" -> drawCards(e, shown, red, walls, color);
            default -> {
            }
        }
    }

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

        drawRing(center, r, rotation, -0.3f, ColorUtil.multAlpha(0xFF00BFFF, shown), !throughWalls.isValue());
        drawRing(center, r * 0.8f, -rotation * 0.7f, 0.0f, ColorUtil.multAlpha(0xFFFFFFFF, shown), !throughWalls.isValue());
        drawRing(center, r * 0.6f, rotation * 1.3f, 0.3f, ColorUtil.multAlpha(0xFF8A2BE2, shown), !throughWalls.isValue());
    }

    private void drawRing(Vec3d center, float ringRadius, float angle, float yOffset, int color, boolean depth) {
        int segments = 60;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments + angle;
            double a2 = Math.PI * 2 * (i + 1) / segments + angle;

            Vec3d start = center.add(Math.cos(a1) * ringRadius, yOffset, Math.sin(a1) * ringRadius);
            Vec3d end = center.add(Math.cos(a2) * ringRadius, yOffset, Math.sin(a2) * ringRadius);

            Render3DUtil.drawLine(start, end, color, 2.0f, depth);
        }
    }

    // === Карты ===

    private void drawCards(WorldRenderEvent e, float shown, float red, boolean walls, int accent) {
        long now = System.currentTimeMillis();
        float delta = cardsLastMs == 0L ? 0f : Math.min(0.1f, (now - cardsLastMs) / 1000f);
        cardsLastMs = now;
        cardsElapsed += delta * speed.getValue();

        int cardCount = 4;
        float appear = shown;
        float targetHeight = target.getHeight();
        float size = MathHelper.clamp(targetHeight / 1.8f, 0.6f, 1.6f) * (0.55f + 0.45f * appear);
        double orbitRadius = (target.getWidth() * 0.9f + 0.55f) * (0.7f + 0.3f * appear);
        float hitSpin = red * red * 360f;
        float time = cardsElapsed;

        // Render3DUtil queues use the world-space matrix that already subtracts the camera,
        // so card geometry must be in world coordinates.
        Vec3d base = target.getLerpedPos(e.partialTicks());
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        boolean depth = !walls;

        Vec3d[] positions = new Vec3d[cardCount];
        Vec3d[] axisU = new Vec3d[cardCount];
        Vec3d[] axisV = new Vec3d[cardCount];
        for (int i = 0; i < cardCount; i++) {
            float phase = (float) i / cardCount;
            double angle = time * 0.9f + phase * Math.PI * 2.0;
            double bob = Math.sin(time * 2.1f + i * 1.7f) * 0.12 * targetHeight / 1.8f;
            double y = targetHeight * 0.55 + bob + Math.sin(angle * 2.0 + i) * 0.05;
            positions[i] = base.add(Math.cos(angle) * orbitRadius, y, Math.sin(angle) * orbitRadius);

            float yaw = (float) Math.atan2(Math.cos(angle), Math.sin(angle));
            float wobble = (float) Math.toRadians(Math.sin(time * 1.3f + i * 2.3f) * 22.0f + hitSpin);
            float tilt = (float) Math.toRadians(Math.sin(time * 1.7f + i * 0.9f) * 12.0f - 8.0f);
            float roll = (float) Math.toRadians(Math.sin(time * 1.1f + i * 1.3f) * 14.0f);

            float cy = (float) Math.cos(yaw + wobble);
            float sy = (float) Math.sin(yaw + wobble);
            float cx = (float) Math.cos(tilt);
            float sx = (float) Math.sin(tilt);
            float cr = (float) Math.cos(roll);
            float sr = (float) Math.sin(roll);

            axisU[i] = new Vec3d(cr * cy, sr * cx - cr * sy * sx, sr * sx + cr * sy * cx);
            axisV[i] = new Vec3d(-sr * cy, cr * cx + sr * sy * sx, cr * sx - sr * sy * cx);
        }

        Integer[] order = new Integer[cardCount];
        for (int i = 0; i < cardCount; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (p, q) -> Double.compare(positions[q].squaredDistanceTo(camera), positions[p].squaredDistanceTo(camera)));

        for (int index : order) {
            drawCard(positions[index], axisU[index], axisV[index], index % 4, size, appear, accent, camera, depth);
        }
    }

    private void drawCard(Vec3d pos, Vec3d u, Vec3d v, int suit, float size, float appear, int accent, Vec3d camera, boolean depth) {
        Vec3d normal = u.crossProduct(v).normalize();
        boolean front = normal.dotProduct(camera.subtract(pos)) >= 0.0;
        Vec3d dir = front ? normal : normal.multiply(-1.0);

        double hw = CARD_HALF_W * size;
        double hh = CARD_HALF_H * size;
        double rim = 0.014 * size;
        float alpha = appear * 0.95f;
        boolean redSuit = suit == 1 || suit == 2;
        int suitColor = ColorUtil.multAlpha(redSuit ? 0xFFD8263A : 0xFF16161C, alpha);

        cardRect(pos, u, v, dir, 0, -hw - rim, -hh - rim, hw + rim, hh + rim, ColorUtil.multAlpha(0xFF101018, alpha), depth);
        if (front) {
            cardRect(pos, u, v, dir, 1, -hw, -hh, hw, hh, ColorUtil.multAlpha(0xFFF7F3EA, alpha), depth);
            cardFrame(pos, u, v, dir, 2, hw * 0.84, hh * 0.88, 0.014 * size, suitColor, depth);
            cardSuit(pos, u, v, dir, 3, suit, 0.0, 0.0, 0.25 * size, false, suitColor, depth);
            cardSuit(pos, u, v, dir, 3, suit, -hw * 0.62, hh * 0.68, 0.075 * size, false, suitColor, depth);
            cardSuit(pos, u, v, dir, 3, suit, hw * 0.62, -hh * 0.68, 0.075 * size, true, suitColor, depth);
        } else {
            int backColor = ColorUtil.multAlpha(0xFF23285A, alpha);
            int pattern = ColorUtil.multAlpha(accent, alpha * 0.85f);
            cardRect(pos, u, v, dir, 1, -hw, -hh, hw, hh, backColor, depth);
            cardFrame(pos, u, v, dir, 2, hw * 0.84, hh * 0.88, 0.014 * size, pattern, depth);
            cardSuit(pos, u, v, dir, 3, 2, 0.0, 0.0, 0.3 * size, false, pattern, depth);
            cardSuit(pos, u, v, dir, 4, 2, 0.0, 0.0, 0.17 * size, false, backColor, depth);
        }
    }

    private static Vec3d cardPoint(Vec3d pos, Vec3d u, Vec3d v, Vec3d dir, int layer, double a, double b) {
        return pos.add(u.multiply(a)).add(v.multiply(b)).add(dir.multiply(layer * 0.0035));
    }

    private void cardRect(Vec3d pos, Vec3d u, Vec3d v, Vec3d dir, int layer, double x0, double y0, double x1, double y1, int color, boolean depth) {
        Render3DUtil.drawQuad(
                cardPoint(pos, u, v, dir, layer, x0, y0),
                cardPoint(pos, u, v, dir, layer, x1, y0),
                cardPoint(pos, u, v, dir, layer, x1, y1),
                cardPoint(pos, u, v, dir, layer, x0, y1),
                color, depth);
    }

    private void cardFrame(Vec3d pos, Vec3d u, Vec3d v, Vec3d dir, int layer, double hw, double hh, double t, int color, boolean depth) {
        cardRect(pos, u, v, dir, layer, -hw, hh - t, hw, hh, color, depth);
        cardRect(pos, u, v, dir, layer, -hw, -hh, hw, -hh + t, color, depth);
        cardRect(pos, u, v, dir, layer, -hw, -hh + t, -hw + t, hh - t, color, depth);
        cardRect(pos, u, v, dir, layer, hw - t, -hh + t, hw, hh - t, color, depth);
    }

    private void cardFan(Vec3d pos, Vec3d u, Vec3d v, Vec3d dir, int layer, double cx, double cy, double[][] points, double ox, double oy, double scale, double sign, int color, boolean depth) {
        Vec3d center = cardPoint(pos, u, v, dir, layer, ox + cx * scale * sign, oy + cy * scale * sign);
        for (int k = 0; k < points.length; k++) {
            double[] p = points[k];
            double[] q = points[(k + 1) % points.length];
            Vec3d a = cardPoint(pos, u, v, dir, layer, ox + p[0] * scale * sign, oy + p[1] * scale * sign);
            Vec3d b = cardPoint(pos, u, v, dir, layer, ox + q[0] * scale * sign, oy + q[1] * scale * sign);
            Render3DUtil.drawQuad(center, a, b, b, color, depth);
        }
    }

    private static double[][] circle(double cx, double cy, double radius, int n) {
        double[][] pts = new double[n][2];
        for (int k = 0; k < n; k++) {
            double ang = Math.PI * 2.0 * k / n;
            pts[k][0] = cx + Math.cos(ang) * radius;
            pts[k][1] = cy + Math.sin(ang) * radius;
        }
        return pts;
    }

    private static double[][] heart(boolean flipped) {
        int n = 28;
        double[][] pts = new double[n][2];
        for (int k = 0; k < n; k++) {
            double t = Math.PI * 2.0 * k / n;
            double x = 16.0 * Math.pow(Math.sin(t), 3) / 17.0;
            double y = (13.0 * Math.cos(t) - 5.0 * Math.cos(2 * t) - 2.0 * Math.cos(3 * t) - Math.cos(4 * t) + 2.5) / 17.0;
            pts[k][0] = x;
            pts[k][1] = flipped ? -y : y;
        }
        return pts;
    }

    private void cardSuit(Vec3d pos, Vec3d u, Vec3d v, Vec3d dir, int layer, int suit, double ox, double oy, double scale, boolean rotated, int color, boolean depth) {
        double sign = rotated ? -1.0 : 1.0;
        switch (suit) {
            case 1 -> cardFan(pos, u, v, dir, layer, 0.0, 0.05, heart(false), ox, oy, scale, sign, color, depth);
            case 2 -> cardFan(pos, u, v, dir, layer, 0.0, 0.0,
                    new double[][]{{0.0, 0.95}, {0.62, 0.0}, {0.0, -0.95}, {-0.62, 0.0}}, ox, oy, scale, sign, color, depth);
            case 0 -> {
                cardFan(pos, u, v, dir, layer, 0.0, -0.05, heart(true), ox, oy, scale, sign, color, depth);
                cardFan(pos, u, v, dir, layer, 0.0, -0.8,
                        new double[][]{{0.0, -0.4}, {-0.24, -1.0}, {0.24, -1.0}}, ox, oy, scale, sign, color, depth);
            }
            default -> {
                cardFan(pos, u, v, dir, layer, 0.0, 0.4, circle(0.0, 0.42, 0.36, 14), ox, oy, scale, sign, color, depth);
                cardFan(pos, u, v, dir, layer, -0.4, -0.2, circle(-0.4, -0.2, 0.36, 14), ox, oy, scale, sign, color, depth);
                cardFan(pos, u, v, dir, layer, 0.4, -0.2, circle(0.4, -0.2, 0.36, 14), ox, oy, scale, sign, color, depth);
                cardFan(pos, u, v, dir, layer, 0.0, -0.6,
                        new double[][]{{0.0, -0.1}, {-0.2, -1.0}, {0.2, -1.0}}, ox, oy, scale, sign, color, depth);
            }
        }
    }

    public LivingEntity getTarget() {
        return target;
    }
                    }
