package ru.zov.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class TargetESP extends Module {

    // Текстура черепа (путь: src/main/resources/assets/zov/textures/target_skull.png)
    private static final Identifier SKULL_TEXTURE = Identifier.of("zov", "textures/target_skull.png");

    public final SelectSetting effect = new SelectSetting("Эффект", "Тип отрисовки")
            .value("Skull", "Ring", "Triangle")
            .selected("Skull");

    public final BooleanSetting look = new BooleanSetting("Цвет", "Настройки цвета").setValue(true);
    public final ValueSetting colorSetting = new ValueSetting("Цвет", "Цвет ESP")
            .setValue(0.5f).range(0f, 1f);

    public final BooleanSetting shape = new BooleanSetting("Форма", "Настройки размера").setValue(true);
    public final ValueSetting radius = new ValueSetting("Радиус", "Размер черепа")
            .setValue(1.2f).range(0.4f, 3.0f);

    public final BooleanSetting timing = new BooleanSetting("Тайминг", "Время показа").setValue(true);
    public final ValueSetting hold = new ValueSetting("Держать", "Секунд после удара")
            .setValue(2.3f).range(0.3f, 10.0f);

    private LivingEntity target;
    private long lastHitTime;
    private float fade;

    public TargetESP() {
        super("TargetESP", "3D ESP вокруг цели", ModuleCategory.RENDER);
        this.setup(new Setting[]{effect, look, colorSetting, shape, radius, timing, hold});
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
        boolean shouldShow = target != null && (timeSinceHit < hold.getValue() * 1000);

        if (shouldShow) {
            fade += (1.0f - fade) * 0.15f;
        } else {
            fade -= fade * 0.15f;
            if (fade < 0.03f) {
                fade = 0f;
                target = null;
            }
        }
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
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        float tickDelta = e.getTickDelta();

        // Позиция над головой цели
        Vec3d targetPos = target.getLerpedPos(tickDelta).add(0, target.getHeight() + 0.5, 0);
        
        double x = targetPos.x - camPos.x;
        double y = targetPos.y - camPos.y;
        double z = targetPos.z - camPos.z;

        matrixStack.push();
        matrixStack.translate(x, y, z);
        
        // Поворачиваем череп к камере (Billboard)
        matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());
        
        float size = radius.getValue();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // Видно сквозь стены
        
        RenderSystem.setShaderTexture(0, SKULL_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        int alpha = (int) (fade * 255);
        int r = 255, g = 255, b = 255; // Белый цвет
        
        buffer.vertex(matrix, -size / 2, -size / 2, 0).texture(0, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, -size / 2, size / 2, 0).texture(0, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, size / 2, size / 2, 0).texture(1, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, size / 2, -size / 2, 0).texture(1, 0).color(r, g, b, alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrixStack.pop();
    }

    public LivingEntity getTarget() {
        return target;
    }
}
