package ru.zov.implement.features.modules.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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

import java.awt.Color;

public class TargetESP extends Module {

    public final SelectSetting effect = new SelectSetting("Эффект", "Тип отрисовки")
            .value("Ring", "Triangle", "Lightning")
            .selected("Ring");

    public final BooleanSetting look = new BooleanSetting("Цвет", "Настройки цвета").setValue(true);
    public final ValueSetting colorSetting = new ValueSetting("Цвет", "Цвет ESP")
            .setValue(0.5f).range(0f, 1f); // 0 = красный, 1 = синий (упрощенно)

    public final BooleanSetting shape = new BooleanSetting("Форма", "Настройки размера").setValue(true);
    public final ValueSetting radius = new ValueSetting("Радиус", "Радиус круга")
            .setValue(0.7f).range(0.4f, 1.6f);

    public final BooleanSetting timing = new BooleanSetting("Тайминг", "Время показа").setValue(true);
    public final ValueSetting hold = new ValueSetting("Держать", "Секунд после удара")
            .setValue(2.3f).range(0.3f, 10.0f);

    private LivingEntity target;
    private LivingEntity prevTarget;
    private long lastHitTime;
    private float fade;
    private float prevFade;

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

        // Плавное появление/исчезновение
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

    // Этот метод вызывается, когда ты бьешь кого-то (нужно добавить в Aura)
    public void onHit(Entity entity) {
        if (entity instanceof LivingEntity living && living.isAlive()) {
            if (this.target != living) {
                this.prevTarget = this.target;
                this.prevFade = this.fade;
                this.target = living;
                this.fade = 0f;
            }
            this.lastHitTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (fade <= 0.03f || target == null) return;

        // Отрисовка 3D-круга вокруг цели
        Vec3d pos = target.getLerpedPos(e.getTickDelta()).add(0, target.getHeight() / 2.0, 0);
        float r = radius.getValue();
        
        // Цвет (переводим 0-1 в RGB)
        int red = (int) (255 * (1f - colorSetting.getValue()));
        int blue = (int) (255 * colorSetting.getValue());
        Color color = new Color(red, 50, blue, (int)(255 * fade));

        // Рисуем кольцо из линий
        int points = 40;
        for (int i = 0; i < points; i++) {
            double angle1 = Math.toRadians((360.0 / points) * i);
            double angle2 = Math.toRadians((360.0 / points) * (i + 1));

            Vec3d p1 = pos.add(Math.cos(angle1) * r, 0, Math.sin(angle1) * r);
            Vec3d p2 = pos.add(Math.cos(angle2) * r, 0, Math.sin(angle2) * r);

            // Отрисовка линии (зависит от твоей утилиты рендера линий)
            // Если у тебя есть Render3DUtil, это будет выглядеть так:
            ru.zov.common.util.render.Render3DUtil.drawLine(p1, p2, color.getRGB(), 2.0f);
        }
    }

    // Геттер для Aura
    public LivingEntity getTarget() {
        return target;
    }
}
