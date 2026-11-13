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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.Render2DUtil;
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
import ru.zov.implement.features.modules.combat.killaura.type.ShardRotationMode;
import ru.zov.implement.features.modules.combat.killaura.type.ShardNanoRotationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
import net.minecraft.util.math.MathHelper;

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
    public final NumberSetting shardMinMinutes;
    public final NumberSetting shardMaxMinutes;
    public final NumberSetting fovSize;
    public final BooleanSetting drawFovCircle;
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
    private final ShardRotationMode shardRotationMode = new ShardRotationMode();
    private final ShardNanoRotationMode shard2RotationMode = new ShardNanoRotationMode();
    
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
    
    // Для логики FunTime (выбор точек)
    private LivingEntity lastPointTarget;
    private int pointHitCount = -1;

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
        this.shardMinMinutes = new NumberSetting(ru.zov.implement.features.modules.combat.killaura.model.Translation.clearText("Shard min minutes"))
                .currentValue(3.0f).range(1.0f, 30.0f).step(1.0f)
                .visible(() -> this.rotationMode.isSelected(AuraRotationMode.SHARD));
        this.shardMaxMinutes = new NumberSetting(ru.zov.implement.features.modules.combat.killaura.model.Translation.clearText("Shard max minutes"))
                .currentValue(9.0f).range(1.0f, 30.0f).step(1.0f)
                .visible(() -> this.rotationMode.isSelected(AuraRotationMode.SHARD));
        this.fovSize = new NumberSetting(ru.zov.implement.features.modules.combat.killaura.model.Translation.clearText("Size Fov"))
                .currentValue(45.0f).range(5.0f, 180.0f).step(1.0f).unit(SettingUnit.DEGREES)
                .visible(() -> this.rotationMode.isSelected(AuraRotationMode.FOV));
        this.drawFovCircle = new BooleanSetting(ru.zov.implement.features.modules.combat.killaura.model.Translation.clearText("Draw Circle"))
                .setValue(true)
                .visible(() -> this.rotationMode.isSelected(AuraRotationMode.FOV));
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
        
        addSettings(this.rotationMode, this.targetTypes, this.sortModes, this.moveCorrectionMode, this.rotationReturn, this.extraDistance, this.maxDistance, this.autoMace, this.autoMaceHeight, this.maxDamage, this.maxDamageItem, this.onlyCriticalHit, this.critsWithSpace, this.keepSprintWhenPossible, this.ignoreWalls, this.ignoreNaked, this.noHitWhileEatingOffhand, this.unpressShield, this.breakShield, this.tpsSync, this.shardMinMinutes, this.shardMaxMinutes, this.fovSize, this.drawFovCircle);
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
                        AuraRuntime.INSTANCE.notificationRepository().post(NotificationType.SUCCESS, (Text) Text.literal("Сломал щит игроку " + this.target.getName().getString()), 3L, TimeUnit.SECONDS);
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
            if (selectedMode == AuraRotationMode.SHARD
                    || this.observedRotationMode == AuraRotationMode.SHARD) {
                this.shardRotationMode.reset();
            }
            if (isSpookyTimeMode(selectedMode)
                    || isSpookyTimeMode(this.observedRotationMode)) {
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
                restoreMaxDamageItem(clientPlayerEntity);
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
                if (selectedMode == AuraRotationMode.FUNTIME) {
                    aimPoint = getFunTimeAimPoint(this.target, fCurrentValue, clientPlayerEntity);
                } else {
                    aimPoint = (Vec3d) this.hitPointResolver.computeVector(
                            this.target,
                            fCurrentValue,
                            rotationManager.getCurrentRotation(),
                            selectedRotationMode.randomValue(),
                            false
                    ).getLeft();
                }
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
            this.spookyAimPoint = null;
            requestMaceRestore(clientPlayerEntity, 0);
            restoreMaxDamageItem(clientPlayerEntity);
        }
    }

    private Vec3d getFunTimeAimPoint(LivingEntity target, float range, ClientPlayerEntity player) {
        if (target != lastPointTarget) {
            lastPointTarget = target;
            pointHitCount = -1;
        }
        Box box = target.getBoundingBox();
        List<Vec3d> points = new ArrayList<>();
        for (double x = 0.1; x < 0.9; x += 0.4) {
            for (double y = 0.2; y < 0.9; y += 0.3) {
                for (double z = 0.1; z < 0.9; z += 0.4) {
                    points.add(new Vec3d(
                        MathHelper.lerp(x, box.minX, box.maxX),
                        MathHelper.lerp(y, box.minY, box.maxY),
                        MathHelper.lerp(z, box.minZ, box.maxZ)
                    ));
                }
            }
        }
        pointHitCount = hitCounter;
        return points.get((int) (Math.random() * points.size()));
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
        this.shardRotationMode.reset();
        clearMacePreparation(player, true);
        restoreMaxDamageItem(player);
        this.maceAttackedThisFall = false;
        this.target = null;
        this.spookyAimPoint = null;
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

    public boolean isFovMode() {
        return this.rotationMode.isSelected(AuraRotationMode.FOV);
    }

    /** Angle between the player's look direction and the eye-to-hitbox-centre direction, against half of "Size Fov". */
    private boolean isTargetWithinFov(LivingEntity entity, ClientPlayerEntity player) {
        if (!isFovMode() || entity == null || player == null) {
            return true;
        }
        Vec3d toTarget = entity.getBoundingBox().getCenter().subtract(player.getEyePos());
        double length = toTarget.length();
        if (length < 1.0E-4D) {
            return true;
        }
        Vec3d look = RotationMath.INSTANCE.rotationToVector(Rotation.playerRotation()).normalize();
        double cosAngle = look.dotProduct(toTarget.multiply(1.0D / length));
        double halfFov = MathHelper.clamp(this.fovSize.currentValue() * 0.5F, 2.5F, 90.0F);
        return cosAngle >= Math.cos(Math.toRadians(halfFov));
    }

    /** Screen-space radius of the FOV circle: tan(aim half-angle) / tan(game half-angle) of the smaller screen half. */
    private float fovCircleRadius(MinecraftClient client, int width, int height) {
        float maxRadius = Math.max(0.0f, Math.min(width, height) * 0.5f - 2.0f);
        if (maxRadius <= 0.0f) {
            return 0.0f;
        }
        double aimHalf = MathHelper.clamp(this.fovSize.currentValue() * 0.5F, 2.5F, 90.0F);
        double gameFov = MathHelper.clamp((double) client.options.getFov().getValue(), 40.0D, 170.0D);
        double gameHalf = MathHelper.clamp(gameFov * 0.5D, 1.0D, 89.9D);
        if (aimHalf >= gameHalf) {
            return maxRadius;
        }
        double radius = Math.tan(Math.toRadians(aimHalf)) / Math.tan(Math.toRadians(gameHalf)) * (Math.min(width, height) * 0.5D);
        return (float) MathHelper.clamp(radius, 0.0D, maxRadius);
    }

    /** Called from InGameHudMixin before Render2DUtil flushes its queue. */
    public void renderFovCircle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isState() || !isFovMode() || !this.drawFovCircle.isValue()
                || client.player == null || client.world == null || client.currentScreen != null
                || client.options.hudHidden || client.options.getPerspective() != Perspective.FIRST_PERSON) {
            return;
        }
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float radius = fovCircleRadius(client, width, height);
        if (radius < 1.0f) {
            return;
        }
        int color = this.target != null ? ColorUtil.getColor(127, 242, 255, 165) : ColorUtil.getColor(255, 255, 255, 95);
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        int segments = 72;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2.0D * i / segments;
            double a2 = Math.PI * 2.0D * (i + 1) / segments;
            Render2DUtil.drawLine(cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius,
                    cx + (float) Math.cos(a2) * radius, cy + (float) Math.sin(a2) * radius, 1.2f, color);
        }
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
            case SHARD -> this.shardRotationMode;
            case FOV -> this.spookyTimeRotationMode;
            case SHARD_2 -> this.shard2RotationMode;
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
            double dCos = Math.cos(Math.toRadians(180.0d));
            float targetDistance = maceDiveTargeting
                    ? getMaceAimDistance(clientPlayerEntity, this.target)
                    : fCurrentValue;
            if (!this.hitPointResolver.hasValidPoint(this.target, targetDistance, this.ignoreWalls.isValue()) || dDotProduct <= dCos
                    || !isTargetWithinFov(this.target, clientPlayerEntity)) {
                return null;
            }
            return this.target;
        }
        EntityFilter class095Var = createTargetFilter();
            ArrayList arrayList = new ArrayList();
    for (Entity livingEntity : this.mc.getWorld().getEntities()) {
        if (livingEntity instanceof LivingEntity) {
            LivingEntity livingEntity2 = (LivingEntity) livingEntity;
            boolean validForCurrentAttack = maceDiveTargeting
                    ? isValidMaceDiveTarget(livingEntity2, clientPlayerEntity)
                    : isValidTarget(livingEntity2, clientPlayerEntity, fCurrentValue);
            if (class095Var.matches(livingEntity2)
                    && validForCurrentAttack
                    && vec3dNormalize.dotProduct(livingEntity2.getEntityPos().subtract(clientPlayerEntity.getEntityPos()).normalize()) > Math.cos(Math.toRadians(180.0d))
                    && isTargetWithinFov(livingEntity2, clientPlayerEntity)) {
                arrayList.add(livingEntity2);
            }
        }
    }
    List<LivingEntity> listSort = TargetSorters.of(this.sortModes.selectedValues()).sort(arrayList, clientPlayerEntity);
    this.target = listSort.isEmpty() ? null : (LivingEntity) listSort.getFirst();
    return this.target;
}

public boolean isValidTarget(LivingEntity livingEntity, ClientPlayerEntity clientPlayerEntity, float f) {
    if (livingEntity == null || livingEntity == clientPlayerEntity || !livingEntity.isAlive() || livingEntity.getHealth() <= 0.0f || AntiBotModule.isBot(livingEntity)) {
        return false;
    }
    if (this.ignoreNaked.isValue() && (livingEntity instanceof PlayerEntity) && EquipmentUtil.armor(livingEntity).stream().allMatch((v0) -> {
        return v0.isEmpty();
    })) {
        return false;
    }
    return this.hitPointResolver.hasValidPoint(livingEntity, f, this.ignoreWalls.isValue());
}

private boolean canStartMaceDiveTargeting(ClientPlayerEntity player) {
    if (!this.autoMace.isValue()
            || player == null
            || player.isGliding()
            || player.isTouchingWater()
            || player.isInLava()) {
        return false;
    }
    if (this.preparedMace == null
            && !this.maceSwapScheduled
            && !player.getMainHandStack().isOf(Items.MACE)
            && (System.currentTimeMillis() < this.maceSwapCooldownUntil
                    || findMace(player).isEmpty())) {
        return false;
    }

    EntityFilter filter = createTargetFilter();
    for (Entity entity : this.mc.getWorld().getEntities()) {
        if (entity instanceof LivingEntity livingEntity
                && filter.matches(livingEntity)
                && isValidMaceDiveTarget(livingEntity, player)) {
            return true;
        }
    }
    return false;
}

private boolean isValidMaceDiveTarget(
        LivingEntity livingEntity,
        ClientPlayerEntity player) {
    if (livingEntity == null) {
        return false;
    }
    boolean maceAlreadyPrepared = this.preparedMace != null
            || this.maceSwapScheduled
            || player.getMainHandStack().isOf(Items.MACE);
    double requiredHeight = maceAlreadyPrepared
            ? 0.0D
            : this.autoMaceHeight.currentValue();
    double heightAboveTarget = player.getY() - livingEntity.getY();
    if (heightAboveTarget < requiredHeight
            || livingEntity.getBoundingBox().maxY >= player.getEyeY()) {
        return false;
    }
    double maximumHorizontalDistance = this.maxDistance.currentValue()
            + this.extraDistance.currentValue()
            + 1.5D;
    Vec3d playerHorizontal = new Vec3d(player.getX(), 0.0D, player.getZ());
    Vec3d targetHorizontal = new Vec3d(
            livingEntity.getX(),
            0.0D,
            livingEntity.getZ());
    if (playerHorizontal.squaredDistanceTo(targetHorizontal)
            > maximumHorizontalDistance * maximumHorizontalDistance) {
        return false;
    }
    return isValidTarget(
            livingEntity,
            player,
            getMaceAimDistance(player, livingEntity));
}

private EntityFilter createTargetFilter() {
    EntityFilter filter = new EntityFilter();
    if (this.targetTypes.isSelected(AuraTargetType.PLAYERS)) {
        filter.add(EntityCategory.PLAYER);
    }
    if (this.targetTypes.isSelected(AuraTargetType.FRIENDS)) {
        filter.add(EntityCategory.FRIEND);
    }
    if (this.targetTypes.isSelected(AuraTargetType.MOBS)) {
        filter.add(EntityCategory.MOB);
    }
    if (this.targetTypes.isSelected(AuraTargetType.ANIMALS)) {
        filter.add(EntityCategory.ANIMAL);
    }
    return filter;
}

private boolean isMaceDiveAimActive(
        ClientPlayerEntity player,
        LivingEntity livingEntity) {
    return canStartMaceDiveTargeting(player)
            && isValidMaceDiveTarget(livingEntity, player);
}

private float getMaceAimDistance(
        ClientPlayerEntity player,
        LivingEntity livingEntity) {
    return (float) Math.max(
            this.maxDistance.currentValue() + this.extraDistance.currentValue(),
            player.getEyePos().distanceTo(livingEntity.getBoundingBox().getCenter())
                    + MACE_AIM_DISTANCE_MARGIN);
}

private Optional<Float> predictMaceImpactPitch(
        ClientPlayerEntity player,
        LivingEntity livingEntity) {
    if (!isMaceDiveAimActive(player, livingEntity)) {
        return Optional.empty();
    }

    double attackDistance = player.isCreative()
            ? Math.max(this.maxDistance.currentValue(), player.getEntityInteractionRange())
            : this.maxDistance.currentValue();
    SimulatedPlayer predictedPlayer = SimulatedPlayer.simulateLocalPlayer(0);
    SimulatedPlayer predictedTarget = livingEntity instanceof PlayerEntity targetPlayer
            ? SimulatedPlayer.fromOtherPlayer(
                    targetPlayer,
                    MovementInputState.guessInput(targetPlayer))
            : null;
    Vec3d targetVelocity = livingEntity.getVelocity();

    for (int tick = 0; tick <= MACE_PITCH_PREDICTION_TICKS; tick++) {
        if (tick > 0) {
            predictedPlayer.tick();
            if (predictedTarget != null) {
                predictedTarget.tick();
            }
        }
        if (predictedPlayer.onGround) {
            break;
        }

        Box predictedTargetBox = predictedTarget != null
                ? predictedTarget.boundingBox
                : livingEntity.getBoundingBox().offset(targetVelocity.multiply(tick));
        Vec3d predictedEyePosition = predictedPlayer.getEyePos();
        Vec3d closestPoint = this.hitPointResolver.getClosestVec(
                predictedEyePosition,
                predictedTargetBox);
        if (predictedEyePosition.distanceTo(closestPoint)
                > attackDistance + MACE_PITCH_REACH_TOLERANCE) {
            continue;
        }

        Vec3d predictedAimPoint = predictedTargetBox.getCenter();
        Rotation predictedRotation = RotationMath.INSTANCE.fromVec3d(
                predictedAimPoint.subtract(predictedEyePosition));
        if (Float.isFinite(predictedRotation.getPitch())) {
            return Optional.of(predictedRotation.getPitch());
        }
        break;
    }
    return Optional.empty();
}

public boolean canAttack(ClientPlayerEntity clientPlayerEntity, int i) {
    if (this.target == null || !this.target.isAlive() || (this.mc.getCurrentScreen() instanceof GenericContainerScreen)) {
        return false;
    }
    UseAction useAction = clientPlayerEntity.getActiveItem().getUseAction();
    boolean z = useAction == UseAction.EAT || useAction == UseAction.DRINK || useAction == UseAction.CROSSBOW || useAction == UseAction.SPEAR || useAction == UseAction.BOW;
    if (clientPlayerEntity.isUsingItem() && z) {
        if (clientPlayerEntity.getActiveHand() == Hand.MAIN_HAND) {
            return false;
        }
        if (clientPlayerEntity.getActiveHand() == Hand.OFF_HAND && this.noHitWhileEatingOffhand.isValue()) {
            return false;
        }
    }
    float fCurrentValue = this.maxDistance.currentValue();
    if (clientPlayerEntity.isCreative()) {
        fCurrentValue = (float) Math.max(fCurrentValue, clientPlayerEntity.getEntityInteractionRange());
    }
    if (this.hitPointResolver.hasValidPoint(this.target, fCurrentValue, this.ignoreWalls.isValue())) {
        return IntStream.rangeClosed(0, i).anyMatch(i2 -> {
            return canCrit(clientPlayerEntity, i2);
        });
    }
    return false;
}

public boolean canCrit(ClientPlayerEntity clientPlayerEntity, int i) {
    if (this.autoMace.isValue()
            && (this.maceSwapScheduled
                    || this.maceRestoreScheduled)) {
        return false;
    }
    boolean maceSmashReady = this.autoMace.isValue() && isMaceSmashReady(clientPlayerEntity, i);
    if (this.autoMace.isValue() && isMaceDivePrepared(clientPlayerEntity, i) && !maceSmashReady) {
        return false;
    }
    if (!maceSmashReady && !(clientPlayerEntity.getAttackCooldownProgress(((float) i) + 0.5f) > 0.9f && this.attackCooldown - i <= 0)) {
        return false;
    }
    if (maceSmashReady) {
        return true;
    }
    if (!this.onlyCriticalHit.isValue()) {
        return true;
    }
    SimulatedPlayer class136VarSimulateLocalPlayer = SimulatedPlayer.simulateLocalPlayer(i);
    boolean z = class136VarSimulateLocalPlayer.input.playerInput.jump() || ((AutoJumpModule) AuraRuntime.INSTANCE.moduleRepository().get(AutoJumpModule.class)).isState();
    if (this.critsWithSpace.isValue() && class136VarSimulateLocalPlayer.onGround && !z) {
        return true;
    }
    return shouldCrit(clientPlayerEntity, i);
}
    private boolean shouldResetSprintForAttack(
            ClientPlayerEntity clientPlayerEntity,
            int predictionTicks) {
        if (!this.keepSprintWhenPossible.isValue()) {
            return true;
        }
        if (this.autoMace.isValue()
                && isMaceSmashReady(clientPlayerEntity, predictionTicks)) {
            return false;
        }

        SimulatedPlayer simulatedPlayer =
                SimulatedPlayer.simulateLocalPlayer(predictionTicks);
        if (simulatedPlayer.onGround
                || simulatedPlayer.fallDistance <= 0.0f
                || simulatedPlayer.isFallFlying
                || simulatedPlayer.touchingWater
                || simulatedPlayer.isSubmergedInWater()
                || simulatedPlayer.isInLava()
                || simulatedPlayer.isClimbing()
                || clientPlayerEntity.hasVehicle()
                || clientPlayerEntity.getAbilities().flying
                || simulatedPlayer.hasStatusEffect(StatusEffects.LEVITATION)
                || simulatedPlayer.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulatedPlayer.hasStatusEffect(StatusEffects.SLOW_FALLING)
                || BlockUtil.checkBlockIntersection(
                        simulatedPlayer.boundingBox,
                        block -> block instanceof CobwebBlock)) {
            return false;
        }
        return true;
    }

    public boolean shouldCrit(ClientPlayerEntity clientPlayerEntity, int i) {
        if (isMaceSmashReady(clientPlayerEntity, i)) {
            return true;
        }
        SimulatedPlayer class136VarSimulateLocalPlayer = SimulatedPlayer.simulateLocalPlayer(i);
        if (class136VarSimulateLocalPlayer.hasStatusEffect(StatusEffects.LEVITATION) || class136VarSimulateLocalPlayer.hasStatusEffect(StatusEffects.BLINDNESS) || class136VarSimulateLocalPlayer.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            return true;
        }
        if (BlockUtil.checkBlockIntersection(class136VarSimulateLocalPlayer.boundingBox, block -> {
            return block instanceof CobwebBlock;
        }) || class136VarSimulateLocalPlayer.isSubmergedInWater() || class136VarSimulateLocalPlayer.isInLava() || class136VarSimulateLocalPlayer.isClimbing() || this.mc.getPlayer().getAbilities().flying) {
            return true;
        }
        if (clientPlayerEntity.getMainHandStack().getItem() instanceof MaceItem) {
            return MaceItem.shouldDealAdditionalDamage(clientPlayerEntity);
        }
        boolean z = SimulatedPlayer.simulateLocalPlayer(i + 1).onGround;
        boolean z2 = SimulatedPlayer.simulateLocalPlayer(i + 2).onGround;
        boolean z3 = class136VarSimulateLocalPlayer.fallDistance > 1.0f && z;
        return (class136VarSimulateLocalPlayer.onGround || class136VarSimulateLocalPlayer.fallDistance <= 0.0f || ((class136VarSimulateLocalPlayer.velocity.y > (-0.3d) ? 1 : (class136VarSimulateLocalPlayer.velocity.y == (-0.3d) ? 0 : -1)) < 0 && z2 && (verticalCollisionSnapshot(4) || verticalCollisionSnapshot(5) || verticalCollisionSnapshot(6) || verticalCollisionSnapshot(7))) || ((class136VarSimulateLocalPlayer.fallDistance > ((float) class136VarSimulateLocalPlayer.velocity.y) ? 1 : (class136VarSimulateLocalPlayer.fallDistance == ((float) class136VarSimulateLocalPlayer.velocity.y) ? 0 : -1)) == 0 && z) || z3) ? false : true;
    }

    private void updateMaceFallState(ClientPlayerEntity clientPlayerEntity) {
        if (clientPlayerEntity == null) {
            return;
        }
        if (clientPlayerEntity.isOnGround()) {
            this.maceAttackedThisFall = false;
        }
        if (clientPlayerEntity.isGliding()) {
            requestMaceRestore(clientPlayerEntity, 0);
            return;
        }
        if (this.preparedMace != null) {
            this.macePreparedTicks++;
            if (this.maceAttackedThisFall
                    || this.macePreparedTicks > MACE_HOLD_TIMEOUT_TICKS
                    || !clientPlayerEntity.getMainHandStack().isOf(Items.MACE)) {
                requestMaceRestore(clientPlayerEntity, MACE_RESTORE_DELAY_TICKS);
            }
        }
    }

    private void prepareMaceForDive(ClientPlayerEntity clientPlayerEntity) {
        if (!this.autoMace.isValue()) {
            requestMaceRestore(clientPlayerEntity, 0);
            return;
        }
        if (this.preparedMace != null
                || this.maceSwapScheduled
                || this.maceRestoreScheduled
                || this.maceAttackedThisFall
                || this.target == null
                || System.currentTimeMillis() < this.maceSwapCooldownUntil) {
            return;
        }
        if (clientPlayerEntity.getY() - this.target.getY()
                < this.autoMaceHeight.currentValue()) {
            return;
        }
        if (clientPlayerEntity.getMainHandStack().isOf(Items.MACE)) {
            return;
        }
        Optional<SlotSearchResult2> optionalMace = findMace(clientPlayerEntity);
        if (optionalMace.isEmpty() || !GrimDelayHandler.script.isFinished()) {
            return;
        }
        SlotSearchResult2 mace = optionalMace.get();
        ScopedSlot maceSlot = mace.slotReference();
        int selectedSlot = clientPlayerEntity.getInventory().getSelectedSlot();
        this.maceRestoreSelectedSlot = selectedSlot;
        if (maceSlot.scope() == InventoryScope.HOTBAR) {
            this.maceInventorySourceSlot = -1;
            this.maceInventoryTargetSlot = -1;
        } else {
            this.maceInventorySourceSlot = maceSlot.increasedSlot();
            this.maceInventoryTargetSlot = selectedSlot;
        }
        this.maceSwapScheduled = true;
        GrimDelayHandler.addTask(RotationManager.INSTANCE.getCurrentRotation(), () -> {
            if (!this.maceSwapScheduled) {
                return;
            }
            this.maceSwapScheduled = false;
            if (!isState()
                    || !this.autoMace.isValue()
                    || this.mc.getPlayer() != clientPlayerEntity
                    || this.target == null
                    || !this.target.isAlive()) {
                resetMaceSwapState();
                return;
            }

            if (maceSlot.scope() == InventoryScope.HOTBAR) {
                int slot = maceSlot.slot();
                this.maceInternalSlotChange = true;
                try {
                    clientPlayerEntity.getInventory().setSelectedSlot(slot);
                    clientPlayerEntity.networkHandler.sendPacket(
                            new UpdateSelectedSlotC2SPacket(slot));
                } finally {
                    this.maceInternalSlotChange = false;
                }
            } else {
                PlayerActionUtil.INSTANCE.windowClick(
                        SlotActionType.SWAP,
                        this.maceInventorySourceSlot,
                        this.maceInventoryTargetSlot,
                        true);
            }
            this.preparedMace = new SlotSearchResult2(
                    new ScopedSlot(selectedSlot, InventoryScope.HOTBAR),
                    clientPlayerEntity.getInventory().getStack(selectedSlot));
            this.macePreparedTicks = 0;
        });
    }

    private Optional<SlotSearchResult2> findMace(ClientPlayerEntity clientPlayerEntity) {
        int selectedSlot = clientPlayerEntity.getInventory().getSelectedSlot();
        if (clientPlayerEntity.getInventory().getStack(selectedSlot).isOf(Items.MACE)) {
            return Optional.of(new SlotSearchResult2(new ScopedSlot(selectedSlot, InventoryScope.HOTBAR), clientPlayerEntity.getInventory().getStack(selectedSlot)));
        }
        return AuraRuntime.INSTANCE.inventoryService().searcher().findItem(itemStack -> {
            return itemStack.isOf(Items.MACE);
        }, InventoryScope.HOTBAR, InventoryScope.INVENTORY);
    }

    private boolean isMaceDivePrepared(ClientPlayerEntity clientPlayerEntity, int predictionTicks) {
        return this.autoMace.isValue()
                && clientPlayerEntity != null
                && clientPlayerEntity.getMainHandStack().isOf(Items.MACE)
                && !this.maceAttackedThisFall
                && isMaceDivePhysics(clientPlayerEntity, predictionTicks, false);
    }

    private boolean isMaceSmashReady(ClientPlayerEntity clientPlayerEntity, int predictionTicks) {
        return isMaceDivePrepared(clientPlayerEntity, predictionTicks)
                && isMaceDivePhysics(clientPlayerEntity, predictionTicks, true);
    }

    private boolean isMaceDivePhysics(ClientPlayerEntity clientPlayerEntity, int predictionTicks, boolean requireSmashDistance) {
        if (!this.autoMace.isValue() || clientPlayerEntity == null || this.target == null || clientPlayerEntity.isGliding()) {
            return false;
        }
        if (!requireSmashDistance) {
            return this.target.getBoundingBox().maxY < clientPlayerEntity.getEyeY()
                    && (this.preparedMace != null
                            || clientPlayerEntity.getMainHandStack().isOf(Items.MACE)
                            || clientPlayerEntity.getY() - this.target.getY()
                                    >= this.autoMaceHeight.currentValue());
        }
        if (predictionTicks <= 0) {
            return !clientPlayerEntity.isOnGround()
                    && clientPlayerEntity.getVelocity().y < 0.0D
                    && MaceItem.shouldDealAdditionalDamage(clientPlayerEntity);
        }
        SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(predictionTicks);
        return !simulatedPlayer.onGround
                && simulatedPlayer.velocity.y < 0.0D
                && simulatedPlayer.fallDistance > MIN_MACE_FALL_DISTANCE;
    }

    private void performMaceAttack(ClientPlayerEntity clientPlayerEntity, ClientPlayerInteractionManager interactionManager, RotationManager rotationManager) {
        if (!this.autoMace.isValue()
                || this.maceAttackedThisFall
                || !clientPlayerEntity.getMainHandStack().isOf(Items.MACE)) {
            return;
        }
        LivingEntity attackTarget = this.target;
        if (!attackTarget(clientPlayerEntity, interactionManager, attackTarget)) {
            return;
        }
        this.maceAttackedThisFall = true;
        if (this.preparedMace != null) {
            requestMaceRestore(clientPlayerEntity, MACE_RESTORE_DELAY_TICKS);
        }
    }

    private boolean attackTarget(ClientPlayerEntity clientPlayerEntity, ClientPlayerInteractionManager interactionManager, LivingEntity attackTarget) {
        if (!attackEntityWithMaxDamage(
                clientPlayerEntity,
                interactionManager,
                attackTarget)) {
            return false;
        }
        clientPlayerEntity.swingHand(Hand.MAIN_HAND);
        this.hitCounter++;
        this.attackTimer.reset();
        
        if (ru.zov.implement.features.modules.render.TargetESP.getInstance() != null 
                && ru.zov.implement.features.modules.render.TargetESP.getInstance().isState()) {
            ru.zov.implement.features.modules.render.TargetESP.getInstance().onHit(attackTarget);
        }
        
        return true;
    }

    private boolean attackEntityWithMaxDamage(
            ClientPlayerEntity player,
            ClientPlayerInteractionManager interactionManager,
            LivingEntity attackTarget) {
        if (!shouldUseMaxDamage(player, 0)) {
            attackEntityVanillaSafe(player, interactionManager, attackTarget);
            return true;
        }
        if (isAutoTotemProtecting()) {
            restoreMaxDamageItem(player);
            attackEntityVanillaSafe(player, interactionManager, attackTarget);
            return true;
        }

        MaxDamageItem selectedItem = this.maxDamageItem.currentValue();
        if (matchesMaxDamageItem(player.getOffHandStack(), selectedItem)) {
            if (this.maxDamageSwapSourceSlot != -1
                    && !isMaxDamageItemReady(player, selectedItem)) {
                return false;
            }
            attackEntityVanillaSafe(player, interactionManager, attackTarget);
            if (this.maxDamageSwapSourceSlot != -1) {
                scheduleMaxDamageRestore(player);
            }
            return true;
        }

        if (!hasMaxDamageItem(player, selectedItem)) {
            attackEntityVanillaSafe(player, interactionManager, attackTarget);
            return true;
        }
        if (!isMaxDamageItemReady(player, selectedItem)) {
            prepareMaxDamageItem(player);
            return false;
        }
        try {
            attackEntityVanillaSafe(player, interactionManager, attackTarget);
        } finally {
            scheduleMaxDamageRestore(player);
        }
        return true;
    }

    private boolean shouldUseMaxDamage(
            ClientPlayerEntity player,
            int predictionTicks) {
        if (!this.maxDamage.isValue() || player == null) {
            return false;
        }
        if (this.autoMace.isValue()
                && isMaceSmashReady(player, predictionTicks)) {
            return true;
        }
        SimulatedPlayer simulatedPlayer =
                SimulatedPlayer.simulateLocalPlayer(predictionTicks);
        return simulatedPlayer.fallDistance > 0.0F
                && !simulatedPlayer.onGround
                && !simulatedPlayer.isFallFlying
                && !simulatedPlayer.touchingWater
                && !simulatedPlayer.isSubmergedInWater()
                && !simulatedPlayer.isInLava()
                && !simulatedPlayer.isClimbing()
                && !player.hasVehicle()
                && !player.getAbilities().flying
                && !simulatedPlayer.hasStatusEffect(StatusEffects.BLINDNESS)
                && !simulatedPlayer.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !simulatedPlayer.hasStatusEffect(StatusEffects.LEVITATION);
    }

    private boolean prepareMaxDamageItem(ClientPlayerEntity player) {
        if (this.maxDamageRestoreAtAge != Integer.MIN_VALUE) {
            return false;
        }
        if (player == null
                || !this.maxDamage.isValue()
                || isAutoTotemProtecting()) {
            restoreMaxDamageItem(player);
            return false;
        }

        MaxDamageItem selectedItem = this.maxDamageItem.currentValue();
        if (this.preparedMaxDamageItem != null
                && this.preparedMaxDamageItem != selectedItem) {
            restoreMaxDamageItem(player);
        }
        if (matchesMaxDamageItem(player.getOffHandStack(), selectedItem)) {
            if (this.preparedMaxDamageItem == null) {
                this.preparedMaxDamageItem = selectedItem;
                this.maxDamagePreparedAtAge = player.age - 1;
            }
            return true;
        }
        if (!SwapUtil.isIdle()) {
            return false;
        }
        if (this.maxDamageSwapScheduled) {
            return false;
        }

        Optional<SlotSearchResult2> damageItem =
                AuraRuntime.INSTANCE.inventoryService().searcher().findItem(
                        stack -> matchesMaxDamageItem(stack, selectedItem),
                        InventoryScope.HOTBAR,
                        InventoryScope.INVENTORY);
        if (damageItem.isEmpty()) {
            return false;
        }

        int sourceSlot = damageItem.get().slotReference().increasedSlot();
        this.maxDamageSwapScheduled = true;
        GrimDelayHandler.addTask(() -> {
            this.maxDamageSwapScheduled = false;
            if (!isState()
                    || !this.maxDamage.isValue()
                    || this.mc.getPlayer() != player
                    || this.target == null
                    || isAutoTotemProtecting()) {
                return;
            }
            PlayerActionUtil.INSTANCE.windowClick(
                    SlotActionType.SWAP,
                    sourceSlot,
                    40,
                    false);
            if (!matchesMaxDamageItem(player.getOffHandStack(), selectedItem)) {
                return;
            }
            this.maxDamageSwapSourceSlot = sourceSlot;
            this.maxDamagePreparedAtAge = player.age;
            this.preparedMaxDamageItem = selectedItem;
        });
        return false;
    }

    private boolean isMaxDamageItemReady(
            ClientPlayerEntity player,
            MaxDamageItem selectedItem) {
        return player != null
                && this.preparedMaxDamageItem == selectedItem
                && matchesMaxDamageItem(player.getOffHandStack(), selectedItem)
                && (this.maxDamageSwapSourceSlot == -1
                || player.age > this.maxDamagePreparedAtAge);
    }

    private boolean hasMaxDamageItem(
            ClientPlayerEntity player,
            MaxDamageItem selectedItem) {
        return matchesMaxDamageItem(player.getOffHandStack(), selectedItem)
                || AuraRuntime.INSTANCE.inventoryService().searcher().findItem(
                        stack -> matchesMaxDamageItem(stack, selectedItem),
                        InventoryScope.HOTBAR,
                        InventoryScope.INVENTORY).isPresent();
    }

    private void restoreMaxDamageItem(ClientPlayerEntity player) {
        if (this.maxDamageRestoreScheduled) {
            return;
        }
        if (this.maxDamageSwapSourceSlot != -1 && player != null) {
            if (!SwapUtil.isIdle()) {
                this.maxDamageRestoreAtAge = player.age;
                return;
            }
            int sourceSlot = this.maxDamageSwapSourceSlot;
            this.maxDamageRestoreScheduled = true;
            GrimDelayHandler.addTask(() -> {
                try {
                    if (this.mc.getPlayer() == player) {
                        PlayerActionUtil.INSTANCE.windowClick(
                                SlotActionType.SWAP,
                                sourceSlot,
                                40,
                                false);
                    }
                } finally {
                    clearMaxDamagePreparation();
                }
            });
            return;
        }
        clearMaxDamagePreparation();
    }

    private void clearMaxDamagePreparation() {
        this.maxDamageSwapSourceSlot = -1;
        this.maxDamagePreparedAtAge = Integer.MIN_VALUE;
        this.maxDamageRestoreAtAge = Integer.MIN_VALUE;
        this.maxDamageRestoreScheduled = false;
        this.preparedMaxDamageItem = null;
    }

    private void scheduleMaxDamageRestore(ClientPlayerEntity player) {
        if (this.maxDamageSwapSourceSlot == -1 || player == null) {
            return;
        }
        this.maxDamageRestoreAtAge = player.age + 1;
    }

    private boolean isAutoTotemProtecting() {
        AutoTotemModule autoTotem =
                AuraRuntime.INSTANCE.moduleRepository().get(AutoTotemModule.class);
        return autoTotem != null
                && autoTotem.isState()
                && autoTotem.trigger();
    }

    private boolean matchesMaxDamageItem(
            ItemStack stack,
            MaxDamageItem selectedItem) {
        if (stack == null || stack.isEmpty() || selectedItem == null) {
            return false;
        }
        if (selectedItem == MaxDamageItem.TOTEM) {
            return stack.isOf(Items.TOTEM_OF_UNDYING);
        }
        String itemName = StringHelper.stripTextFormat(
                stack.getName().getString()).toLowerCase().trim();
        return itemName.contains(selectedItem.serverName());
    }

    private void attackEntityVanillaSafe(
            ClientPlayerEntity clientPlayerEntity,
            ClientPlayerInteractionManager interactionManager,
            LivingEntity attackTarget) {
        prepareVisualSprintHold(clientPlayerEntity);
        interactionManager.attackEntity(clientPlayerEntity, attackTarget);
    }

        private void prepareVisualSprintHold(ClientPlayerEntity player) {
        if (!this.keepSprintWhenPossible.isValue()
                || !player.isSprinting()
                || shouldResetSprintForAttack(player, 0)) {
            return;
        }

        this.visualSprintPlayer = player;
        this.visualSprintUntilAge = player.age + 1;
        this.visualSprintEyeHeight = player.getStandingEyeHeight();
        this.visualSprintPose = player.getPose();
        this.visualSprintSwimming = player.isInSwimmingPose();
        this.visualSprintLeaningPitch = player.getLeaningPitch(1.0F);
    }

    public boolean shouldHoldSprintVisual(ClientPlayerEntity player) {
        return isState()
                && this.keepSprintWhenPossible.isValue()
                && player != null
                && player == this.visualSprintPlayer
                && player.age <= this.visualSprintUntilAge;
    }

    public float visualSprintEyeHeight() {
        return this.visualSprintEyeHeight;
    }

    public EntityPose visualSprintPose() {
        return this.visualSprintPose;
    }

    public boolean visualSprintSwimming() {
        return this.visualSprintSwimming;
    }

    public float visualSprintLeaningPitch() {
        return this.visualSprintLeaningPitch;
    }

    private void clearPreparedHotbarSlot(ClientPlayerEntity clientPlayerEntity, boolean restoreSelectedSlot) {
        AuraRuntime.INSTANCE.inventoryService().hotbarSlotSwapper().cancel(this);
        if (restoreSelectedSlot && clientPlayerEntity != null) {
            if (this.maceInventorySourceSlot != -1 && this.maceInventoryTargetSlot != -1) {
                PlayerActionUtil.INSTANCE.windowClick(
                    SlotActionType.SWAP,
                    this.maceInventorySourceSlot,
                    this.maceInventoryTargetSlot,
                    true
                );
            }
            if (this.maceRestoreSelectedSlot != -1) {
                this.maceInternalSlotChange = true;
                try {
                    clientPlayerEntity.getInventory().setSelectedSlot(this.maceRestoreSelectedSlot);
                    clientPlayerEntity.networkHandler.sendPacket(
                            new UpdateSelectedSlotC2SPacket(
                                    this.maceRestoreSelectedSlot));
                } finally {
                    this.maceInternalSlotChange = false;
                }
            }
        }
        this.maceRestoreSelectedSlot = -1;
        this.maceInventorySourceSlot = -1;
        this.maceInventoryTargetSlot = -1;
    }

    private void requestMaceRestore(
            ClientPlayerEntity clientPlayerEntity,
            int delayTicks) {
        if (this.preparedMace == null) {
            if (!this.maceSwapScheduled) {
                this.maceRestoreAtAge = Integer.MIN_VALUE;
            }
            return;
        }
        if (clientPlayerEntity == null) {
            clearMacePreparation(null, false);
            return;
        }
        int restoreAge = clientPlayerEntity.age + Math.max(0, delayTicks);
        if (this.maceRestoreAtAge == Integer.MIN_VALUE
                || restoreAge < this.maceRestoreAtAge) {
            this.maceRestoreAtAge = restoreAge;
        }
    }

    private void queueMaceRestore(ClientPlayerEntity clientPlayerEntity) {
        if (this.preparedMace == null) {
            this.maceRestoreAtAge = Integer.MIN_VALUE;
            return;
        }
        if (this.maceRestoreScheduled) {
            return;
        }
        if (!GrimDelayHandler.script.isFinished()) {
            this.maceRestoreAtAge = clientPlayerEntity.age + 1;
            return;
        }

        this.maceRestoreScheduled = true;
        this.maceRestoreAtAge = Integer.MIN_VALUE;
        GrimDelayHandler.addTask(RotationManager.INSTANCE.getCurrentRotation(), () -> {
            if (!this.maceRestoreScheduled) {
                return;
            }
            this.maceRestoreScheduled = false;
            clearPreparedHotbarSlot(clientPlayerEntity, true);
            this.preparedMace = null;
            this.macePreparedTicks = 0;
            this.maceSwapCooldownUntil =
                    System.currentTimeMillis() + MACE_SWAP_COOLDOWN_MS;
        });
    }

    private void resetMaceSwapState() {
        this.preparedMace = null;
        this.maceRestoreSelectedSlot = -1;
        this.maceInventorySourceSlot = -1;
        this.maceInventoryTargetSlot = -1;
        this.macePreparedTicks = 0;
        this.maceRestoreAtAge = Integer.MIN_VALUE;
    }

    private void clearMacePreparation(ClientPlayerEntity clientPlayerEntity, boolean restoreServerSlot) {
        this.maceSwapScheduled = false;
        this.maceRestoreScheduled = false;
        this.maceRestoreAtAge = Integer.MIN_VALUE;
        clearPreparedHotbarSlot(clientPlayerEntity, restoreServerSlot);
        resetMaceSwapState();
    }

    public boolean verticalCollisionSnapshot(int i) {
        LivingEntity player = Mc.INSTANCE.getPlayer();
        return ((Boolean) PlayerSnapshotManager.INSTANCE.getSnapshot(player, i).map(class373Var -> {
            return Boolean.valueOf(class373Var.verticalCollision);
        }).orElse(Boolean.valueOf(((ClientPlayerEntity) player).verticalCollision))).booleanValue();
    }

    int getAttackDelay() {
        int i = this.hitCounter;
        return switch ((AuraRotationMode) this.rotationMode.currentValue()) {
            case FUNTIME -> this.funtimeAttackDelays[i % this.funtimeAttackDelays.length];
            case SMOOTH -> this.smoothAttackDelays[i % this.smoothAttackDelays.length];
            case GRIM -> this.grimAttackDelays[i % this.grimAttackDelays.length];
            default -> 10;
        };
    }

    private boolean isSpookyTimeMode(AuraRotationMode mode) {
        return mode == AuraRotationMode.SPOOKYTIME;
    }

    private DirectionalInput createSpookyDistanceInput(ClientPlayerEntity player) {
        int targetId = this.target.getId();
        if (targetId != this.spookyDistanceTargetId) {
            this.spookyDistanceTargetId = targetId;
        }

        Box playerBox = player.getBoundingBox();
        Box targetBox = this.target.getBoundingBox();
        double hitboxGap = getHorizontalHitboxGap(playerBox, targetBox);
        boolean airborne = !player.isOnGround();
        double controlGap = hitboxGap;
        if (airborne) {
            Vec3d velocity = player.getVelocity();
            Box predictedBox = playerBox.offset(
                    velocity.x * SPOOKY_AIR_PREDICTION_TICKS,
                    0.0D,
                    velocity.z * SPOOKY_AIR_PREDICTION_TICKS);
            controlGap = Math.min(
                    hitboxGap,
                    getHorizontalHitboxGap(predictedBox, targetBox));
        }

        double holdGap = airborne
                ? SPOOKY_AIR_APPROACH_GAP
                : SPOOKY_DISTANCE_APPROACH_GAP;
        if (controlGap <= holdGap) {
            return DirectionalInput.NONE;
        }

        if (!airborne
                && hitboxGap < SPOOKY_FINE_STEP_GAP
                && player.age % 3 == 2) {
            return DirectionalInput.NONE;
        }

        if (this.moveCorrectionMode.isSelected(MoveCorrectionMode.FOCUS)) {
            return new DirectionalInput(true, false, false, false);
        }

        Vec3d targetDirection = this.target.getEntityPos().subtract(player.getEntityPos());
        return MovementInputHelper.getDirectionalInputForDegrees(
                DirectionalInput.NONE,
                MovementInputHelper.getDegreesRelativeToView(
                        targetDirection,
                        player.getYaw()));
    }

    private double getHorizontalHitboxGap(Box playerBox, Box targetBox) {
        double xGap = Math.max(
                0.0D,
                Math.max(targetBox.minX - playerBox.maxX, playerBox.minX - targetBox.maxX));
        double zGap = Math.max(
                0.0D,
                Math.max(targetBox.minZ - playerBox.maxZ, playerBox.minZ - targetBox.maxZ));
        return Math.hypot(xGap, zGap);
    }

    private void resetSpookyDistanceControl() {
        this.spookyDistanceTargetId = Integer.MIN_VALUE;
    }

    private void finishAuraRotationWithoutReturn(
            ClientPlayerEntity player,
            RotationManager rotationManager) {
        if (this.rotationReturn.isValue()
                || !this.auraRotationActive
                || player == null) {
            return;
        }
        Rotation rotation = rotationManager.getCurrentRotationOriginal();
        if (rotation == null) {
            return;
        }

        float yaw = rotation.getYaw();
        float pitch = rotation.getPitch();
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.lastYaw = yaw;
        player.lastPitch = pitch;
    }

    public Stopwatch attackTimer() {
        return this.attackTimer;
    }

    public Stopwatch lookingTimer() {
        return this.lookingTimer;
    }

    public int attackCooldown() {
        return this.attackCooldown;
    }

    public int sprintTicks() {
        return this.sprintTicks;
    }

    public Aura hitCounter(int i) {
        this.hitCounter = i;
        return this;
    }

    public int hitCounter() {
        return this.hitCounter;
    }

    public LivingEntity target() {
        return this.target;
    }

    public LivingEntity lastTarget() {
        return this.lastTarget;
    }

    public LivingEntity getTarget() {
        return target();
    }

    public LivingEntity getLastTarget() {
        return lastTarget();
    }
}
