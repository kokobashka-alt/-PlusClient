package ru.zov.implement.features.modules.misc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.keyboard.HotBarScrollEvent;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.player.HotBarUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.features.modules.render.ProjectilePrediction;

public class ClickAction extends Module {
   private final BindSetting expBind = new BindSetting("EXP Bottle", "Throw Experience Bottle");
   private final BindSetting friendBind = new BindSetting("Friend Add", "add/remove Friend");
   private final List<ClickAction.KeyBind> keyBindings = new ArrayList<>();
   private final StopWatch stopWatch = new StopWatch();
   private final StopWatch itemWatch = new StopWatch();
   private final Script script = new Script();

   public ClickAction() {
      super("ClickAction", "Click Action", ModuleCategory.MISC);
      this.keyBindings.add(this.createBind(Items.ENDER_PEARL, new BindSetting("Ender Pearl", "Throw Ender Pearl")));
      this.keyBindings.add(this.createBind(Items.WIND_CHARGE, new BindSetting("Wind Charge", "Throw Wind Charge")));
      this.keyBindings.add(this.createBind(Items.SPLASH_POTION, new BindSetting("Any Buff", "Throw Any Buff")));
      this.keyBindings.add(this.createBind(Items.SPLASH_POTION, new BindSetting("Any Debuff", "Throw Any Debuff")));
      this.keyBindings.forEach(bind -> this.setup(bind.setting));
      this.setup(this.expBind, this.friendBind);
   }

   @EventHandler
   public void onHotBarUpdate(HotBarUpdateEvent e) {
      if (!this.script.isFinished()) {
         e.cancel();
      }
   }

   @EventHandler
   public void onHotBarScroll(HotBarScrollEvent e) {
      if (!this.script.isFinished()) {
         e.cancel();
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (e.isKeyDown(this.friendBind) && mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof PlayerEntity player) {
         if (FriendUtils.isFriend(player)) {
            FriendUtils.removeFriend(player);
         } else {
            FriendUtils.addFriend(player);
         }
      }

      this.keyBindings.stream().filter(bind -> e.isKeyReleased(bind.setting)).forEach(this::swapAndUse);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         this.keyBindings.forEach(bind -> {
            Animation animation = bind.anim;
            MathUtil.multAlpha(animation.getOutput().floatValue(), () -> {
               Item item = bind.item;
               animation.setDirection(PlayerIntersectionUtil.isKey(bind.setting) && PlayerInventoryUtil.getSlot(item) != null && !ItemUtil.hasCooldown(item));
               if (!animation.isFinished(Direction.BACKWARDS)) {
                  ProjectilePrediction.getInstance().drawPredictionItem(e.stack(), item.getDefaultStack(), AngleUtil.cameraAngle());
               }
            });
         });
      }
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (PlayerIntersectionUtil.isKey(this.expBind)) {
         Slot slot = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE);
         if (slot == null) {
            return;
         }

         RotationController.INSTANCE.rotateTo(AngleUtil.pitch(75.0F), new RotationConfig(true, false), TaskPriority.HIGH_IMPORTANCE_2, this);
         if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            this.itemWatch.reset();
            if (this.stopWatch.every(250.0)) {
               PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true);
               if (this.script.isFinished()) {
                  this.script.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true));
               }
            }
         } else if (this.itemWatch.finished(150.0)) {
            PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, AngleUtil.pitch(75.0F), true);
            this.stopWatch.reset();
         }
      } else if (!this.script.isFinished() && this.stopWatch.every(250.0)) {
         this.script.update();
      }
   }

   @Native
   public void swapAndUse(ClickAction.KeyBind bind) {
      switch (bind.setting.getName()) {
         case "Any Buff":
            PlayerInventoryUtil.swapAndUse(PlayerInventoryUtil.getPotionFromCategory(StatusEffectCategory.BENEFICIAL), "Бафф", true);
            break;
         case "Any Debuff":
            PlayerInventoryUtil.swapAndUse(PlayerInventoryUtil.getPotionFromCategory(StatusEffectCategory.HARMFUL), "Дебафф", true);
            break;
         default:
            PlayerInventoryUtil.swapAndUse(bind.item);
      }
   }

   public ClickAction.KeyBind createBind(Item item, BindSetting setting) {
      return new ClickAction.KeyBind(item, setting, new DecelerateAnimation().setMs(200).setValue(1.0).setDirection(Direction.BACKWARDS));
   }

   public record KeyBind(Item item, BindSetting setting, Animation anim) {
   }
}
