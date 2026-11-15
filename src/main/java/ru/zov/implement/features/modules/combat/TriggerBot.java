package ru.zov.implement.features.modules.combat;

import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.player.AttackEvent;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.TickEvent;

public class TriggerBot extends Module {
    private static final long CROSSHAIR_GRACE_MS = 80L;

    private static final class Timer {
       private long start = System.currentTimeMillis();

       void reset() {
          this.start = System.currentTimeMillis();
       }

       boolean hasElapsed(long ms) {
          return System.currentTimeMillis() - this.start >= ms;
       }
    }

    private final MultiSelectSetting targets = new MultiSelectSetting("Targets", "Какие цели бить")
       .value("Mobs", "Invisible", "Naked", "Friends")
       .selected("Invisible");
    private final BooleanSetting attackInWater = new BooleanSetting("Attack In Water", "Бить в воде").setValue(true);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", "Только с оружием в руке").setValue(true);
    private final BooleanSetting onlyCrits = new BooleanSetting("Only Crits", "Бить только критами").setValue(false);
    private final BooleanSetting mace = new BooleanSetting("Mace", "Считать булаву оружием").setValue(false);
    private final ValueSetting maceCharge = new ValueSetting("Mace charge", "Заряд кулдауна для булавы, %")
       .setValue(15.0F)
       .range(1, 100)
       .visible(this.mace::isValue);
    private final ValueSetting comboCharge = new ValueSetting("Combo charge", "Заряд кулдауна для остального оружия, %")
       .setValue(80.0F)
       .range(1, 100);
    private final BooleanSetting sprintReset = new BooleanSetting("Sprint Reset", "Сбрасывать спринт перед критом").setValue(true);
    private final ValueSetting sprintPauseMin = new ValueSetting("Sprint Pause min", "Минимальная пауза спринта, мс")
       .setValue(50.0F)
       .range(0, 250)
       .visible(this.sprintReset::isValue);
    private final ValueSetting sprintPauseMax = new ValueSetting("Sprint Pause max", "Максимальная пауза спринта, мс")
       .setValue(100.0F)
       .range(0, 250)
       .visible(this.sprintReset::isValue);
    private final BooleanSetting wTap = new BooleanSetting("WTap", "Отпускать вперёд на 3 тика после удара").setValue(false);
    private final BooleanSetting tpsSync = new BooleanSetting("TPS Sync", "Подстраивать задержку под TPS сервера").setValue(true);
    private final ValueSetting hitChance = new ValueSetting("Hit Chance", "Шанс совершить удар, %")
       .setValue(100.0F)
       .range(0, 100);
    private final ValueSetting swapDelayMin = new ValueSetting("Swap Delay min", "Минимальная задержка после смены слота, мс")
       .setValue(40.0F)
       .range(0, 150);
    private final ValueSetting swapDelayMax = new ValueSetting("Swap Delay max", "Максимальная задержка после смены слота, мс")
       .setValue(70.0F)
       .range(0, 150);

    private final Random random = new Random();
    private final Timer attackTimer = new Timer();
    private final Timer swapTimer = new Timer();
    private final Timer sprintResetTimer = new Timer();

    private LivingEntity target;
    private Entity pendingTarget;
    private long crosshairSeenAt;
    private int lastSlot = -1;
    private int activeSwapDelay;
    private int activeSprintPause;
    private int pendingAttackDelay = -1;
    private float wTapPauseTicks;
    private boolean resettingSprint;
    private boolean sprintReleased;
    private boolean wasSprintingBeforeAttack;

    public TriggerBot() {
       super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
       this.setup(
          this.targets,
          this.attackInWater,
          this.onlyWeapon,
          this.mace,
          this.maceCharge,
          this.comboCharge,
          this.sprintReset,
          this.sprintPauseMin,
          this.sprintPauseMax,
          this.onlyCrits,
          this.wTap,
          this.tpsSync,
          this.swapDelayMin,
          this.swapDelayMax,
          this.hitChance
       );
    }

    public static TriggerBot getInstance() {
       return (TriggerBot) Instance.get(TriggerBot.class);
    }

    public LivingEntity getTarget() {
       return this.target;
    }

    @Override
    public void deactivate() {
       this.endSprintReset();
       this.wTapPauseTicks = 0.0F;
       this.pendingTarget = null;
       this.target = null;
       this.lastSlot = -1;
       super.deactivate();
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
       this.resetAttackTimers();
    }

    @EventHandler
    public void onInput(InputEvent e) {
       if (this.resettingSprint) {
          e.setForward(false);
          e.setSprint(false);
          if (mc.player != null) {
             mc.player.setSprinting(false);
          }
       }

       if (this.wTapPauseTicks > 0.0F) {
          e.setForward(false);
          this.wTapPauseTicks--;
       }
    }

    @EventHandler
    public void onTick(TickEvent e) {
       if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.isPaused() || mc.interactionManager == null) {
          this.endSprintReset();
          this.target = null;
          return;
       }

       this.trackSlotChange();

       if (mc.player.isUsingItem() || mc.player.hasVehicle() || mc.player.isSleeping()
          || mc.player.isTouchingWater() && !this.attackInWater.isValue()) {
          this.endSprintReset();
          this.target = null;
          return;
       }

       Entity entity = this.findCrosshairTarget();
       this.target = entity instanceof LivingEntity living ? living : null;
       boolean visible = entity != null && mc.player.canSee(entity);
       boolean sprintResetWindow = visible
          && this.sprintReset.isValue()
          && this.isReadyForCrit()
          && !mc.player.isOnGround()
          && mc.player.getVelocity().y <= 0.0;

       if (this.sprintReleased && !sprintResetWindow) {
          this.endSprintReset();
       }

       this.tickSprintReset();
       if (!visible) {
          return;
       }

       if (sprintResetWindow && mc.player.isSprinting()) {
          if (!this.resettingSprint) {
             this.beginSprintReset();
          }

          return;
       }

       boolean critical = this.canCriticalHit();
       if (this.onlyCrits.isValue() && !critical) {
          return;
       }

       boolean waitingForCrit = !critical && this.isReadyForCrit() && !(mc.player.isOnGround() && !mc.options.jumpKey.isPressed());
       if (waitingForCrit || !this.canAttackNow(critical)) {
          return;
       }

       if (this.hitChance.getValue() < 100.0F && Math.random() * 100.0 >= this.hitChance.getValue()) {
          this.resetAttackTimers();
          this.wasSprintingBeforeAttack = true;
       } else {
          this.performAttack(entity);
       }
    }

    private void trackSlotChange() {
       int slot = mc.player.getInventory().getSelectedSlot();
       if (slot == this.lastSlot) {
          return;
       }

       this.lastSlot = slot;
       if (this.isCombatWeapon(mc.player.getMainHandStack())) {
          this.activeSwapDelay = this.nextRandomInRange(this.swapDelayMin.getValue(), this.swapDelayMax.getValue());
          this.swapTimer.reset();
       }
    }

    private Entity findCrosshairTarget() {
       Entity entity = null;
       HitResult hit = mc.crosshairTarget;
       if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity) {
          entity = entityHit.getEntity();
       }

       boolean inGrace = this.isValidTarget(this.pendingTarget) && System.currentTimeMillis() - this.crosshairSeenAt < CROSSHAIR_GRACE_MS;
       if (this.isValidTarget(entity)) {
          this.pendingTarget = entity;
          this.crosshairSeenAt = System.currentTimeMillis();
          return entity;
       }

       if (inGrace) {
          return this.pendingTarget;
       }

       this.pendingTarget = null;
       return null;
    }

    private boolean isValidTarget(Entity candidate) {
       if (candidate == null || mc.player == null || !candidate.isAlive() || candidate == mc.player) {
          return false;
       }

       if (candidate.isInvisible() && !this.targets.isSelected("Invisible")) {
          return false;
       }

       if (this.onlyWeapon.isValue() && !this.isCombatWeapon(mc.player.getMainHandStack())) {
          return false;
       }

       if (!this.targets.isSelected("Mobs") && !(candidate instanceof PlayerEntity)) {
          return false;
       }

       if (!(candidate instanceof PlayerEntity player)) {
          return true;
       }

       if (!this.targets.isSelected("Friends") && FriendUtils.isFriend(player)) {
          return false;
       }

       AntiBot antiBot = AntiBot.getInstance();
       if (antiBot != null && antiBot.isBot(player)) {
          return false;
       }

       return this.hasAnyArmor(player) || this.targets.isSelected("Naked");
    }

    private void performAttack(Entity entity) {
       if (this.wTap.isValue()) {
          this.wTapPauseTicks += 3.0F;
       }

       mc.interactionManager.attackEntity(mc.player, entity);
       mc.player.swingHand(Hand.MAIN_HAND);
       this.resetAttackTimers();
    }

    private boolean canCriticalHit() {
       return this.isReadyForCrit()
          && !mc.player.isOnGround()
          && mc.player.getY() < mc.player.lastY
          && mc.player.getVelocity().y < 0.0
          && mc.player.fallDistance > 0.0;
    }

    private boolean isReadyForCrit() {
       return !mc.player.isClimbing()
          && !mc.player.isTouchingWater()
          && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
          && !mc.player.hasVehicle()
          && !mc.player.isGliding()
          && !mc.player.getAbilities().flying
          && (!mc.player.isSprinting() || this.sprintReset.isValue());
    }

    private boolean canAttackNow(boolean critical) {
       if (this.wTapPauseTicks > 0.0F) {
          return false;
       }

       if (this.activeSwapDelay > 0 && !this.swapTimer.hasElapsed(this.activeSwapDelay)) {
          return false;
       }

       float cooldown = mc.player.getAttackCooldownProgress(0.5F);
       boolean maceInHand = this.mace.isValue() && mc.player.getMainHandStack().isOf(Items.MACE);
       float required = (maceInHand ? this.maceCharge.getValue() : this.comboCharge.getValue()) / 100.0F;
       if (critical && !maceInHand) {
          if (cooldown <= 0.9F) {
             return false;
          }

          required = 0.9F;
       } else if (cooldown < required) {
          return false;
       }

       if (critical && !this.wasSprintingBeforeAttack) {
          return true;
       }

       float tps = this.tpsSync.isValue() ? Math.max(5.0F, Math.min(20.0F, ServerUtil.TPS)) : 20.0F;
       float waitTicks = Math.max(0.0F, mc.player.getAttackCooldownProgressPerTick() * required - 0.5F);
       if (this.pendingAttackDelay < 0) {
          this.pendingAttackDelay = this.random.nextInt(61);
       }

       return this.attackTimer.hasElapsed((long) Math.ceil(waitTicks * 1000.0F / tps) + this.pendingAttackDelay);
    }

    private void resetAttackTimers() {
       this.attackTimer.reset();
       this.pendingAttackDelay = -1;
       this.wasSprintingBeforeAttack = false;
       this.sprintReleased = false;
    }

    private void beginSprintReset() {
       this.activeSprintPause = this.nextRandomInRange(this.sprintPauseMin.getValue(), this.sprintPauseMax.getValue());
       this.sprintResetTimer.reset();
       this.resettingSprint = true;
       this.sprintReleased = true;
    }

    private void tickSprintReset() {
       if (!this.resettingSprint) {
          return;
       }

       boolean pauseOver = this.activeSprintPause == 0 || this.sprintResetTimer.hasElapsed(this.activeSprintPause);
       if (mc.player == null || !this.sprintReset.isValue() || !this.sprintReleased && pauseOver) {
          this.endSprintReset();
       }
    }

    private void endSprintReset() {
       this.resettingSprint = false;
       this.sprintReleased = false;
    }

    private int nextRandomInRange(float a, float b) {
       int low = Math.round(Math.min(a, b));
       int high = Math.round(Math.max(a, b));
       return this.random.nextInt(low, high + 1);
    }

    private boolean isCombatWeapon(ItemStack stack) {
       return stack.isIn(ItemTags.SWORDS)
          || stack.isIn(ItemTags.AXES)
          || stack.isOf(Items.TRIDENT)
          || this.mace.isValue() && stack.isOf(Items.MACE);
    }

    private boolean hasAnyArmor(PlayerEntity player) {
       EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
       for (EquipmentSlot slot : slots) {
          if (!player.getEquippedStack(slot).isEmpty()) {
             return true;
          }
       }

       return false;
    }
}
