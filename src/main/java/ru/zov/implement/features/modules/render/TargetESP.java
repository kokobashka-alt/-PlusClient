package ru.zov.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class TargetESP extends Module {

    public final BooleanSetting chains = new BooleanSetting("Цепи", "Рисовать кольца вокруг цели").setValue(true);
    public final ValueSetting radius = new ValueSetting("Радиус", "Размер кольца").setValue(0.7f).range(0.3f, 2.0f);
    public final ValueSetting speed = new ValueSetting("Скорость", "Скорость вращения").setValue(1.0f).range(0.1f, 3.0f);
    public final ValueSetting height = new ValueSetting("Высота", "Высота колец").setValue(0.5f).range(0.1f, 1.5f);

    private LivingEntity target;
    private long lastHitTime;
    private float fade;
    private float rotation;

    public TargetESP() {
        super("TargetESP", "targetцепи333", ModuleCategory.RENDER);
        this.setup(new Setting[]{chains, radius, speed, height});
    }

    public static TargetESP getInstance() {
        return (TargetESP) Instance.get(TargetESP.class);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (target != null && (!target.isAlive() || target.isRemoved())) {
            target = null;
        }

        long timeSinceHit = System.currentTimeMillis() - lastHitTime;
        boolean shouldShow = target != null && (timeSinceHit < 3000); // 3 секунды после удара

        if (shouldShow) {
            fade += (1.0f - fade) * 0.15f;
        } else {
            fade -= fade * 0.15f;
            if (fade < 0.03f) fade = 0f;
        }

        rotation += speed.getValue() * 2.0f;
    }

    public void onHit(Entity entity) {
        if (entity instanceof LivingEntity living && living.isAlive()) {
            this.target = living;
            this.lastHitTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (fade <= 0.03f || target == null) return;

        MatrixStack matrixStack = e.getMatrixStack();
        float tickDelta = e.getTickDelta();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        
        // Центр цели
        Vec3d targetPos = target.getLerpedPos(tickDelta).add(0, target.getHeight() * height.getValue(), 0);
        double x = targetPos.x - camPos.x;
        double y = targetPos.y - camPos.y;
        double z = targetPos.z - camPos.z;

        matrixStack.push();
        matrixStack.translate(x, y, z);

        // Настройка рендера линий
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // Видно сквозь стены
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.0f); // Толщина линии

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        int segments = 60; // Количество точек в круге
        float r = radius.getValue();
        
        // Вращаем кольца в разные стороны для эффекта
        float angleOffset1 = rotation;
        float angleOffset2 = -rotation * 0.7f;
        float angleOffset3 = rotation * 1.3f;

        int alpha = (int) (fade * 255);
        
        // Рисуем 3 кольца на разной высоте
        drawRing(buffer, matrix, r, angleOffset1, -0.3f, alpha, 0x00BFFF); // Голубое сверху
        drawRing(buffer, matrix, r * 0.8f, angleOffset2, 0.0f, alpha, 0xFFFFFF); // Белое посередине
        drawRing(buffer, matrix, r * 0.6f, angleOffset3, 0.3f, alpha, 0x8A2BE2); // Фиолетовое снизу

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrixStack.pop();
    }

    private void drawRing(BufferBuilder buffer, Matrix4f matrix, float radius, float rotation, float yOffset, int alpha, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        int segments = 60;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (Math.PI * 2 * i / segments) + rotation;
            float angle2 = (float) (Math.PI * 2 * (i + 1) / segments) + rotation;

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            buffer.vertex(matrix, x1, yOffset, z1).color(r, g, b, alpha);
            buffer.vertex(matrix, x2, yOffset, z2).color(r, g, b, alpha);
        }
    }

    public LivingEntity getTarget() {
        return target;
    }
}
