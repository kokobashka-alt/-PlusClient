package ru.zov.implement.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RaytracingUtil;

public class TriggerBot extends Module {
   private LivingEntity target = null;
   private final MultiSelectSetting attackSetting = new MultiSelectSetting("Attack Setting", "Allows you to customize the attack")
      .value("Crits With Space", "Break Shield", "UnPress Shield", "No Attack When Eat")
      .selected("Break Shield", "UnPress Shield");

   public static TriggerBot getInstance() {
      return Instance.get(TriggerBot.class);
   }

   public TriggerBot() {
      super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
      this.setup(this.attackSetting);
   }

   @Override
   public void deactivate() {
      this.target = null;
      super.deactivate();
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (RaytracingUtil.raytraceEntity(3.0, AngleUtil.cameraAngle(), s -> !FriendUtils.isFriend(s.getName().getString())) instanceof EntityHitResult r
         && r.getEntity() instanceof LivingEntity entity) {
         this.target = entity;
         if (mc.player.getAttackCooldownProgress(0.5F) <= 0.9F) {
            return;
         }
         if (this.attackSetting.isSelected("No Attack When Eat") && mc.player.isUsingItem()) {
            return;
         }
         if (this.attackSetting.isSelected("Crits With Space")
            && mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            return;
         }
         if (this.attackSetting.isSelected("UnPress Shield")
            && mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
         }
         mc.interactionManager.attackEntity(mc.player, entity);
         mc.player.swingHand(Hand.MAIN_HAND);
      } else {
         this.target = null;
      }
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public MultiSelectSetting getAttackSetting() {
      return this.attackSetting;
   }
}
