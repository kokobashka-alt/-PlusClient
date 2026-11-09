package ru.zov.implement.features.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
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

        // Получаем DrawContext из ивента (если твой WorldRenderEvent его отдает)
        // Если нет, используем MatrixStack напрямую
        MatrixStack matrixStack = e.getMatrixStack();
        float tickDelta = e.getTickDelta();
        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
        
        Vec3d targetPos = target.getLerpedPos(tickDelta).add(0, target.getHeight() + 0.5, 0);
        Vec3d camPos = camera.getPos();
        
        double x = targetPos.x - camPos.x;
        double y = targetPos.y - camPos.y;
        double z = targetPos.z - camPos.z;

        matrixStack.push();
        matrixStack.translate(x, y, z);
        
        // Поворот к камере (Billboard)
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        
        float size = radius.getValue();
        int alpha = (int) (fade * 255);

        // В 1.21.11 самый надежный способ нарисовать 2D-текстуру в 3D — через DrawContext.
        // Если у тебя WorldRenderEvent не отдает DrawContext, то используем RenderSystem напрямую.
        // Здесь мы используем стандартный рендер текстуры.
        
        net.minecraft.client.gui.DrawContext context = new net.minecraft.client.gui.DrawContext(
                mc, matrixStack, mc.getBufferBuilders().getEntityVertexConsumers(), 0, 0
        );
        
        // Рисуем текстуру
        context.drawTexture(
                SKULL_TEXTURE, 
                (int)(-size / 2), (int)(-size / 2), 
                0, 0, 
                (int)size, (int)size, 
                (int)size, (int)size
        );
        
        // Очищаем буфер, чтобы текстура отрисовалась
        mc.getBufferBuilders().getEntityVertexConsumers().draw();

        matrixStack.pop();
    }

    public LivingEntity getTarget() {
        return target;
    }
}
