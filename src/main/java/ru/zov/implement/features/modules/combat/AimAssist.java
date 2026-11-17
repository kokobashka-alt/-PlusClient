package ru.zov.implement.features.modules.combat;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.keyboard.MouseRotationEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.modules.combat.aimassist.HumanAimModel;

/**
 * Улучшенный AimAssist с фокусом на хитбокс, мгновенный отклик и постоянное липкое удержание.
 */
public class AimAssist extends Module {
    private static final float DEGREES_PER_UNIT = 0.15F;
    private static final float MAX_FRAME_SECONDS = 0.05F;
    private static final double SWITCH_RATIO = 0.75;
    private static final double CANDIDATE_CONE_SCALE = 1.5;
    private static final double LOSE_CONE_SCALE = 1.8;

    public final ValueSetting distance = new ValueSetting("Дистанция", "Максимальная дистанция цели")
            .setValue(3.8f).range(1.0f, 8.0f);
    public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Работать только если в руке меч/топор")
            .setValue(false);
    public final ValueSetting strength = new ValueSetting("Сила", "Мощность доворота по горизонтали, %")
            .setValue(65.0f).range(0.0f, 100.0f);
    public final ValueSetting vertical = new ValueSetting("Вертикаль", "Мощность доворота по вертикали, %")
            .setValue(40.0f).range(0.0f, 100.0f);
    public final ValueSetting fov = new ValueSetting("FOV", "Угол работы ассиста, °")
            .setValue(15.0f).range(4.0f, 45.0f);

    private final HumanAimModel model = new HumanAimModel(ThreadLocalRandom.current());
    private final HumanAimModel.Settings modelSettings = new HumanAimModel.Settings();
    private final HumanAimModel.Input input = new HumanAimModel.Input();
    private final HumanAimModel.Output output = new HumanAimModel.Output();

    private LivingEntity target;
    private long lastFrameNanos;

    public AimAssist() {
        super("AimAssist", "CombatAim", ModuleCategory.COMBAT);
        this.setup(new Setting[]{distance, onlyWeapon, strength, vertical, fov});
    }

    public static AimAssist getInstance() {
        return (AimAssist) Instance.get(AimAssist.class);
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    @Override
    public void activate() {
        this.setTarget(null);
        this.lastFrameNanos = 0L;
    }

    @Override
    public void deactivate() {
        this.setTarget(null);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.isPaused() || !this.weaponAllowed()) {
            this.setTarget(null);
            return;
        }

        this.updateTarget();
    }

    @EventHandler
    public void onMouseRotation(MouseRotationEvent event) {
        long now = System.nanoTime();
        float dt = this.lastFrameNanos == 0L ? 1.0F / 60.0F : (now - this.lastFrameNanos) / 1.0E9F;
        this.lastFrameNanos = now;

        if (this.target == null || mc.player == null || mc.currentScreen != null || mc.isPaused()) {
            return;
        }

        float deltaX = event.getCursorDeltaX();
        float deltaY = event.getCursorDeltaY();

        this.modelSettings.strength = this.strength.getValue() / 100.0F;
        this.modelSettings.vertical = this.vertical.getValue() / 100.0F;
        this.modelSettings.coneDegrees = this.fov.getValue();

        Vec3d eye = mc.player.getEyePos();
        Vec3d feet = this.target.getEntityPos();
        this.input.dt = Math.min(dt, MAX_FRAME_SECONDS);
        this.input.yaw = mc.player.getYaw();
        this.input.pitch = mc.player.getPitch();
        this.input.eyeX = eye.x;
        this.input.eyeY = eye.y;
        this.input.eyeZ = eye.z;
        this.input.feetX = feet.x;
        this.input.feetY = feet.y;
        this.input.feetZ = feet.z;
        this.input.width = this.target.getWidth();
        this.input.height = this.target.getHeight();
        this.input.inputYaw = deltaX * DEGREES_PER_UNIT;
        this.input.inputPitch = deltaY * DEGREES_PER_UNIT;
        this.input.quantum = mouseCountDegrees();

        this.model.step(this.input, this.modelSettings, this.output);

        if (this.output.yaw != 0.0F) {
            event.setCursorDeltaX(deltaX + this.output.yaw / DEGREES_PER_UNIT);
        }
        if (this.output.pitch != 0.0F) {
            event.setCursorDeltaY(deltaY + this.output.pitch / DEGREES_PER_UNIT);
        }
    }

    private void setTarget(LivingEntity entity) {
        if (entity == this.target) {
            return;
        }

        this.target = entity;
        if (entity == null) {
            this.model.clearTarget();
        } else {
            this.model.acquire();
        }
    }

    private boolean weaponAllowed() {
        if (!this.onlyWeapon.isValue()) {
            return true;
        }

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) {
            return false;
        }
        String name = stack.getItem().getName().getString().toLowerCase();
        return name.contains("sword") || name.contains("axe") || name.contains("меч")
                || name.contains("топор") || name.contains("mace");
    }

    private void updateTarget() {
        double range = this.distance.getValue();
        double rangeSq = range * range;
        double cone = this.fov.getValue();

        if (this.target != null && (mc.player.squaredDistanceTo(this.target) > rangeSq || !this.isValidTarget(this.target)
                || this.angleTo(this.target) > cone * LOSE_CONE_SCALE)) {
            this.setTarget(null);
        }

        LivingEntity best = this.findBest(range, cone * CANDIDATE_CONE_SCALE);
        if (best == null) {
            return;
        }

        if (this.target == null) {
            this.setTarget(best);
        } else if (best != this.target && this.angleTo(best) < SWITCH_RATIO * this.angleTo(this.target)) {
            this.setTarget(best);
        }
    }

    private LivingEntity findBest(double range, double maxAngle) {
        Box box = mc.player.getBoundingBox().expand(range);
        double rangeSq = range * range;
        LivingEntity best = null;
        double bestAngle = maxAngle;
        for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class, box, this::isValidTarget)) {
            if (mc.player.squaredDistanceTo(entity) > rangeSq) {
                continue;
            }

            double angle = this.angleTo(entity);
            if (angle <= bestAngle) {
                bestAngle = angle;
                best = entity;
            }
        }

        return best;
    }

    /** Угол между взглядом и центром хитбокса цели */
    private double angleTo(LivingEntity entity) {
        Vec3d pos = entity.getEntityPos();
        double[] point = {pos.x, pos.y + (entity.getHeight() * 0.5), pos.z};
        Vec3d dir = new Vec3d(point[0], point[1], point[2]).subtract(mc.player.getEyePos()).normalize();
        Vec3d look = mc.player.getRotationVec(1.0F);
        return Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(dir), -1.0, 1.0)));
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity.isDead() || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof ArmorStandEntity) {
            return false;
        }
        if (!mc.player.canSee(entity)) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            return player != mc.player && !player.isSpectator() && !FriendUtils.isFriend(player);
        }
        return true;
    }

    private static float mouseCountDegrees() {
        double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * DEGREES_PER_UNIT);
    }
}
