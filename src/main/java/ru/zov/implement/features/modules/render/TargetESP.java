package ru.zov.implement.features.modules.render;

import net.minecraft.client.render.Camera;
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
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

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

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vec3d rel = target.getLerpedPos(e.partialTicks()).subtract(camPos);

        int faceColor = ColorUtil.multAlpha(0xFFF7F3EA, appear * 0.95f);
        int redColor = ColorUtil.multAlpha(0xFFD8263A, appear * 0.95f);
        int blackColor = ColorUtil.multAlpha(0xFF16161C, appear * 0.95f);

        // В карте depth true = с учётом глубины (walls=false), false = сквозь стены
        boolean depth = !walls;

        for (int i = 0; i < cardCount; i++) {
            float phase = (float) i / cardCount;
            double angle = time * 0.9f + phase * Math.PI * 2.0;
            double bob = Math.sin(time * 2.1f + i * 1.7f) * 0.12 * targetHeight / 1.8f;
            double y = targetHeight * 0.55 + bob + Math.sin(angle * 2.0 + i) * 0.05;
            Vec3d pos = rel.add(Math.cos(angle) * orbitRadius, y, Math.sin(angle) * orbitRadius);

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

            float hw = CARD_HALF_W * size;
            float hh = CARD_HALF_H * size;

            Vec3d[] corners = new Vec3d[4];
            float[][] local = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
            for (int k = 0; k < 4; k++) {
                float lx = local[k][0];
                float ly = local[k][1];
                float rX = lx * cr - ly * sr;
                float rY = lx * sr + ly * cr;
                float yx = rX * cy;
                float yz = rX * sy;
                float ty = rY * cx - yz * sx;
                float tz = rY * sx + yz * cx;
                corners[k] = pos.add(yx, ty, tz);
            }

            int suitColor = (i % 4 == 1 || i % 4 == 2) ? redColor : blackColor;

            // Контур
            for (int k = 0; k < 4; k++) {
                Render3DUtil.drawLine(corners[k], corners[(k + 1) % 4], faceColor, 1.5f, depth);
            }

            // Внутренний контур
            float innerScale = 0.85f;
            Vec3d[] inner = new Vec3d[4];
            for (int k = 0; k < 4; k++) {
                inner[k] = pos.add(
                        (corners[k].x - pos.x) * innerScale,
                        (corners[k].y - pos.y) * innerScale,
                        (corners[k].z - pos.z) * innerScale
                );
            }
            for (int k = 0; k < 4; k++) {
                Render3DUtil.drawLine(inner[k], inner[(k + 1) % 4], faceColor, 1.0f, depth);
            }

            // Масть
            drawSuit(pos, i % 4, size, suitColor, depth);
        }
    }

    private void drawSuit(Vec3d center, int suit, float size, int color, boolean depth) {
        switch (suit) {
            case 1 -> { // hearts
                float s = size * 0.12f;
                Vec3d leftTop = center.add(-s * 0.7f, s * 0.5f, 0);
                Vec3d rightTop = center.add(s * 0.7f, s * 0.5f, 0);
                Vec3d bottom = center.add(0, -s * 0.6f, 0);
                Render3DUtil.drawLine(leftTop, bottom, color, 1.0f, depth);
                Render3DUtil.drawLine(rightTop, bottom, color, 1.0f, depth);
                Render3DUtil.drawLine(leftTop, rightTop, color, 1.0f, depth);
            }
            case 2 -> { // diamonds
                float s = size * 0.12f;
                Vec3d top = center.add(0, s, 0);
                Vec3d bottom = center.add(0, -s, 0);
                Vec3d left = center.add(-s * 0.6f, 0, 0);
                Vec3d right = center.add(s * 0.6f, 0, 0);
                Render3DUtil.drawLine(top, right, color, 1.0f, depth);
                Render3DUtil.drawLine(right, bottom, color, 1.0f, depth);
                Render3DUtil.drawLine(bottom, left, color, 1.0f, depth);
                Render3DUtil.drawLine(left, top, color, 1.0f, depth);
            }
            case 0 -> { // spades
                float s = size * 0.12f;
                Vec3d top = center.add(0, s, 0);
                Vec3d left = center.add(-s * 0.7f, -s * 0.5f, 0);
                Vec3d right = center.add(s * 0.7f, -s * 0.5f, 0);
                Render3DUtil.drawLine(top, left, color, 1.0f, depth);
                Render3DUtil.drawLine(left, right, color, 1.0f, depth);
                Render3DUtil.drawLine(right, top, color, 1.0f, depth);
                Render3DUtil.drawLine(center.add(0, -s * 0.5f, 0), center.add(0, -s, 0), color, 1.0f, depth);
            }
            case 3 -> { // clubs
                float s = size * 0.09f;
                Vec3d top = center.add(0, s, 0);
                Vec3d left = center.add(-s * 1.2f, -s * 0.6f, 0);
                Vec3d right = center.add(s * 1.2f, -s * 0.6f, 0);
                Vec3d bottom = center.add(0, -s * 1.2f, 0);
                Render3DUtil.drawLine(top, left, color, 1.0f, depth);
                Render3DUtil.drawLine(top, right, color, 1.0f, depth);
                Render3DUtil.drawLine(left, bottom, color, 1.0f, depth);
                Render3DUtil.drawLine(right, bottom, color, 1.0f, depth);
            }
        }
    }

    public LivingEntity getTarget() {
        return target;
    }
                    }
