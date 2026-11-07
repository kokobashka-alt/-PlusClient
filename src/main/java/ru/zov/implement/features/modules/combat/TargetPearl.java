package ru.zov.implement.features.modules.combat;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.entity.SimulatedPlayer;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.implement.events.player.EntityUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.implement.features.modules.render.ProjectilePrediction;

public class TargetPearl extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final SelectSetting modeSetting = (new SelectSetting("Mode", "When will target pearl work")).value("Bind", "Always").selected("Always");
   private final SelectSetting targetSetting = (new SelectSetting("Targets", "Targets for which pearls will be thrown")).value("Aura Target", "All").selected("Aura Target");
   private final BindSetting throwSetting = (new BindSetting("Throw", "Throw Key")).visible(() -> this.modeSetting.isSelected("Bind"));
   private final ValueSetting distanceSetting = (new ValueSetting("Distance", "Target Pearl Trigger Distance")).setValue(6.0F).range(3, 15);

   public TargetPearl() {
      super("TargetPearl", "Target Pearl", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.modeSetting, this.targetSetting, this.throwSetting, this.distanceSetting});
   }

   @EventHandler
   public void onEntityUpdate(EntityUpdateEvent e) {
      if (e.type().equals(EntityUpdateEvent.Type.ADD)) {
         Entity var3 = e.entity();
         if (var3 instanceof EnderPearlEntity) {
            EnderPearlEntity pearl = (EnderPearlEntity)var3;
            if (mc.world != null) {
               Optional<Entity> var10000 = mc.world.getPlayers().stream().filter((p) -> p.getEyePos().distanceTo(pearl.getEntityPos()) <= (double)6.0F).min(Comparator.comparingDouble((p) -> (double)p.distanceTo(pearl))).map(Entity.class::cast);
               Objects.requireNonNull(pearl);
               var10000.ifPresent(pearl::setOwner);
            }
         }
      }

   }

   @EventHandler(0)
   @Native
   public void onTick(TickEvent e) {
      int protocol = PlayerIntersectionUtil.getProtocolVersion();
      if (protocol >= 755 && protocol <= 766) {
         LivingEntity target = Aura.getInstance().getLastTarget();
         Slot slot = PlayerInventoryUtil.getSlot(Items.ENDER_PEARL);
         if (slot != null && !ItemUtil.hasCooldown(slot.getStack()) && this.stopWatch.finished((double)1000.0F)) {
            if (!this.modeSetting.isSelected("Bind") || PlayerIntersectionUtil.isKey(this.throwSetting)) {
               Stream var10000 = PlayerIntersectionUtil.streamEntities();
               Objects.requireNonNull(EnderPearlEntity.class);
               var10000 = var10000.filter(EnderPearlEntity.class::isInstance);
               Objects.requireNonNull(EnderPearlEntity.class);
               List<EnderPearlEntity> pearls = var10000.map(EnderPearlEntity.class::cast).toList();
               if (pearls.stream().anyMatch((pearl) -> Objects.equals(pearl.getOwner(), mc.player))) {
                  this.stopWatch.reset();
               } else {
                  Vec3d eye = SimulatedPlayer.simulateLocalPlayer(PlayerInventoryComponent.getTaskTime()).getEyePos();
                  EnderPearlEntity enderPearl = new EnderPearlEntity(mc.world, mc.player, slot.getStack());
                  ProjectilePrediction prediction = ProjectilePrediction.getInstance();
                  pearls.stream().filter((pearl) -> pearl.getOwner() != null && !FriendUtils.isFriend(pearl.getOwner()) && (this.targetSetting.isSelected("All") || target != null && target.equals(pearl.getOwner()))).min(Comparator.comparingDouble((pearl) -> RotationController.computeRotationDifference(AngleUtil.cameraAngle(), AngleUtil.calculateAngle(prediction.calcTrajectory(pearl).getPos())))).ifPresent((pearl) -> {
                     HitResult targetResult = prediction.calcTrajectory(pearl);
                     if (!(mc.player.getEntityPos().distanceTo(targetResult.getPos()) <= (double)this.distanceSetting.getInt())) {
                        Angle resultAngle = AngleUtil.fromVec3d(targetResult.getPos().subtract(eye));
                        IntStream.rangeClosed(-89, 89).mapToObj((pitch) -> new Angle(resultAngle.getYaw(), (float)pitch)).filter((angle) -> prediction.checkTrajectory(eye, angle.toVector(), enderPearl, (double)1.5F, false).getPos().distanceTo(targetResult.getPos()) <= (double)1.0F).max(Comparator.comparingDouble(Angle::getPitch)).ifPresent((angle) -> {
                           PlayerInventoryUtil.swapAndUse(slot, angle, true);
                           pearl.setOwner((Entity)null);
                           this.stopWatch.reset();
                        });
                     }
                  });
               }
            }
         }
      } else {
         Notifications.getInstance().addList("[Target Pearl] нужна версия " + String.valueOf(Formatting.RED) + "1.17-1.20.6", 2000L);
         this.animation.setDirection(false);
         this.toggleSilent(false);
      }
   }
}
