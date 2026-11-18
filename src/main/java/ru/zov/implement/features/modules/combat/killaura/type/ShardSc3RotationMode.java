package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.Random;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.BooleanSetting;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.ModeSetting;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.NumberSetting;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;

/**
 * Shard (SC3) rotation, ported from the uploaded dev.leg1tka ShardRotationStrategy.
 *
 * Replaced in the port:
 * - RotationStorage / RotationStrategy: the mode returns the next rotation from process(); Aura applies it.
 * - BackTrackOld history: the box is the target's current bounding box.
 * - EntityRaycast / MathOperations / RotationContext: local equivalents, same formulas.
 * - ModuleManager.O (gliding target check): not ported; gliding targets use the regular point.
 * - RotationSensitivity gate (d()): not ported; treated as inactive.
 * - Return speeds (smoothReturn, returnSpeedYaw/Pitch): in the original they apply to the return phase after the
 *   aim is released (RotationStorage.RESET), not to aiming. Kept as settings, not applied here.
 */
public final class ShardSc3RotationMode extends RotationMode {
    public enum SpeedMode implements DisplayNamed {
        ACCELERATION(Translation.clearText("Ускорение")),
        CONSTANT(Translation.clearText("Постоянная")),
        ADAPTIVE(Translation.clearText("Адаптивная"));

        private final Translation displayName;

        SpeedMode(Translation displayName) {
            this.displayName = displayName;
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum PointMode implements DisplayNamed {
        NEAREST_VISIBLE(Translation.clearText("Ближайшая видимая")),
        CHEST(Translation.clearText("Грудь")),
        HEAD(Translation.clearText("Голова"));

        private final Translation displayName;

        PointMode(Translation displayName) {
            this.displayName = displayName;
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum MultipointMode implements DisplayNamed {
        SOFT(Translation.clearText("Мягкий")),
        AGGRESSIVE(Translation.clearText("Агрессивный")),
        SKELETAL(Translation.clearText("Скелетный")),
        GAUSS(Translation.clearText("Гауссов")),
        ORGANIC(Translation.clearText("Органический")),
        SPIRAL(Translation.clearText("Спиральный")),
        PERIMETER(Translation.clearText("Периметр")),
        KINETIC(Translation.clearText("Кинетический"));

        private final Translation displayName;

        MultipointMode(Translation displayName) {
            this.displayName = displayName;
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public final ModeSetting<SpeedMode> speedMode = new ModeSetting<SpeedMode>(Translation.clearText("SC3: Режим скорости")).values(SpeedMode.class);
    public final BooleanSetting finalAdjust = new BooleanSetting(Translation.clearText("SC3: Доводка")).setValue(false);
    public final NumberSetting yawSpeedLimit = new NumberSetting(Translation.clearText("SC3: Скорость Yaw")).currentValue(45.0F).range(5.0F, 180.0F).step(5.0F);
    public final NumberSetting pitchSpeedLimit = new NumberSetting(Translation.clearText("SC3: Скорость Pitch")).currentValue(30.0F).range(5.0F, 180.0F).step(5.0F);
    public final BooleanSetting freezeOnHitbox = new BooleanSetting(Translation.clearText("SC3: Застывать на хитбоксе")).setValue(false);
    public final NumberSetting accelGain = new NumberSetting(Translation.clearText("SC3: Прирост ускорения")).currentValue(0.004F).range(0.001F, 0.02F).step(0.001F);
    public final NumberSetting accelDecay = new NumberSetting(Translation.clearText("SC3: Спад ускорения")).currentValue(0.01F).range(0.001F, 0.1F).step(0.001F);
    public final NumberSetting largeAngleDecay = new NumberSetting(Translation.clearText("SC3: Спад при большом угле")).currentValue(0.1F).range(0.01F, 0.3F).step(0.01F);
    public final NumberSetting accelMax = new NumberSetting(Translation.clearText("SC3: Макс. ускорение")).currentValue(0.18F).range(0.1F, 0.5F).step(0.01F);
    public final NumberSetting accelMin = new NumberSetting(Translation.clearText("SC3: Мин. ускорение")).currentValue(-0.02F).range(-0.1F, 0.0F).step(0.01F);
    public final NumberSetting largeAngleThreshold = new NumberSetting(Translation.clearText("SC3: Порог большого угла")).currentValue(80.0F).range(30.0F, 180.0F).step(5.0F);
    public final NumberSetting rayTraceDistance = new NumberSetting(Translation.clearText("SC3: Дистанция RayTrace")).currentValue(999.0F).range(100.0F, 2000.0F).step(10.0F);
    public final NumberSetting hitboxExpand = new NumberSetting(Translation.clearText("SC3: Расширение хитбокса")).currentValue(-0.2F).range(-1.0F, 1.0F).step(0.1F);
    public final ModeSetting<PointMode> pointMode = new ModeSetting<PointMode>(Translation.clearText("SC3: Режим точки")).values(PointMode.class);
    public final BooleanSetting multipoint = new BooleanSetting(Translation.clearText("SC3: Мультипоинт")).setValue(true);
    public final ModeSetting<MultipointMode> multipointMode = new ModeSetting<MultipointMode>(Translation.clearText("SC3: Режим мультипоинта")).values(MultipointMode.class);
    public final NumberSetting multipointSpeed = new NumberSetting(Translation.clearText("SC3: Скорость смены мультипоинтов")).currentValue(1.0F).range(0.1F, 3.0F).step(0.1F);
    public final NumberSetting attackDistance = new NumberSetting(Translation.clearText("Дистанция атаки")).currentValue(3.0F).range(0.0F, 6.0F).step(0.05F);
    public final NumberSetting shakePitchAmp = new NumberSetting(Translation.clearText("SC3: Амплитуда Shake Pitch")).currentValue(0.8F).range(0.0F, 6.0F).step(0.1F);
    public final NumberSetting shakePitchDamp = new NumberSetting(Translation.clearText("SC3: Затухание Shake Pitch")).currentValue(0.35F).range(0.0F, 1.0F).step(0.01F);
    public final NumberSetting shakePitchBounce = new NumberSetting(Translation.clearText("SC3: Отскок Shake Pitch")).currentValue(0.0F).range(0.0F, 1.0F).step(0.05F);
    public final NumberSetting shakePitchRandom = new NumberSetting(Translation.clearText("SC3: Рандом Shake Pitch")).currentValue(0.5F).range(0.0F, 1.0F).step(0.05F);
    public final BooleanSetting jitter = new BooleanSetting(Translation.clearText("SC3: Jitter")).setValue(false);
    public final NumberSetting jitterChance = new NumberSetting(Translation.clearText("SC3: Шанс Jitter")).currentValue(18.0F).range(0.0F, 100.0F).step(1.0F);
    public final NumberSetting jitterStrength = new NumberSetting(Translation.clearText("SC3: Сила Jitter")).currentValue(0.35F).range(0.0F, 2.0F).step(0.05F);
    public final BooleanSetting jitterOnlyFinal = new BooleanSetting(Translation.clearText("SC3: Jitter только на доводке")).setValue(true);
    public final BooleanSetting jitterOnlyYaw = new BooleanSetting(Translation.clearText("SC3: Jitter только Yaw")).setValue(true);
    public final BooleanSetting smoothReturn = new BooleanSetting(Translation.clearText("SC3: Плавная отводка")).setValue(true);
    public final NumberSetting returnSpeedYaw = new NumberSetting(Translation.clearText("SC3: Скорость отводки Yaw")).currentValue(25.0F).range(5.0F, 180.0F).step(1.0F);
    public final NumberSetting returnSpeedPitch = new NumberSetting(Translation.clearText("SC3: Скорость отводки Pitch")).currentValue(20.0F).range(5.0F, 180.0F).step(1.0F);
    public final BooleanSetting fixPoint = new BooleanSetting(Translation.clearText("SC3: Фиксация точки")).setValue(true);
    public final NumberSetting fixTime = new NumberSetting(Translation.clearText("SC3: Время фиксации")).currentValue(3.0F).range(1.0F, 10.0F).step(1.0F);
    public final NumberSetting yawSmoothMul = new NumberSetting(Translation.clearText("SC3: Множитель плавности Yaw")).currentValue(1.0F).range(0.1F, 2.5F).step(0.1F);
    public final NumberSetting pitchSmoothMul = new NumberSetting(Translation.clearText("SC3: Множитель плавности Pitch")).currentValue(0.5F).range(0.1F, 2.5F).step(0.1F);
    public final NumberSetting limitDown = new NumberSetting(Translation.clearText("SC3: Максимально вниз")).currentValue(100.0F).range(0.0F, 100.0F).step(1.0F);
    public final NumberSetting limitUp = new NumberSetting(Translation.clearText("SC3: Максимально вверх")).currentValue(100.0F).range(0.0F, 100.0F).step(1.0F);
    public final NumberSetting hitboxSlow = new NumberSetting(Translation.clearText("SC3: Замедление на хитбоксе")).currentValue(0.26F).range(0.05F, 1.0F).step(0.01F);
    public final NumberSetting speedReturn = new NumberSetting(Translation.clearText("SC3: Возврат скорости")).currentValue(0.03F).range(0.01F, 0.5F).step(0.01F);
    public final BooleanSetting fixSnap = new BooleanSetting(Translation.clearText("SC3: Fix Snap")).setValue(false);
    public final BooleanSetting gcdFix = new BooleanSetting(Translation.clearText("SC3: Исправление GCD")).setValue(false);
    public final BooleanSetting microOvershoot = new BooleanSetting(Translation.clearText("SC3: Микро-овершут")).setValue(false);
    public final NumberSetting overshootStrength = new NumberSetting(Translation.clearText("SC3: Сила овершута")).currentValue(0.6F).range(0.1F, 2.0F).step(0.05F);
    public final BooleanSetting reactionDelay = new BooleanSetting(Translation.clearText("SC3: Задержка реакции")).setValue(false);
    public final NumberSetting reactionMs = new NumberSetting(Translation.clearText("SC3: Время реакции (мс)")).currentValue(80.0F).range(20.0F, 200.0F).step(5.0F);
    public final BooleanSetting bezier = new BooleanSetting(Translation.clearText("SC3: Траектория Безье")).setValue(false);
    public final BooleanSetting shakeYaw = new BooleanSetting(Translation.clearText("SC3: Shake Yaw")).setValue(false);
    public final NumberSetting shakeYawSpeed = new NumberSetting(Translation.clearText("SC3: Скорость Shake Yaw")).currentValue(0.45F).range(0.0F, 3.0F).step(0.05F);
    public final NumberSetting shakeYawAmp = new NumberSetting(Translation.clearText("SC3: Амплитуда Shake Yaw")).currentValue(1.8F).range(0.0F, 10.0F).step(0.1F);
    public final NumberSetting shakeYawDamp = new NumberSetting(Translation.clearText("SC3: Затухание Shake Yaw")).currentValue(0.3F).range(0.0F, 1.0F).step(0.01F);
    public final NumberSetting shakeYawBounce = new NumberSetting(Translation.clearText("SC3: Отскок Shake Yaw")).currentValue(0.1F).range(0.0F, 1.0F).step(0.05F);
    public final NumberSetting shakeYawRandom = new NumberSetting(Translation.clearText("SC3: Рандом Shake Yaw")).currentValue(0.8F).range(0.0F, 1.0F).step(0.05F);
    public final BooleanSetting shakePitch = new BooleanSetting(Translation.clearText("SC3: Shake Pitch")).setValue(false);
    public final NumberSetting shakePitchSpeed = new NumberSetting(Translation.clearText("SC3: Скорость Shake Pitch")).currentValue(0.25F).range(0.0F, 3.0F).step(0.05F);
    public final BooleanSetting antiSnap = new BooleanSetting(Translation.clearText("SC3: Анти резкий разворот")).setValue(true);
    public final NumberSetting antiSnapThreshold = new NumberSetting(Translation.clearText("SC3: Порог анти разворота")).currentValue(110.0F).range(45.0F, 180.0F).step(5.0F);
    public final NumberSetting antiSnapStrength = new NumberSetting(Translation.clearText("SC3: Сила анти разворота")).currentValue(0.45F).range(0.05F, 0.95F).step(0.05F);

    private final Random rng = new Random();

    private Entity tracked;
    private float yaw;
    private float pitch;
    private float accel;
    private boolean reacting;
    private float absorb = 1.0F;
    private float shakeYawValue;
    private float shakePitchValue;
    private float shakeYawPhase;
    private float shakePitchPhase;
    private float jitterYaw;
    private float jitterPitch;
    private Vec3d fixedPoint;
    private Entity fixedEntity;
    private int fixedUses;
    private float overshootYaw;
    private float overshootPitch;
    private int overshootTicks;
    private float residualYaw;
    private float residualPitch;
    private long reactionUntil;
    private Vec3d bezierControl;
    private float bezierT;
    private Vec3d organicOffset;
    private Vec3d organicNoise;
    private float bounceYaw;
    private float bouncePitch;

    public ShardSc3RotationMode() {
        super("Shard SC3");
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    public void reset() {
        this.tracked = null;
        this.accel = 0.0F;
        this.reacting = false;
        this.absorb = 1.0F;
        this.shakeYawValue = 0.0F;
        this.shakePitchValue = 0.0F;
        this.jitterYaw = 0.0F;
        this.jitterPitch = 0.0F;
        this.fixedPoint = null;
        this.fixedEntity = null;
        this.fixedUses = 0;
        this.overshootYaw = 0.0F;
        this.overshootPitch = 0.0F;
        this.overshootTicks = 0;
        this.residualYaw = 0.0F;
        this.residualPitch = 0.0F;
        this.reactionUntil = 0L;
        this.bezierControl = null;
        this.bezierT = 0.0F;
        this.organicOffset = null;
        this.organicNoise = null;
        this.bounceYaw = 0.0F;
        this.bouncePitch = 0.0F;
    }

    public Setting[] settings() {
        return new Setting[]{
            this.speedMode, this.finalAdjust, this.yawSpeedLimit, this.pitchSpeedLimit, this.freezeOnHitbox,
            this.accelGain, this.accelDecay, this.largeAngleDecay, this.accelMax, this.accelMin,
            this.largeAngleThreshold, this.rayTraceDistance, this.hitboxExpand, this.pointMode, this.multipoint,
            this.multipointMode, this.multipointSpeed, this.attackDistance, this.shakePitchAmp, this.shakePitchDamp,
            this.shakePitchBounce, this.shakePitchRandom, this.jitter, this.jitterChance, this.jitterStrength,
            this.jitterOnlyFinal, this.jitterOnlyYaw, this.smoothReturn, this.returnSpeedYaw, this.returnSpeedPitch,
            this.fixPoint, this.fixTime, this.yawSmoothMul, this.pitchSmoothMul, this.limitDown, this.limitUp,
            this.hitboxSlow, this.speedReturn, this.fixSnap, this.gcdFix, this.microOvershoot, this.overshootStrength,
            this.reactionDelay, this.reactionMs, this.bezier, this.shakeYaw, this.shakeYawSpeed, this.shakeYawAmp,
            this.shakeYawDamp, this.shakeYawBounce, this.shakeYawRandom, this.shakePitch, this.shakePitchSpeed,
            this.antiSnap, this.antiSnapThreshold, this.antiSnapStrength
        };
    }

    /**
     * @param current the rotation the player has now
     * @return the next rotation; the same as current when there is nothing to aim at yet
     */
    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (current == null) {
            return target;
        }
        if (player == null || Mc.INSTANCE.getWorld() == null || !(entity instanceof LivingEntity living)) {
            return current;
        }

        if (living != this.tracked) {
            this.tracked = living;
            this.yaw = MathHelper.wrapDegrees(player.getYaw());
            this.pitch = player.getPitch();
            this.accel = 0.0F;
            this.reacting = false;
            this.absorb = 1.0F;
            this.shakeYawValue = 0.0F;
            this.shakePitchValue = 0.0F;
            this.jitterYaw = 0.0F;
            this.jitterPitch = 0.0F;
            this.fixedPoint = null;
            this.fixedEntity = null;
            this.fixedUses = 0;
            this.residualYaw = 0.0F;
            this.residualPitch = 0.0F;
            this.overshootYaw = 0.0F;
            this.overshootPitch = 0.0F;
            this.overshootTicks = 0;
            this.bezierControl = null;
            this.bezierT = 0.0F;
            this.bounceYaw = 0.0F;
            this.bouncePitch = 0.0F;
            this.reactionUntil = this.reactionDelay.isValue()
                ? System.currentTimeMillis() + (long) this.reactionMs.currentValue()
                : 0L;
        }

        if (System.currentTimeMillis() < this.reactionUntil) {
            return current;
        }

        // Aim from the view the player actually has now: current already contains our previous output plus the
        // player's own mouse movement, so the mouse keeps working instead of being overwritten each tick.
        this.yaw = MathHelper.wrapDegrees(current.getYaw());
        this.pitch = current.getPitch();
        float out[] = this.aim(living, player);
        return new Rotation(out[0], out[1]);
    }

    private float[] aim(LivingEntity living, ClientPlayerEntity player) {
        Vec3d aimPoint = this.pointFor(living, player);
        Vec3d eye = player.getEyePos();

        if (this.finalAdjust.isValue()) {
            Vec3d velocity = player.getVelocity();
            double dx = living.getX() - living.lastX;
            double dz = living.getZ() - living.lastZ;
            double dy = living.getY() - living.lastY;
            double dist = Math.max(1.0, eye.distanceTo(aimPoint));
            double k = Math.min(dist * 0.12, 0.35);
            aimPoint = aimPoint.add((dx - velocity.x) * k, (dy - velocity.y) * k * 0.5, (dz - velocity.z) * k);
        }

        Vec3d toAim = aimPoint.subtract(eye);
        float yawToAim = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toAim.z, toAim.x)) - 90.0);
        float pitchToAim = (float) (-Math.toDegrees(Math.atan2(toAim.y, toAim.horizontalLength())));
        pitchToAim = this.clampPitch(pitchToAim);

        Box box = living.getBoundingBox();
        boolean hit = rayHits(player.getRotationVector(this.pitch, this.yaw), this.rayTraceDistance.currentValue(),
            box.expand(this.hitboxExpand.currentValue()));

        float dy = MathHelper.wrapDegrees(yawToAim - this.yaw);
        float dp = pitchToAim - this.pitch;
        float outYaw = this.yaw;
        float outPitch = this.pitch;

        if (this.freezeOnHitbox.isValue() && hit && Math.abs(dy) < 5.0F && Math.abs(dp) < 4.0F) {
            outYaw = this.yaw;
            outPitch = this.pitch;
        } else if (this.speedMode.currentValue() == SpeedMode.CONSTANT) {
            float stepYaw = Math.min(Math.abs(dy), this.yawSpeedLimit.currentValue()) * Math.signum(dy);
            float stepPitch = Math.min(Math.abs(dp), this.pitchSpeedLimit.currentValue()) * Math.signum(dp);
            outYaw = this.yaw + stepYaw * this.yawSmoothMul.currentValue();
            outPitch = this.pitch + stepPitch * this.pitchSmoothMul.currentValue();
        } else if (this.speedMode.currentValue() == SpeedMode.ADAPTIVE) {
            float factor = MathHelper.clamp((float) Math.sqrt(dy * dy + dp * dp) / 45.0F, 0.15F, 1.0F);
            outYaw = this.yaw + dy * factor * this.yawSmoothMul.currentValue();
            outPitch = this.pitch + dp * factor * this.pitchSmoothMul.currentValue();
        } else {
            boolean fixSnapActive = this.fixSnap.isValue() && player.getAttackCooldownProgress(1.5F) < 0.85F;
            if (fixSnapActive) {
                this.reacting = true;
            }

            if (this.reacting) {
                float floor = this.accelMin.currentValue();
                float decay = Math.abs(MathHelper.wrapDegrees(yawToAim - this.yaw)) > this.largeAngleThreshold.currentValue()
                    ? this.largeAngleDecay.currentValue()
                    : this.accelDecay.currentValue();
                if (this.accel >= floor) {
                    this.accel -= decay;
                }
                if (this.accel <= floor) {
                    this.reacting = false;
                }
            } else {
                this.accel += this.accelGain.currentValue();
                if (this.accel >= this.accelMax.currentValue() || hit) {
                    this.reacting = true;
                }
            }

            float factor = this.antiSnapFactor(dy);
            dy *= factor;
            dp *= 1.0F - (1.0F - factor) * 0.5F;
            float strength = Math.max(this.accel, 0.0F) * this.absorbFactor(box, eye);
            outYaw = this.yaw + dy * Math.min(Math.max(strength * this.yawSmoothMul.currentValue(), 0.0F), 1.0F);
            outPitch = this.pitch + dp * Math.min(Math.max(strength * this.pitchSmoothMul.currentValue(), 0.0F), 1.0F);
            if (fixSnapActive) {
                outYaw = this.yaw;
                outPitch = this.pitch;
            }
        }

        if (this.microOvershoot.isValue() && this.overshootTicks == 0
            && Math.abs(dy) < 8.0F && Math.abs(dp) < 6.0F && this.accel > 0.04F) {
            float strength = this.overshootStrength.currentValue();
            this.overshootYaw = Math.signum(dy) * (0.6F + this.rng.nextFloat() * 0.5F) * strength;
            this.overshootPitch = Math.signum(dp) * (0.3F + this.rng.nextFloat() * 0.3F) * strength;
            this.overshootTicks = 3;
        }

        if (this.overshootTicks > 0) {
            outYaw += this.overshootYaw;
            outPitch += this.overshootPitch;
            this.overshootYaw *= 0.5F;
            this.overshootPitch *= 0.5F;
            this.overshootTicks--;
            if (this.overshootTicks == 0) {
                this.overshootYaw = 0.0F;
                this.overshootPitch = 0.0F;
            }
        }

        if (this.bezier.isValue() && !hit) {
            if (this.bezierControl == null || this.bezierT >= 1.0F) {
                float offsetX = (this.rng.nextFloat() - 0.5F) * 6.0F;
                this.bezierControl = new Vec3d(
                    this.yaw + dy * 0.5F + offsetX,
                    this.pitch + dp * 0.5F + (this.rng.nextFloat() - 0.5F) * 3.0F,
                    0.0);
                this.bezierT = 0.0F;
            }
            this.bezierT = Math.min(1.0F, this.bezierT + 0.25F);
            float inv = 1.0F - this.bezierT;
            outYaw = inv * inv * this.yaw + 2.0F * inv * this.bezierT * (float) this.bezierControl.x + this.bezierT * this.bezierT * outYaw;
            outPitch = inv * inv * this.pitch + 2.0F * inv * this.bezierT * (float) this.bezierControl.y + this.bezierT * this.bezierT * outPitch;
        } else {
            this.bezierControl = null;
            this.bezierT = 0.0F;
        }

        if (!this.freezeOnHitbox.isValue() || !hit) {
            if (this.shakeYaw.isValue() || this.shakePitch.isValue()) {
                this.shakeStep(dy, dp);
                outYaw += this.shakeYawValue;
                outPitch += this.shakePitchValue;
            }

            if (this.jitter.isValue()) {
                this.jitterStep(dy, dp);
                outYaw += this.jitterYaw;
                outPitch += this.jitterPitch;
            }
        }

        if (this.bounceYaw != 0.0F || this.bouncePitch != 0.0F) {
            outYaw += this.bounceYaw;
            outPitch += this.bouncePitch;
            this.bounceYaw *= 0.4F;
            this.bouncePitch *= 0.4F;
            if (Math.abs(this.bounceYaw) < 0.005F) {
                this.bounceYaw = 0.0F;
            }
            if (Math.abs(this.bouncePitch) < 0.005F) {
                this.bouncePitch = 0.0F;
            }
        }

        if (this.gcdFix.isValue()) {
            double gcd = MathUtil.computeGcd();
            float totalYaw = MathHelper.wrapDegrees(outYaw - this.yaw) + this.residualYaw;
            float totalPitch = outPitch - this.pitch + this.residualPitch;
            int indexYaw = gridIndex(totalYaw, gcd);
            int indexPitch = gridIndex(totalPitch, gcd);
            float quantYaw = (float) (indexYaw * gcd);
            float quantPitch = (float) (indexPitch * gcd);
            float half = (float) (gcd * 0.5);
            this.residualYaw = MathHelper.clamp(totalYaw - quantYaw, -half, half);
            this.residualPitch = MathHelper.clamp(totalPitch - quantPitch, -half, half);
            outYaw = this.yaw + quantYaw;
            outPitch = this.pitch + quantPitch;
        }

        outYaw = MathHelper.wrapDegrees(outYaw);
        outPitch = this.clampPitch(outPitch);

        this.yaw = outYaw;
        this.pitch = outPitch;
        return new float[]{outYaw, outPitch};
    }

    private float clampPitch(float value) {
        float lo = -90.0F * (this.limitDown.currentValue() / 100.0F);
        float hi = 90.0F * (this.limitUp.currentValue() / 100.0F);
        return MathHelper.clamp(value, lo, hi);
    }

    private float antiSnapFactor(float yawError) {
        if (!this.antiSnap.isValue()) {
            return 1.0F;
        }
        float absYaw = Math.abs(yawError);
        float threshold = this.antiSnapThreshold.currentValue();
        if (absYaw <= threshold) {
            return 1.0F;
        }
        float t = MathHelper.clamp((absYaw - threshold) / Math.max(1.0F, 180.0F - threshold), 0.0F, 1.0F);
        return MathHelper.clamp(1.0F - t * this.antiSnapStrength.currentValue(), 0.1F, 1.0F);
    }

    private float absorbFactor(Box box, Vec3d point) {
        boolean inside = box.expand(this.hitboxExpand.currentValue()).contains(point) || box.expand(0.3).contains(point);
        float target = inside ? this.hitboxSlow.currentValue() : 1.0F;
        float rate = inside ? 1.0F : this.speedReturn.currentValue();
        this.absorb = this.absorb + (target - this.absorb) * rate;
        this.absorb = MathHelper.clamp(this.absorb, this.hitboxSlow.currentValue(), 1.0F);
        return this.absorb;
    }

    private void shakeStep(float yawError, float pitchError) {
        if (!this.shakeYaw.isValue()) {
            this.shakeYawValue = 0.0F;
        } else {
            float amp = this.shakeYawAmp.currentValue();
            float speed = Math.min(this.shakeYawSpeed.currentValue() * 0.1F, 0.5F);
            float scale = MathHelper.clamp(Math.max(Math.abs(yawError) / 25.0F, Math.abs(pitchError) / 20.0F), 0.12F, 1.0F);
            this.shakeYawPhase += speed;
            float wave = (float) Math.sin(this.shakeYawPhase) * amp;
            float noise = 1.0F + (this.rng.nextFloat() - 0.5F) * this.shakeYawRandom.currentValue() * 0.2F;
            this.shakeYawValue = wave * noise;
            this.shakeYawValue = this.shakeYawValue * (1.0F - this.shakeYawDamp.currentValue() * 0.5F);
            if (this.shakeYawBounce.currentValue() > 0.0F && Math.abs(wave) > amp * 0.7F) {
                this.shakeYawPhase += (float) (Math.PI * this.shakeYawBounce.currentValue() * 0.1F);
            }
            this.shakeYawValue = MathHelper.clamp(this.shakeYawValue * scale, -amp, amp);
        }

        if (!this.shakePitch.isValue()) {
            this.shakePitchValue = 0.0F;
        } else {
            float amp = this.shakePitchAmp.currentValue();
            float speed = Math.min(this.shakePitchSpeed.currentValue() * 0.1F, 0.5F);
            float scale = MathHelper.clamp(Math.max(Math.abs(yawError) / 30.0F, Math.abs(pitchError) / 18.0F), 0.1F, 1.0F);
            this.shakePitchPhase += speed;
            float wave = (float) Math.sin(this.shakePitchPhase) * amp;
            float noise = 1.0F + (this.rng.nextFloat() - 0.5F) * this.shakePitchRandom.currentValue() * 0.2F;
            this.shakePitchValue = wave * noise;
            this.shakePitchValue = this.shakePitchValue * (1.0F - this.shakePitchDamp.currentValue() * 0.5F);
            if (this.shakePitchBounce.currentValue() > 0.0F && Math.abs(wave) > amp * 0.7F) {
                this.shakePitchPhase += (float) (Math.PI * this.shakePitchBounce.currentValue() * 0.1F);
            }
            this.shakePitchValue = MathHelper.clamp(this.shakePitchValue * scale, -amp, amp);
        }
    }

    private void jitterStep(float yawError, float pitchError) {
        boolean near = Math.abs(yawError) <= 8.0F && Math.abs(pitchError) <= 6.0F;
        if (this.jitterOnlyFinal.isValue() && !near) {
            this.jitterYaw = 0.0F;
            this.jitterPitch = 0.0F;
        } else if (this.rng.nextFloat() * 100.0F <= this.jitterChance.currentValue()) {
            float strength = this.jitterStrength.currentValue();
            this.jitterYaw = (this.rng.nextFloat() - 0.5F) * 2.0F * strength;
            this.jitterPitch = this.jitterOnlyYaw.isValue() ? 0.0F : (this.rng.nextFloat() - 0.5F) * strength;
        } else {
            this.jitterYaw *= 0.2F;
            this.jitterPitch *= 0.2F;
        }
    }

    private static int gridIndex(float value, double gcd) {
        return gcd < 1.0E-6 ? 0 : (int) Math.round(value / gcd);
    }

    private static boolean rayHits(Vec3d direction, double distance, Box box) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        Vec3d eye = player.getEyePos();
        if (box.contains(eye)) {
            return true;
        }
        Vec3d end = eye.add(direction.multiply(distance));
        return box.raycast(eye, end).isPresent();
    }

    private Vec3d pointFor(LivingEntity living, ClientPlayerEntity player) {
        int maxUses = Math.max(1, (int) this.fixTime.currentValue());
        if (this.fixPoint.isValue() && this.fixedEntity == living && this.fixedPoint != null && this.fixedUses < maxUses) {
            this.fixedUses++;
            return this.fixedPoint;
        }

        Vec3d point;
        if (this.pointMode.currentValue() == PointMode.CHEST) {
            point = living.getEntityPos().add(0.0, living.getHeight() * 0.72, 0.0);
        } else if (this.pointMode.currentValue() == PointMode.HEAD) {
            point = living.getEntityPos().add(0.0, living.getHeight() * 0.92, 0.0);
        } else {
            Vec3d closest = nearestPointOnBox(living, player);
            point = visiblePoint(living, closest != null ? closest : living.getBoundingBox().getCenter(), this.attackDistance.currentValue());
        }

        if (this.multipoint.isValue()) {
            float speed = this.multipointSpeed.currentValue();
            point = switch (this.multipointMode.currentValue()) {
                case AGGRESSIVE -> aggressivePoint(living, speed);
                case SKELETAL -> skeletalPoint(living);
                case GAUSS -> gaussPoint(living, speed);
                case ORGANIC -> organicPoint(living, speed);
                case SPIRAL -> spiralPoint(living, speed);
                case PERIMETER -> perimeterPoint(living, speed);
                case KINETIC -> kineticPoint(living, speed, player);
                default -> softPoint(living, speed);
            };
        }

        if (this.fixPoint.isValue()) {
            this.fixedEntity = living;
            this.fixedPoint = point;
            this.fixedUses = 0;
        }
        return point != null ? point : living.getBoundingBox().getCenter();
    }

    /** BacktrackAimGeometry.b: the box point nearest to the eye, sampled every 0.1 blocks. */
    private static Vec3d nearestPointOnBox(LivingEntity living, ClientPlayerEntity player) {
        Box box = living.getBoundingBox();
        Vec3d eye = player.getEyePos();
        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;
        for (double x = box.minX; x <= box.maxX; x += 0.1) {
            for (double y = box.minY; y <= box.maxY; y += 0.1) {
                for (double z = box.minZ; z <= box.maxZ; z += 0.1) {
                    Vec3d candidate = new Vec3d(x, y, z);
                    double dist = eye.distanceTo(candidate);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    /** BacktrackAimGeometry.a(entity, point, reach): the point itself if visible, else the nearest visible box point. */
    private static Vec3d visiblePoint(LivingEntity living, Vec3d point, double reach) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (point == null || player == null || Mc.INSTANCE.getWorld() == null) {
            return point;
        }
        if (isVisible(living, point, reach)) {
            return point;
        }

        Box box = living.getBoundingBox();
        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;
        for (double x = box.minX; x <= box.maxX; x += 0.12) {
            for (double y = box.minY; y <= box.maxY; y += 0.12) {
                for (double z = box.minZ; z <= box.maxZ; z += 0.12) {
                    Vec3d candidate = new Vec3d(x, y, z);
                    if (isVisible(living, candidate, reach)) {
                        double dist = candidate.squaredDistanceTo(point);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = candidate;
                        }
                    }
                }
            }
        }
        return best != null ? best : point;
    }

    private static boolean isVisible(LivingEntity living, Vec3d point, double reach) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        Vec3d eye = player.getEyePos();
        double dist = eye.distanceTo(point);
        if (dist > reach) {
            return false;
        }
        Vec3d direction = point.subtract(eye).normalize();
        if (!rayHits(direction, dist + 0.2, living.getBoundingBox())) {
            return false;
        }
        BlockHitResult block = Mc.INSTANCE.getWorld().raycast(
            new RaycastContext(eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return block.getType() == HitResult.Type.MISS
            || eye.squaredDistanceTo(block.getPos()) >= eye.squaredDistanceTo(point) - 1.0E-4;
    }

    private static double lerp(double t, double start, double end) {
        return start + (end - start) * t;
    }

    private Vec3d softPoint(LivingEntity living, float speed) {
        Box box = living.getBoundingBox();
        double dx = box.getLengthX();
        double dy = box.getLengthY();
        double dz = box.getLengthZ();
        double cx = box.minX + dx * 0.5;
        double cy = box.minY + dy * 0.7;
        double cz = box.minZ + dz * 0.5;
        double phase = (double) System.currentTimeMillis() / 50.0 * speed;
        int id = living.getId();
        return new Vec3d(
            cx + Math.sin(phase + id) * dx * 0.45,
            cy + Math.cos(phase * 0.8 + id) * dy * 0.1,
            cz + Math.cos(phase * 1.2 + id) * dz * 0.45);
    }

    private Vec3d aggressivePoint(LivingEntity living, float speed) {
        Box box = living.getBoundingBox();
        double dx = box.getLengthX();
        double dy = box.getLengthY();
        double dz = box.getLengthZ();
        double cx = box.minX + dx * 0.5;
        double cy = box.minY + dy * 0.65;
        double cz = box.minZ + dz * 0.5;
        double phase = (double) System.currentTimeMillis() / 65.0 * speed;
        int id = living.getId();
        return new Vec3d(
            cx + Math.sin(phase + id) * dx * 0.7,
            cy + Math.cos(phase * 0.8 + id) * dy * 0.4,
            cz + Math.cos(phase * 1.2 + id) * dz * 0.7);
    }

    private Vec3d skeletalPoint(LivingEntity living) {
        Box box = living.getBoundingBox();
        double cx = box.minX + box.getLengthX() * 0.5;
        double cz = box.minZ + box.getLengthZ() * 0.5;
        double dy = box.getLengthY();
        double phase = (double) System.currentTimeMillis() / 400.0;
        double wave = Math.abs(Math.sin(phase + living.getId()));
        double ratio;
        if (living.isSneaking()) {
            ratio = 0.55 + wave * 0.25;
        } else if (!(living.fallDistance > 0.5F) && living.isOnGround()) {
            ratio = 0.6 + wave * 0.32;
        } else {
            ratio = 0.45 + wave * 0.35;
        }
        return new Vec3d(cx, box.minY + dy * ratio, cz);
    }

    private Vec3d gaussPoint(LivingEntity living, float speed) {
        Box box = living.getBoundingBox();
        double dx = box.getLengthX();
        double dy = box.getLengthY();
        double dz = box.getLengthZ();
        double cx = box.minX + dx * 0.5;
        double cy = box.minY + dy * 0.65;
        double cz = box.minZ + dz * 0.5;
        double phase = (double) System.currentTimeMillis() / 150.0 * speed;
        int id = living.getId();
        double u1 = Math.max(1.0E-6, Math.abs((Math.sin(phase * 1.1 + id) + 1.0) * 0.5));
        double u2 = (Math.cos(phase * 0.9 + id * 2) + 1.0) * 0.5;
        double radius = Math.sqrt(-2.0 * Math.log(u1));
        double angle = Math.PI * 2 * u2;
        double offX = MathHelper.clamp(radius * Math.cos(angle) * 0.18 * dx, -dx * 0.4, dx * 0.4);
        double offZ = MathHelper.clamp(radius * Math.sin(angle) * 0.18 * dz, -dz * 0.4, dz * 0.4);
        double offY = MathHelper.clamp(Math.sin(phase * 1.3) * 0.2 * dy, -dy * 0.25, dy * 0.25);
        return new Vec3d(cx + offX, cy + offY, cz + offZ);
    }

    private Vec3d organicPoint(LivingEntity living, float speed) {
        Box box = living.getBoundingBox();
        double dx = box.getLengthX();
        double dy = box.getLengthY();
        double dz = box.getLengthZ();
        if (this.organicOffset == null || this.organicNoise == null) {
            this.organicOffset = new Vec3d(0.0, dy * 0.6, 0.0);
            this.organicNoise = new Vec3d(
                (this.rng.nextFloat() - 0.5F) * 0.02,
                (this.rng.nextFloat() - 0.5F) * 0.01,
                (this.rng.nextFloat() - 0.5F) * 0.02);
        }

        double halfX = (dx - 0.08) * 0.5;
        double halfZ = (dz - 0.08) * 0.5;
        double amp = 0.015 * speed;
        double nx = this.organicNoise.x + (this.rng.nextFloat() - 0.5F) * amp;
        double ny = this.organicNoise.y + (this.rng.nextFloat() - 0.5F) * amp * 0.6;
        double nz = this.organicNoise.z + (this.rng.nextFloat() - 0.5F) * amp;
        nx = MathHelper.clamp(nx, -amp * 2.0, amp * 2.0);
        ny = MathHelper.clamp(ny, -amp * 1.5, amp * 1.5);
        nz = MathHelper.clamp(nz, -amp * 2.0, amp * 2.0);

        double px = this.organicOffset.x + nx;
        double py = this.organicOffset.y + ny;
        double pz = this.organicOffset.z + nz;
        if (px > halfX || px < -halfX) {
            nx = -nx;
            px = MathHelper.clamp(px, -halfX, halfX);
        }
        if (py > dy * 0.85 || py < dy * 0.35) {
            ny = -ny;
            py = MathHelper.clamp(py, dy * 0.35, dy * 0.85);
        }
        if (pz > halfZ || pz < -halfZ) {
            nz = -nz;
            pz = MathHelper.clamp(pz, -halfZ, halfZ);
        }

        this.organicNoise = new Vec3d(nx, ny, nz);
        this.organicOffset = new Vec3d(px, py, pz);
        return new Vec3d(box.minX + dx * 0.5 + px, box.minY + py, box.minZ + dz * 0.5 + pz);
    }

    private Vec3d spiralPoint(LivingEntity living, float speed) {
        Box box = living.getBoundingBox();
        double dx = box.getLengthX() * 0.42;
        double dy = box.getLengthY();
        double cx = box.minX + box.getLengthX() * 0.5;
        double cz = box.minZ + box.getLengthZ() * 0.5;
        double phase = (double) System.currentTimeMillis() / 120.0 * speed;
        double angle = phase + living.getId();
        double oy = (Math.sin(phase * 0.4) * 0.5 + 0.5) * dy * 0.5 + dy * 0.35;
        return new Vec3d(cx + Math.cos(angle) * dx, box.minY + oy, cz + Math.sin(angle) * dx);
    }

    private Vec3d perimeterPoint(LivingEntity living, float speed) {
        Box box = living.getBoundingBox();
        double dx = box.getLengthX() * 0.46;
        double dz = box.getLengthZ() * 0.46;
        double dy = box.getLengthY();
        double cx = box.minX + box.getLengthX() * 0.5;
        double cz = box.minZ + box.getLengthZ() * 0.5;
        double t = (double) System.currentTimeMillis() / 200.0 * speed + living.getId();
        double pos = Math.abs(t % 4.0);
        double px;
        double pz;
        if (pos < 1.0) {
            px = lerp(pos, -dx, dx);
            pz = -dz;
        } else if (pos < 2.0) {
            px = dx;
            pz = lerp(pos - 1.0, -dz, dz);
        } else if (pos < 3.0) {
            px = lerp(pos - 2.0, dx, -dx);
            pz = dz;
        } else {
            px = -dx;
            pz = lerp(pos - 3.0, dz, -dz);
        }
        double oy = (Math.cos(t * 0.5) * 0.15 + 0.65) * dy;
        return new Vec3d(cx + px, box.minY + oy, cz + pz);
    }

    private Vec3d kineticPoint(LivingEntity living, float speed, ClientPlayerEntity player) {
        Box box = living.getBoundingBox();
        double cx = box.minX + box.getLengthX() * 0.5;
        double cz = box.minZ + box.getLengthZ() * 0.5;
        double cy = box.minY + box.getLengthY() * 0.68;
        Vec3d velocity = player.getVelocity();
        double vx = (living.getX() - living.lastX - velocity.x) * 2.5 * speed;
        double vz = (living.getZ() - living.lastZ - velocity.z) * 2.5 * speed;
        double limit = box.getLengthX() * 0.45;
        vx = MathHelper.clamp(vx, -limit, limit);
        vz = MathHelper.clamp(vz, -limit, limit);
        return new Vec3d(cx + vx, cy, cz + vz);
    }
}
