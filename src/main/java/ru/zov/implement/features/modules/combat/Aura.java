package ru.zov.implement.features.modules.combat;

import ru.zov.implement.features.modules.combat.killaura.module.Module;
import ru.zov.implement.features.modules.combat.killaura.module.AntiBotModule;
import ru.zov.implement.features.modules.combat.killaura.module.AutoJumpModule;
import ru.zov.implement.features.modules.combat.killaura.module.AutoTotemModule;
import ru.zov.implement.features.modules.combat.killaura.util.ActionScheduler;
import ru.zov.implement.features.modules.combat.killaura.annotation.Aliases;
import ru.zov.implement.features.modules.combat.killaura.type.AuraRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.AuraTargetType;
import ru.zov.implement.features.modules.combat.killaura.util.BlockUtil;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.BooleanSetting;
import ru.zov.implement.features.modules.combat.killaura.type.CombatPauseManager;
import ru.zov.implement.features.modules.combat.killaura.math.DirectionalInput;
import ru.zov.implement.features.modules.combat.killaura.type.EntityCategory;
import ru.zov.implement.features.modules.combat.killaura.model.EntityFilter;
import ru.zov.implement.features.modules.combat.killaura.internal.EventPhaseSwitchMap;
import ru.zov.implement.features.modules.combat.killaura.type.EventPriority;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.util.EquipmentUtil;
import ru.zov.implement.features.modules.combat.killaura.type.BezierRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.FuntimeRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.GrimRotationMode;
import ru.zov.implement.features.modules.combat.killaura.util.GrimDelayHandler;
import ru.zov.implement.features.modules.combat.killaura.type.HolyWorldRotationMode;
import ru.zov.implement.features.modules.combat.killaura.util.HitPointResolver;
import ru.zov.implement.features.modules.combat.killaura.type.InventoryScope;
import ru.zov.implement.features.modules.combat.killaura.Lang;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.type.MaxDamageItem;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.ModeSetting;
import ru.zov.implement.features.modules.combat.killaura.ui.ModuleTab;
import ru.zov.implement.features.modules.combat.killaura.type.MoveCorrectionMode;
import ru.zov.implement.features.modules.combat.killaura.type.MovementInputState;
import ru.zov.implement.features.modules.combat.killaura.event.MovementInputEvent;
import ru.zov.implement.features.modules.combat.killaura.util.MovementInputHelper;
import ru.zov.implement.features.modules.combat.killaura.event.MovementUpdateEvent;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.MultiSelectSetting;
import ru.zov.implement.features.modules.combat.killaura.type.NotificationType;
import ru.zov.implement.features.modules.combat.killaura.ui.setting.NumberSetting;
import ru.zov.implement.features.modules.combat.killaura.event.PacketReceiveEvent;
import ru.zov.implement.features.modules.combat.killaura.event.PacketSendEvent;
import ru.zov.implement.features.modules.combat.killaura.util.PlayerSnapshotManager;
import ru.zov.implement.features.modules.combat.killaura.util.PlayerActionUtil;
import ru.zov.implement.features.modules.combat.killaura.event.PlayerTickEvent;
import ru.zov.implement.features.modules.combat.killaura.util.RaycastUtil;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.config.RotationConfig;
import ru.zov.implement.features.modules.combat.killaura.util.RotationManager;
import ru.zov.implement.features.modules.combat.killaura.math.RotationMath;
import ru.zov.implement.features.modules.combat.killaura.type.RotationMode;
import ru.zov.implement.features.modules.combat.killaura.event.RotationUpdateEvent;
import ru.zov.implement.features.modules.combat.killaura.math.RotationVector;
import ru.zov.implement.features.modules.combat.killaura.util.ServerUtil;
import ru.zov.implement.features.modules.combat.killaura.type.SettingUnit;
import ru.zov.implement.features.modules.combat.killaura.math.SimulatedPlayer;
import ru.zov.implement.features.modules.combat.killaura.model.ScopedSlot;
import ru.zov.implement.features.modules.combat.killaura.model.SlotSearchResult2;
import ru.zov.implement.features.modules.combat.killaura.util.SlotSyncHandler;
import ru.zov.implement.features.modules.combat.killaura.type.SmoothRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.SlothAcRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.SpookyTimeRotationMode;
import ru.zov.implement.features.modules.combat.killaura.math.Stopwatch;
import ru.zov.implement.features.modules.combat.killaura.util.SwapUtil;
import ru.zov.implement.features.modules.combat.killaura.type.TargetSortMode;
import ru.zov.implement.features.modules.combat.killaura.util.TargetSorters;
import ru.zov.implement.features.modules.combat.killaura.event.UseItemEvent;
import ru.zov.implement.features.modules.combat.killaura.type.MlsRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.NeuroRotationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Aliases(aliases = {"Attack Aura", "Kill Aura", "Hit Aura", "Aura"})
public final class Aura extends Module {
    private static Aura instance;
    private static final float MIN_MACE_FALL_DISTANCE = 1.5f;
    private static final int MACE_HOLD_TIMEOUT_TICKS = 40;
    private static final int MACE_RESTORE_DELAY_TICKS = 2;
    private static final long MACE_SWAP_COOLDOWN_MS = 300L;
    private static final float MACE_AIM_DISTANCE_MARGIN = 2.0f;
    private static final int MACE_PITCH_PREDICTION_TICKS = 20;
    private static final double MACE_PITCH_REACH_TOLERANCE = 0.15D;
    private static final double SPOOKY_AIM_POINT_SWITCH_MARGIN = 0.75D;
    private static final double SPOOKY_DISTANCE_APPROACH_GAP = 0.36D;
    private static final double SPOOKY_AIR_APPROACH_GAP = 0.46D;
    private static final double SPOOKY_FINE_STEP_GAP = 0.90D;
    private static final double SPOOKY_AIR_PREDICTION_TICKS = 1.5D;
    public final ModeSetting<AuraRotationMode> rotationMode;
    public final MultiSelectSetting<AuraTargetType> targetTypes;
    public final MultiSelectSetting<TargetSortMode> sortModes;
    public final ModeSetting<MoveCorrectionMode> moveCorrectionMode;

    public final NumberSetting extraDistance;
    public final NumberSetting maxDistance;
    public final BooleanSetting autoMace;
    public final NumberSetting autoMaceHeight;
    public final BooleanSetting maxDamage;
    public final ModeSetting<MaxDamageItem> maxDamageItem;
    public final BooleanSetting onlyCriticalHit;
    public final BooleanSetting critsWithSpace;
    public final BooleanSetting keepSprintWhenPossible;
    public final BooleanSetting ignoreWalls;
    public final BooleanSetting ignoreNaked;
    public final BooleanSetting noHitWhileEatingOffhand;
    public final BooleanSetting unpressShield;
    public final BooleanSetting breakShield;
    public final BooleanSetting tpsSync;
    public final BooleanSetting rotationReturn;
    public final Mc mc;
    public final HitPointResolver hitPointResolver;

    public final Stopwatch attackTimer;

    public final Stopwatch lookingTimer;
    private final FuntimeRotationMode funtimeRotationMode;
    private final SpookyTimeRotationMode spookyTimeRotationMode;
    private final SmoothRotationMode smoothRotationMode;
    private final GrimRotationMode grimRotationMode;
    private final HolyWorldRotationMode holyWorldRotationMode;
    private final SlothAcRotationMode slothAcRotationMode;
    private final BezierRotationMode bezierRotationMode;
    private final MlsRotationMode mlsRotationMode = new MlsRotationMode();
    private final NeuroRotationMode neuroRotationMode = new NeuroRotationMode();
    public int attackCooldown;
    public int sprintTicks;
    public int hitCounter;
    public LivingEntity target;

    public LivingEntity lastTarget;
    public final int[] funtimeAttackDelays;

    public final int[] smoothAttackDelays;
    public final int[] grimAttackDelays;

    public final ActionScheduler actionScheduler;

    public Vec3d lastAttackVector;
    private SlotSearchResult2 preparedMace;
    private int maceRestoreSelectedSlot;
    private int maceInventorySourceSlot;
    private int maceInventoryTargetSlot;
    private int macePreparedTicks;
    private int maceRestoreAtAge;
    private long maceSwapCooldownUntil;
    private boolean maceSwapScheduled;
    private boolean maceRestoreScheduled;
    private boolean maceInternalSlotChange;
    private boolean maceAttackedThisFall;
    private int maxDamageSwapSourceSlot;
    private int maxDamagePreparedAtAge;
    private int maxDamageRestoreAtAge;
    private boolean maxDamageSwapScheduled;
    private boolean maxDamageRestoreScheduled;
    private MaxDamageItem preparedMaxDamageItem;
    private HitPointResolver.StableAimPoint spookyAimPoint;
    private int spookyDistanceTargetId = Integer.MIN_VALUE;
    private boolean auraRotationActive;
    private AuraRotationMode observedRotationMode;
    private ClientPlayerEntity visualSprintPlayer;
    private int visualSprintUntilAge;
    private float visualSprintEyeHeight;
    private EntityPose visualSprintPose;
    private boolean visualSprintSwimming;
    private float visualSprintLeaningPitch;

    public Aura() {
        super(ModuleTab.COMBAT, "Attack Aura");
        instance = this;
        this.funtimeRotationMode = new FuntimeRotationMode();
        this.spookyTimeRotationMode = new SpookyTimeRotationMode();
        this.smoothRotationMode = new SmoothRotationMode();
        this.grimRotationMode = new GrimRotationMode();
        this.holyWorldRotationMode = new HolyWorldRotationMode();
        this.slothAcRotationMode = new SlothAcRotationMode();
        this.bezierRotationMode = new BezierRotationMode();
        this.rotationMode = new ModeSetting(Lang.ATTACKAURA_ROTATION_MODE).values(AuraRotationMode.class);
        this.targetTypes = new MultiSelectSetting(Lang.SELECTTARGETS).values(AuraTargetType.class).select(AuraTargetType.PLAYERS);
        this.sortModes = new MultiSelectSetting(Lang.ATTACKAURA_SORT_TYPE).values(TargetSortMode.class).select(TargetSortMode.BY_FOV);
        this.moveCorrectionMode = new ModeSetting(Lang.ATTACKAURA_MOVE_CORRECTION, Lang.ATTACKAURA_MOVE_CORRECTION_DESC).values(MoveCorrectionMode.class).select(MoveCorrectionMode.FREE);
        this.extraDistance = new NumberSetting(Lang.ATTACKAURA_EXTRA_DISTANCE, Lang.ATTACKAURA_EXTRA_DISTANCE_DESC).currentValue(0.0f).range(0.0f, 6.0f).step(0.05f).unit(SettingUnit.BLOCKS);
        this.maxDistance = new NumberSetting(Lang.ATTACKAURA_MAX_DISTANCE).currentValue(3.0f).range(1.0f, 6.0f).step(0.05f).unit(SettingUnit.BLOCKS);
        this.autoMace = new BooleanSetting(Lang.ATTACKAURA_AUTO_MACE, Lang.ATTACKAURA_AUTO_MACE_DESC).setValue(true);
        this.autoMaceHeight = new NumberSetting(
                        Lang.ATTACKAURA_AUTO_MACE_HEIGHT,
                        Lang.ATTACKAURA_AUTO_MACE_HEIGHT_DESC)
                .currentValue(2.0F)
                .range(0.5F, 6.0F)
                .step(0.1F)
                .unit(SettingUnit.BLOCKS)
                .visible(this.autoMace::isValue);
        this.maxDamage = new BooleanSetting(
                Lang.ATTACKAURA_MAX_DAMAGE,
                Lang.ATTACKAURA_MAX_DAMAGE_DESC);
        this.maxDamageItem = new ModeSetting<MaxDamageItem>(
                        Lang.ATTACKAURA_MAX_DAMAGE_ITEM)
                .values(MaxDamageItem.class)
                .visible(this.maxDamage::isValue);
        this.onlyCriticalHit = new BooleanSetting(Lang.TRIGGERBOT_ONLY_CRITICAL_HIT);
        BooleanSetting class665Var = new BooleanSetting(Lang.ATTACKAURA_CRITS_WITH_SPACE, Lang.ATTACKAURA_CRITS_WITH_SPACE_DESC);
        BooleanSetting class665Var2 = this.onlyCriticalHit;
        Objects.requireNonNull(class665Var2);
        this.critsWithSpace = class665Var.visible(class665Var2::isValue);
        this.keepSprintWhenPossible = new BooleanSetting(
                Lang.ATTACKAURA_KEEP_SPRINT_WHEN_POSSIBLE,
                Lang.ATTACKAURA_KEEP_SPRINT_WHEN_POSSIBLE_DESC);
        this.ignoreWalls = new BooleanSetting(Lang.ATTACKAURA_IGNORE_WALLS, Lang.ATTACKAURA_IGNORE_WALLS_DESC);
        this.ignoreNaked = new BooleanSetting(Lang.ATTACKAURA_IGNORE_NAKED);
        this.noHitWhileEatingOffhand = new BooleanSetting(Lang.ATTACKAURA_NO_HIT_WHILE_EATING_OFFHAND);
        this.unpressShield = new BooleanSetting(Lang.ATTACKAURA_UNPRESS_SHIELD);
        this.breakShield = new BooleanSetting(Lang.ATTACKAURA_BREAK_SHIELD);
        this.tpsSync = new BooleanSetting(Lang.ATTACKAURA_TPS_SYNC);
        this.rotationReturn = new BooleanSetting(
                        Lang.ATTACKAURA_ROTATION_RETURN,
                        Lang.ATTACKAURA_ROTATION_RETURN_DESC)
                .setValue(true);
        this.mc = Mc.INSTANCE;
        this.hitPointResolver = new HitPointResolver();
        this.attackTimer = new Stopwatch();
        this.lookingTimer = new Stopwatch();
        this.attackCooldown = 0;
        this.sprintTicks = 0;
        this.target = null;
        this.lastTarget = null;
        this.funtimeAttackDelays = new int[]{10, 10, 10, 10, 12};
        this.smoothAttackDelays = new int[]{10, 10, 10, 13};
        this.grimAttackDelays = new int[]{10, 11};
        this.actionScheduler = new ActionScheduler();
        this.lastAttackVector = Vec3d.ZERO;
        this.preparedMace = null;
        this.maceRestoreSelectedSlot = -1;
        this.maceInventorySourceSlot = -1;
        this.maceInventoryTargetSlot = -1;
        this.macePreparedTicks = 0;
        this.maceRestoreAtAge = Integer.MIN_VALUE;
        this.maceSwapCooldownUntil = 0L;
        this.maceSwapScheduled = false;
        this.maceRestoreScheduled = false;
        this.maceInternalSlotChange = false;
        this.maceAttackedThisFall = false;
        this.maxDamageSwapSourceSlot = -1;
        this.maxDamagePreparedAtAge = Integer.MIN_VALUE;
        this.maxDamageRestoreAtAge = Integer.MIN_VALUE;
        this.maxDamageSwapScheduled = false;
        this.maxDamageRestoreScheduled = false;
        this.preparedMaxDamageItem = null;
        this.spookyAimPoint = null;
        this.observedRotationMode = null;
        this.visualSprintPlayer = null;
        this.visualSprintUntilAge = Integer.MIN_VALUE;
        this.visualSprintPose = EntityPose.STANDING;
        addSettings(this.rotationMode, this.targetTypes, this.sortModes, this.moveCorrectionMode, this.rotationReturn, this.extraDistance, this.maxDistance, this.autoMace, this.autoMaceHeight, this.maxDamage, this.maxDamageItem, this.onlyCriticalHit, this.critsWithSpace, this.keepSprintWhenPossible, this.ignoreWalls, this.ignoreNaked, this.noHitWhileEatingOffhand, this.unpressShield, this.breakShield, this.tpsSync);
        register(UseItemEvent.class, class059Var -> {
            if (isState() && this.mc.isWorldLoaded() && this.target != null && canCrit(this.mc.getPlayer(), 0)) {
                class059Var.cancel();
            }
        });
        register(PacketSendEvent.class, class037Var -> {
            if (isState() && this.mc.isWorldLoaded() && this.target != null) {
                if ((class037Var.getPacket() instanceof HandSwingC2SPacket)
                        || (class037Var.getPacket() instanceof UpdateSelectedSlotC2SPacket
                                && !this.maceInternalSlotChange)) {
                    this.attackCooldown = Math.max(0, Math.round(getAttackDelay() * (this.tpsSync.isValue() ? 20.0f / MathUtil.clamp(ServerUtil.TPS, 1.0f, 20.0f) : 1.0f)));
                }
            }
        });
        register(PacketReceiveEvent.class, class051Var -> {
            if (isState() && this.mc.isWorldLoaded() && this.target != null) {
                if ((class051Var.getPacket()) instanceof EntityStatusS2CPacket packet ) {
                    EntityStatusS2CPacket entityStatusS2CPacket = packet;
                    if (entityStatusS2CPacket.getStatus() == 30 && entityStatusS2CPacket.getEntity(this.mc.getWorld()) == this.target) {
                        AuraRuntime.INSTANCE.notificationRepository().post(NotificationType.SUCCESS, (Text) Text.literal("Ð¡Ð»Ð¾Ð¼Ð°Ð» Ñ‰Ð¸Ñ‚ Ð¸Ð³Ñ€Ð¾ÐºÑƒ " + this.target.getName().getString()), 3L, TimeUnit.SECONDS);
                    }
                }
            }
        });
        register(PlayerTickEvent.class, class130Var -> {
            if (this.mc.isWorldLoaded() && class130Var.isPre()) {
                ClientPlayerEntity player = this.mc.getPlayer();
                if (player != null
                        && this.maxDamageRestoreAtAge != Integer.MIN_VALUE
                        && player.age >= this.maxDamageRestoreAtAge) {
                    restoreMaxDamageItem(player);
                }
                if (player != null
                        && this.maceRestoreAtAge != Integer.MIN_VALUE
                        && player.age >= this.maceRestoreAtAge) {
                    queueMaceRestore(player);
                }
                this.actionScheduler.cleanupIfFinished().update();
            }
        });
        register(MovementUpdateEvent.class, class308Var -> {
            if (isState()
                    && this.mc.isWorldLoaded()
                    && class308Var.getDirectionalInput().isMoving()
                    && this.sprintTicks > 0
                    && class308Var.isSprint()
                    && shouldResetSprintForAttack(this.mc.getPlayer(), 0)) {
                class308Var.setStopProgression(true);
                class308Var.setSprint(false);
            }
        }, EventPriority.HIGH);
        register(RotationUpdateEvent.class, class345Var -> {
            if (this.mc.isWorldLoaded()) {
                ClientPlayerInteractionManager interactionManager = Mc.INSTANCE.getInteractionManager();
                ClientPlayerEntity player = this.mc.getPlayer();
                RotationManager rotationManager = RotationManager.INSTANCE;
                switch (EventPhaseSwitchMap.phaseOrdinals[class345Var.stage().ordinal()]) {
                    case 1:
                        updateRotation(player, rotationManager);
                        break;
                    case 2:
                        performAttack(player, interactionManager, rotationManager);
                        break;
                }
            }
        });
        register(MovementInputEvent.class, class040Var -> {
            ClientPlayerEntity player;
            if (!isState()
                    || !this.mc.isWorldLoaded()
                    || (player = this.mc.getPlayer()) == null
                    || this.target == null) {
                resetSpookyDistanceControl();
                return;
            }

            if (isSpookyTimeMode(this.rotationMode.currentValue())
                    && (this.moveCorrectionMode.isSelected(MoveCorrectionMode.FOCUS)
                            || this.moveCorrectionMode.isSelected(MoveCorrectionMode.TARGET))) {
                boolean targetCorrection =
                        this.moveCorrectionMode.isSelected(MoveCorrectionMode.TARGET);
                if (targetCorrection || this.mc.getGameOptions().forwardKey.isPressed()) {
                    class040Var.setInput(createSpookyDistanceInput(player));
                }
                return;
            }

            resetSpookyDistanceControl();
            if (this.moveCorrectionMode.isSelected(MoveCorrectionMode.TARGET)) {
                class040Var.setInput(MovementInputHelper.getDirectionalInputForDegrees(
                        DirectionalInput.NONE,
                        MovementInputHelper.getDegreesRelativeToView(
                                this.target.getEntityPos().subtract(player.getEntityPos()),
                                player.getYaw())));
                if (player.isSwimming()) {
                    class040Var.setJumping(
                            this.target.getEyeY()
                                    > player.getEyeY() - (player.isSwimming() ? 0.0D : 1.0D));
                }
            }
        }, EventPriority.HIGH);
    }

    public void updateRotation(ClientPlayerEntity clientPlayerEntity, RotationManager rotationManager) {
        int i;
        int i2;
        AuraRotationMode selectedMode = this.rotationMode.currentValue();
        if (selectedMode != this.observedRotationMode) {
            if (isSpookyTimeMode(selectedMode)
                    || isSpookyTimeMode(this.observedRotationMode)) {
                this.spookyTimeRotationMode.reset();
                this.spookyAimPoint = null;
            }
            this.observedRotationMode = selectedMode;
        }
        boolean spookyTime = isSpookyTimeMode(selectedMode);
        if (isState()) {
            if (this.autoMace.isValue()) {
                updateMaceFallState(clientPlayerEntity);
            } else {
                this.maceAttackedThisFall = false;
                requestMaceRestore(clientPlayerEntity, 0);
            }
            this.target = findTarget(clientPlayerEntity, true);
            if (this.target == null) {
                finishAuraRotationWithoutReturn(clientPlayerEntity, rotationManager);
                this.auraRotationActive = false;
                this.spookyAimPoint = null;
                requestMaceRestore(clientPlayerEntity, 0);
                restoreMaxDamageItem(clientPlayerEntity
    return;
            }
            if (this.autoMace.isValue()) {
                prepareMaceForDive(clientPlayerEntity);
            }
            boolean maceDiveAim = isMaceDiveAimActive(clientPlayerEntity, this.target);
            float fCurrentValue = maceDiveAim
                    ? getMaceAimDistance(clientPlayerEntity, this.target)
                    : this.maxDistance.currentValue() + this.extraDistance.currentValue();
            if (clientPlayerEntity.isCreative()) {
                fCurrentValue = (float) Math.max(fCurrentValue, clientPlayerEntity.getEntityInteractionRange());
            }
            RotationMode selectedRotationMode = getRotationMode();
            Vec3d aimPoint;
            if (spookyTime) {
                Optional<HitPointResolver.StableAimPoint> resolvedAimPoint = this.hitPointResolver.resolveStablePoint(
                        this.target,
                        clientPlayerEntity,
                        fCurrentValue,
                        rotationManager.getCurrentRotation(),
                        this.ignoreWalls.isValue(),
                        this.spookyAimPoint,
                        SPOOKY_AIM_POINT_SWITCH_MARGIN
                );
                if (resolvedAimPoint.isEmpty()) {
                    this.spookyAimPoint = null;
                    this.lastTarget = this.target;
                    return;
                }
                this.spookyAimPoint = resolvedAimPoint.get();
                aimPoint = this.spookyAimPoint.point();
            } else {
                aimPoint = (Vec3d) this.hitPointResolver.computeVector(
                        this.target,
                        fCurrentValue,
                        rotationManager.getCurrentRotation(),
                        selectedRotationMode.randomValue(),
                        false
                ).getLeft();
            }
            Vec3d vec3dSubtract = aimPoint.subtract(clientPlayerEntity.getEyePos());
            if (vec3dSubtract == null) {
                return;
            }
            Rotation targetRotation = RotationMath.INSTANCE.fromVec3d(vec3dSubtract);
            if (maceDiveAim) {
                Optional<Float> predictedPitch = predictMaceImpactPitch(
                        clientPlayerEntity,
                        this.target);
                if (predictedPitch.isPresent()) {
                    targetRotation = new Rotation(
                            targetRotation.getYaw(),
                            predictedPitch.get());
                }
            }
            if (this.rotationMode.isSelected(AuraRotationMode.GRIM)) {
                i = 2;
            } else {
                i = this.rotationMode.isSelected(AuraRotationMode.FUNTIME) ? 20 : 5;
            }
            int i3 = i;
            if (this.rotationMode.isSelected(AuraRotationMode.GRIM)) {
                i2 = 4;
            } else {
                i2 = this.rotationMode.isSelected(AuraRotationMode.FUNTIME) ? 5 : 0;
            }
            int i4 = i2;
            if (!maceDiveAim
                    && !this.rotationMode.isSelected(AuraRotationMode.GRIM)
                    && i4 > 0
                    && !canAttack(clientPlayerEntity, i4)) {
                this.lastTarget = this.target;
            } else {
                rotationManager.scheduleRotation(new RotationVector(targetRotation, vec3dSubtract), this.target, new RotationConfig(selectedRotationMode, false, !this.moveCorrectionMode.isSelected(MoveCorrectionMode.NONE), this.moveCorrectionMode.isSelected(MoveCorrectionMode.FOCUS)), 1, this, i3);
                this.auraRotationActive = true;
                this.lastTarget = this.target;
            }
        } else {
            this.spookyTimeRotationMode.reset();
            this.spookyAimPoint = null;
            requestMaceRestore(clientPlayerEntity, 0);
            restoreMaxDamageItem(clientPlayerEntity);
        }
    }

    public static Aura getInstance() {
        return instance;
    }

    public void performAttack(ClientPlayerEntity clientPlayerEntity, ClientPlayerInteractionManager clientPlayerInteractionManager, RotationManager rotationManager) {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.sprintTicks > 0) {
            this.sprintTicks--;
        }
        if (!isState() || this.target == null) {
            restoreMaxDamageItem(clientPlayerEntity);
            return;
        }
        if (this.autoMace.isValue()
                && (this.maceSwapScheduled
                        || this.maceRestoreScheduled)) {
            return;
        }
        float fCurrentValue = this.maxDistance.currentValue();
        if (clientPlayerEntity.isCreative()) {
            fCurrentValue = (float) Math.max(fCurrentValue, clientPlayerEntity.getEntityInteractionRange());
        }
        Vec3d attackDirection = RotationMath.INSTANCE.rotationToVector(rotationManager.getCurrentRotation());
        boolean zRayTrace = isSpookyTimeMode(this.rotationMode.currentValue())
                ? RaycastUtil.traceTargetLineOfSight(
                        clientPlayerEntity.getEyePos(),
                        attackDirection,
                        fCurrentValue,
                        clientPlayerEntity,
                        this.target,
                        this.ignoreWalls.isValue()
                ).isPresent()
                : RaycastUtil.rayTrace(this.hitPointResolver, this.target, clientPlayerEntity.getEyePos(), attackDirection, fCurrentValue, this.target.getBoundingBox(), this.ignoreWalls.isValue());
        boolean canAttackSoon = canAttack(clientPlayerEntity, 1);
        boolean maxDamageSoon = this.maxDamage.isValue()
                && (shouldUseMaxDamage(clientPlayerEntity, 0)
                || shouldUseMaxDamage(clientPlayerEntity, 1)
                || shouldUseMaxDamage(clientPlayerEntity, 2));
        if (canAttack(clientPlayerEntity, 2) && maxDamageSoon) {
            prepareMaxDamageItem(clientPlayerEntity);
        } else if (!maxDamageSoon) {
            restoreMaxDamageItem(clientPlayerEntity);
        }
        if (canAttackSoon) {
            if (shouldResetSprintForAttack(clientPlayerEntity, 1)) {
                this.sprintTicks = 1;
                clientPlayerEntity.setSprinting(false);
            }
            if (this.unpressShield.isValue() && clientPlayerEntity.isUsingItem() && clientPlayerEntity.getActiveItem().getItem() == Items.SHIELD && this.mc.getGameOptions().useKey.isPressed() && this.actionScheduler.isFinished()) {
                clientPlayerInteractionManager.stopUsingItem(clientPlayerEntity);
                this.mc.getGameOptions().useKey.setPressed(false);
                this.actionScheduler.cleanup().addTickStep(1, () -> {
                    this.mc.getGameOptions().useKey.setPressed(true);
                });
            }
        }
        boolean requiresSprintReset =
                shouldResetSprintForAttack(clientPlayerEntity, 0);
        if (zRayTrace
                && canAttack(clientPlayerEntity, 0)
                && (!requiresSprintReset || !SlotSyncHandler.lastSprinting)
                && !CombatPauseManager.INSTANCE.shouldPauseCombat()) {
            if (this.autoMace.isValue() && isMaceSmashReady(clientPlayerEntity, 0)) {
                performMaceAttack(clientPlayerEntity, clientPlayerInteractionManager, rotationManager);
            } else {
                if (attackTarget(clientPlayerEntity, clientPlayerInteractionManager, this.target)) {
                    breakShieldWithAxe(clientPlayerEntity, clientPlayerInteractionManager);
                }
            }
        }
    }

    public void breakShieldWithAxe(ClientPlayerEntity clientPlayerEntity, ClientPlayerInteractionManager clientPlayerInteractionManager) {
        if (this.breakShield.isValue()) {
            if (this.target.getActiveItem().getItem() == Items.SHIELD && isActiveItemStackBlocking(this.target, 2) && Math.toDegrees(Math.acos(MathUtil.clamp(this.target.getRotationVec(1.0f).normalize().dotProduct(clientPlayerEntity.getEntityPos().subtract(this.target.getEntityPos()).normalize()), -1.0d, 1.0d))) <= 110.0d) {
                Optional<SlotSearchResult2> optionalFindItem = AuraRuntime.INSTANCE.inventoryService().searcher().findItem(itemStack -> {
                    return itemStack.getItem() instanceof AxeItem;
                }, InventoryScope.HOTBAR, InventoryScope.INVENTORY);
                if (optionalFindItem.isEmpty() || !optionalFindItem.get().found()) {
                    return;
                }
                SwapUtil.swapAndExecute(optionalFindItem.get().slotReference().increasedSlot(), RotationManager.INSTANCE.getCurrentRotation(), true, () -> {
                    attackEntityVanillaSafe(
                            clientPlayerEntity,
                            clientPlayerInteractionManager,
                            this.target);
                    clientPlayerEntity.swingHand(Hand.MAIN_HAND);
                });
            }
        }
    }
       @Override
    public void deactivate() {
        ClientPlayerEntity player = this.mc.getPlayer();
        finishAuraRotationWithoutReturn(player, RotationManager.INSTANCE);
        RotationManager.INSTANCE.cancelTasksFor(this);
        this.auraRotationActive = false;
        clearMacePreparation(player, true);
        restoreMaxDamageItem(player);
        this.maceAttackedThisFall = false;
        this.target = null;
        this.spookyAimPoint = null;
        this.spookyTimeRotationMode.reset();
        resetSpookyDistanceControl();
        this.visualSprintPlayer = null;
        this.visualSprintUntilAge = Integer.MIN_VALUE;
        super.deactivate();
    }

    public boolean isActiveItemStackBlocking(LivingEntity livingEntity, int i) {
        if (!livingEntity.isUsingItem() || livingEntity.activeItemStack.isEmpty()) {
            return false;
        }
        Item item = livingEntity.activeItemStack.getItem();
        return item.getUseAction(livingEntity.activeItemStack) == UseAction.BLOCK && item.getMaxUseTime(livingEntity.activeItemStack, livingEntity) - livingEntity.getItemUseTimeLeft() >= i;
    }

    public RotationMode getRotationMode() {
        return switch ((AuraRotationMode) this.rotationMode.currentValue()) {
            case FUNTIME -> this.funtimeRotationMode;
            case SPOOKYTIME -> this.spookyTimeRotationMode;
            case SMOOTH -> this.smoothRotationMode;
            case GRIM -> this.grimRotationMode;
            case HOLYWORLD -> this.holyWorldRotationMode;
            case SLOTH_AC -> this.slothAcRotationMode;
            case BEZIER -> this.bezierRotationMode;
            case MLS -> this.mlsRotationMode;
            case NEURO -> this.neuroRotationMode;
        };
    }

    public LivingEntity findTarget(ClientPlayerEntity clientPlayerEntity, boolean z) {
        float fCurrentValue = this.maxDistance.currentValue() + this.extraDistance.currentValue();
        boolean maceDiveTargeting = canStartMaceDiveTargeting(clientPlayerEntity);
        if (clientPlayerEntity.isCreative()) {
            fCurrentValue = (float) Math.max(fCurrentValue, clientPlayerEntity.getEntityInteractionRange());
        }
        Vec3d vec3dNormalize = RotationMath.INSTANCE.rotationToVector(Rotation.playerRotation()).normalize();
        boolean currentTargetValid = maceDiveTargeting
                ? isValidMaceDiveTarget(this.target, clientPlayerEntity)
                : isValidTarget(this.target, clientPlayerEntity, fCurrentValue);
        if (z && currentTargetValid) {
            double dDotProduct = vec3dNormalize.dotProduct(this.target.getEntityPos().subtract(clientPlayerEntity.getEntityPos()).normalize());
            double dCos = Math.cos(Math.toRadians
