package ru.zov.implement.features.modules.combat;

import java.util.Comparator;

import net.minecraft.client.MinecraftClient;
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
import ru.zov.implement.features.modules.combat.command.BezierChatCommand;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.type.AimAssistRotationMode;

/**
 * AimAssist as a normal client module.
 *
 * <p>The actual motion profile lives in {@link AimAssistRotationMode}: it wraps the Bezier rotation, adds a reaction
 * delay, acceleration/deceleration, a stopping clamp, tremor and an FOV gate. This module only:
 *
 * <ol>
 *   <li>selects a target with the same rules as the previous AimAssist module;</li>
 *   <li>asks the rotation mode for the next step once per client tick;</li>
 *   <li>adds a scaled part of that step to the player's own mouse delta, and only when the mouse is already moving
 *       toward the target.</li>
 * </ol>
 *
 * <p>It never rotates the view while the mouse is still, and it never fights a mouse movement away from the target.
 * That is what keeps it an assist rather than a silent aim. Pitch is not applied here because the module's
 * {@code MouseRotationEvent} in the client exposes only the horizontal cursor delta; the rotation mode still tracks
 * pitch internally and can be used from a normal rotation pipeline.
 */
public class AimAssist extends Module {

    private final AimAssistRotationMode rotation = new AimAssistRotationMode();

    private LivingEntity target;
    private float pendingYawStep;
    private boolean appliedThisTick;

    public final ValueSetting distance = new ValueSetting("Дистанция", "Максимальная дистанция цели")
            .setValue(3.5f).range(1.0f, 8.0f);
    public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", "Работать только если в руке меч/топор")
            .setValue(false);
    public final ValueSetting strength = new ValueSetting("Сила", "Сколько шага ротации добавлять к мыши, %")
            .setValue(60.0f).range(0.0f, 100.0f);
    public final ValueSetting maxYawSpeed = new ValueSetting("Скорость", "Максимальный шаг по yaw, °/тик")
            .setValue(14.0f).range(1.0f, 30.0f);
    public final ValueSetting fov = new ValueSetting("FOV", "Ассист работает только внутри этого угла, °")
            .setValue(45.0f).range(5.0f, 180.0f);

    private boolean chatCommandRegistered;

    public AimAssist() {
        super("AimAssist", "CombatAim", ModuleCategory.COMBAT);
        this.setup(new Setting[]{distance, onlyWeapon, strength, maxYawSpeed, fov});
        this.registerChatCommand();
    }

    public static AimAssist getInstance() {
        return (AimAssist) Instance.get(AimAssist.class);
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    /** Same recording hotkey as the Bezier rotation; wire it to your keybind if you use the built-in trainer. */
    public void toggleRecording() {
        this.rotation.toggleRecording();
    }

    /** Same training hotkey as the Bezier rotation. */
    public void toggleTraining() {
        this.rotation.toggleTraining();
    }

    public boolean isTraining() {
        return this.rotation.isTraining();
    }

    private void registerChatCommand() {
        if (this.chatCommandRegistered) {
            return;
        }
        MinecraftClient.getInstance().getCommandManager().register(new BezierChatCommand(this));
        this.chatCommandRegistered = true;
    }

    @Override
    public void activate() {
        this.reset();
    }

    @Override
    public void deactivate() {
        this.reset();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        this.appliedThisTick = false;
        this.pendingYawStep = 0.0F;

        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.isPaused() || !this.weaponAllowed()) {
            this.clearTarget();
            return;
        }

        this.updateTarget();
        if (this.target == null) {
            this.rotation.resetMotion();
            return;
        }

        AimAssistRotationMode.Config config = this.rotation.config();
        config.maxYawSpeed = this.maxYawSpeed.getValue();
        config.maxFovDegrees = this.fov.getValue();

        Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        Rotation aim = this.rotationTo(this.target);
        Rotation next = this.rotation.process(current, aim, Vec3d.ZERO, this.target);

        float step = MathHelper.wrapDegrees(next.getYaw() - current.getYaw());
        this.pendingYawStep = step * (this.strength.getValue() / 100.0F);
    }

    @EventHandler
    public void onMouseRotation(MouseRotationEvent event) {
        if (this.appliedThisTick || this.target == null || mc.player == null
                || mc.currentScreen != null || mc.isPaused()) {
            return;
        }

        float deltaX = event.getCursorDeltaX();
        if (deltaX == 0.0F) {
            return;
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d center = this.target.getBoundingBox().getCenter();
        double dx = center.x - eye.x;
        double dz = center.z - eye.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawError = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());

        // Assist only an aim that is already moving toward the target. Moving away stays the player's decision.
        if (yawError == 0.0F || deltaX * yawError <= 0.0F) {
            return;
        }

        float absDelta = Math.abs(deltaX);
        float remaining = Math.abs(yawError) - absDelta;
        if (remaining <= 0.0F) {
            // The player's own movement already reaches or crosses the target; do not add anything.
            this.appliedThisTick = true;
            return;
        }

        // Slow mouse movement gets more help; a fast flick gets little or none.
        float scale = MathHelper.clamp(1.0F - absDelta / 50.0F, 0.0F, 1.0F);
        float assist = this.pendingYawStep * scale;

        // Do not let a step accumulated while the mouse was still turn a tiny movement into a snap. The assist is
        // capped both by the remaining error and by a small multiple of the player's own movement.
        float maxAssist = Math.min(remaining, Math.max(absDelta * 2.0F, 0.05F));
        if (Math.abs(assist) > maxAssist) {
            assist = Math.copySign(maxAssist, assist);
        }
        if (assist == 0.0F) {
            this.appliedThisTick = true;
            return;
        }

        float total = deltaX + assist;
        event.setCursorDeltaX(applyGCD(total, deltaX));
        this.appliedThisTick = true;
    }

    private void reset() {
        this.target = null;
        this.pendingYawStep = 0.0F;
        this.appliedThisTick = true;
        this.rotation.resetMotion();
    }

    private void clearTarget() {
        this.target = null;
        this.pendingYawStep = 0.0F;
        this.rotation.resetMotion();
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

        if (this.target == null || mc.player.squaredDistanceTo(this.target) > rangeSq
                || !this.isValidTarget(this.target)) {
            this.target = this.findTarget(range);
        }
    }

    private LivingEntity findTarget(double range) {
        Box box = mc.player.getBoundingBox().expand(range);
        double rangeSq = range * range;
        return mc.world.getEntitiesByClass(LivingEntity.class, box, this::isValidTarget)
                .stream()
                .filter(entity -> mc.player.squaredDistanceTo(entity) <= rangeSq)
                .min(Comparator.comparingDouble(this::fovTo))
                .orElse(null);
    }

    private double fovTo(LivingEntity entity) {
        Vec3d look = mc.player.getRotationVec(1.0F);
        Vec3d dir = entity.getEntityPos().subtract(mc.player.getEyePos()).normalize();
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

    private Rotation rotationTo(LivingEntity entity) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = entity.getBoundingBox().getCenter();
        double dx = center.x - eye.x;
        double dy = center.y - eye.y;
        double dz = center.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new Rotation(yaw, pitch);
    }

    private static float gcd() {
        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }

    private static float applyGCD(float angle, float previous) {
        float gcd = gcd();
        return previous + Math.round((angle - previous) / gcd) * gcd;
    }
}
