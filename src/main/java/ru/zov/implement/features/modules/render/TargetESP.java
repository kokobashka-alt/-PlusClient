
package ru.zov.implement.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
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

    private static final Identifier SKULL_TEXTURE =
            Identifier.of("zov", "textures/target_skull.png");

    public final SelectSetting effect = new SelectSetting("Эффект", "Тип отрисовки")
            .value("Skull", "Ring", "Triangle")
            .selected("Skull");

    public final BooleanSetting look =
            new BooleanSetting("Цвет", "Настройки цвета").setValue(true);

    public final ValueSetting colorSetting =
            new ValueSetting("Цвет", "Цвет ESP")
                    .setValue(0.5f).range(0f, 1f);

    public final BooleanSetting shape =
            new BooleanSetting("Форма", "Настройки размера").setValue(true);

    public final ValueSetting radius =
            new ValueSetting("Радиус", "Размер черепа")
                    .setValue(1.2f).range(0.4f, 3.0f);

    public final BooleanSetting timing =
            new BooleanSetting("Тайминг", "Время показа").setValue(true);

    public final ValueSetting hold =
            new ValueSetting("Держать", "Секунд после удара")
                    .setValue(2.3f).range(0.3f, 10.0f);

    private LivingEntity target;
    private long lastHitTime;
    private float fade;

    public TargetESP() {
        super("TargetESP", "3D ESP вокруг цели", ModuleCategory.RENDER);
        setup(new Setting[]{
                effect, look, colorSetting, shape, radius, timing, hold
        });
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

        long elapsed = System.currentTimeMillis() - lastHitTime;
        boolean show = target != null &&
                elapsed < hold.getValue() * 1000;

        if (show) {
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
            target = living;
            lastHitTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (fade <= 0.03f || target == null) return;
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = e.stack();
        float tickDelta = e.partialTicks();

        Camera camera = mc.gameRenderer.getCamera();

        Vec3d pos = target.getLerpedPos(tickDelta)
                .add(0, target.getHeight() + 0.5, 0);

        Vec3d camPos = camera.getCameraPos();

        double x = pos.x - camPos.x;
        double y = pos.y - camPos.y;
        double z = pos.z - camPos.z;

        matrices.push();
        matrices.translate(x, y, z);

        matrices.multiply(
                RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw())
        );
        matrices.multiply(
                RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch())
        );

        float size = radius.getValue();
        float half = size / 2.0f;

        int alpha = (int) (fade * 255);

        int red = 255;
        int green = 255;
        int blue = 255;

        if (look.getValue()) {
            float hue = colorSetting.getValue();
            red = (int) (255 * hue);
            green = (int) (255 * (1.0f - hue));
            blue = 255;
        }

        VertexConsumerProvider.Immediate consumers =
                mc.getBufferBuilders().getEntityVertexConsumers();

        VertexConsumer vertex = consumers.getBuffer(
                RenderLayer.getEntityTranslucent(SKULL_TEXTURE)
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        vertex.vertex(matrix, -half, -half, 0)
                .color(red, green, blue, alpha)
                .texture(0, 1)
                .overlay(0)
                .light(15728880)
                .normal(matrices.peek(), 0, 0, 1);

        vertex.vertex(matrix, half, -half, 0)
                .color(red, green, blue, alpha)
                .texture(1, 1)
                .overlay(0)
                .light(15728880)
                .normal(matrices.peek(), 0, 0, 1);

        vertex.vertex(matrix, half, half, 0)
                .color(red, green, blue, alpha)
                .texture(1, 0)
                .overlay(0)
                .light(15728880)
                .normal(matrices.peek(), 0, 0, 1);

        vertex.vertex(matrix, -half, half, 0)
                .color(red, green, blue, alpha)
                .texture(0, 0)
                .overlay(0)
                .light(15728880)
                .normal(matrices.peek(), 0, 0, 1);

        consumers.draw(RenderLayer.getEntityTranslucent(SKULL_TEXTURE));

        matrices.pop();
    }

    public LivingEntity getTarget() {
        return target;
    }
}
