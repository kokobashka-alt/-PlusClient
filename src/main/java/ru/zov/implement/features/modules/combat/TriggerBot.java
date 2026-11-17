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

    private final MultiSelectSetting targets = new MultiSelectSetting("Targets", "Какие цели бить")
       .value("Mobs", "Invisible", "Naked", "Friends")
       .selected("Mobs", "Invisible", "Naked");
    private final BooleanSetting attackInWater = new BooleanSetting("Attack In Water", "Бить в воде").setValue(true);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", "Только с оружием в руке").setValue(false);
    private final BooleanSetting onlyCrits = new BooleanSetting("Only Crits", "Бить только критами").setValue(false);
    private final ValueSetting comboCharge = new ValueSetting("Combo charge", "Заряд кулдауна оружия, %")
       .setValue(50.0F)
       .range(1, 100);
    private final BooleanSetting tpsSync = new BooleanSetting("TPS Sync", "Подстраивать задержку под TPS сервера").setValue(true);
    private final ValueSetting hitChance = new ValueSetting("Hit Chance", "Шанс совершить удар, %")
       .setValue(100.0F)
       .range(0, 100);

    private final Random random = new Random();
    private LivingEntity target;

    public TriggerBot() {
       super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
       this.setup(
          this.targets,
          this.attackInWater,
          this.onlyWeapon,
          this.onlyCrits,
          this.comboCharge,
          this.tpsSync,
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
       this.target = null;
       super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
       if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.isPaused() || mc.interactionManager == null) {
          this.target = null;
          return;
       }

       if (mc.player.isUsingItem() || mc.player.hasVehicle() || mc.player.isSleeping()
          || mc.player.isTouchingWater() && !this.attackInWater.isValue()) {
          this.target = null;
          return;
       }

       Entity entity = this.findCrosshairTarget();
       this.target = entity instanceof LivingEntity living ? living : null;

       if (this.target == null || !mc.player.canSee(this.target)) {
          return;
       }

       // Проверка на криты, если включены
       boolean critical = this.canCriticalHit();
       if (this.onlyCrits.isValue() && !critical) {
          return;
       }

       // Проверка кулдауна атаки
       float cooldown = mc.player.getAttackCooldownProgress(0.5F);
       float required = this.comboCharge.getValue() / 100.0F;
       if (cooldown < required) {
          return;
       }

       // Шанс попадания
       if (this.hitChance.getValue() < 100.0F && Math.random() * 100.0 >= this.hitChance.getValue()) {
          return;
       }

       this.performAttack(this.target);
    }

    private Entity findCrosshairTarget() {
       HitResult hit = mc.crosshairTarget;
       if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
          if (this.isValidTarget(living)) {
             return living;
          }
       }
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

       return true;
    }

    private void performAttack(Entity entity) {
       mc.interactionManager.attackEntity(mc.player, entity);
       mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean canCriticalHit() {
       return !mc.player.isOnGround()
          && mc.player.getY() < mc.player.lastY
          && mc.player.getVelocity().y < 0.0
          && mc.player.fallDistance > 0.0;
    }

    private boolean isCombatWeapon(ItemStack stack) {
       return stack.isIn(ItemTags.SWORDS)
          || stack.isIn(ItemTags.AXES)
          || stack.isOf(Items.TRIDENT)
          || stack.isOf(Items.MACE);
    }
}
