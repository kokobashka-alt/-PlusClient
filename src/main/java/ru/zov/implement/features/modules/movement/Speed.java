package ru.zov.implement.features.modules.movement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.EntityStory;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.entity.SimulatedPlayer;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.events.player.RotationUpdateEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;

public class Speed extends Module {
   private Entity target;
   private final SelectSetting mode = (new SelectSetting("Mode", "Selects the type of mode")).value("Grim Collision");
   private final ValueSetting speed = (new ValueSetting("Speed", "Select value speed")).setValue(0.8F).range(0.0F, 0.8F).visible(() -> this.mode.isSelected("Grim Collision"));
   private final BooleanSetting focus = (new BooleanSetting("Focus Target", "Aims at the target")).setValue(true).visible(() -> this.mode.isSelected("Grim Collision"));
   private final BooleanSetting autoMove = (new BooleanSetting("Auto Move", "Run after the target")).setValue(true).visible(() -> this.focus.isValue() && this.mode.isSelected("Grim Collision"));

   public Speed() {
      super("Speed", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.speed, this.mode, this.focus, this.autoMove});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.mode.isSelected("Grim Collision") && this.target != null) {
         Render3DUtil.drawBox(this.target.getBoundingBox().offset(MathUtil.interpolate(this.target).subtract(this.target.getEntityPos())), ColorUtil.LIGHT_RED, 2.0F, true, true, true);
      }

   }

   @EventHandler
   @Native
   public void onInput(InputEvent e) {
      if (this.mode.isSelected("Grim Collision") && mc.player != null && this.autoMove.isValue() && this.target != null) {
         e.setInput(new PlayerInput(true, false, false, false, this.target.getEyeY() > mc.player.getEyeY() - (double)(mc.player.isSwimming() ? 0 : 1), false, true));
      }

   }

   @EventHandler
   @Native
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (this.mode.isSelected("Grim Collision")) {
         RotationController controller = RotationController.INSTANCE;
         switch (e.type()) {
            case 0:
               this.target = null;
               if (this.focus.isValue()) {
                  this.getEntities((double)1.5F).filter((ent) -> ent.isPlayer() && !FriendUtils.isFriend((Entity)ent)).min(Comparator.comparingInt((living) -> living.equals(Aura.getInstance().getLastTarget()) ? 0 : 1)).ifPresent((entity) -> {
                     this.target = entity;
                     controller.rotateTo(AngleUtil.calculateAngle(this.target.getBoundingBox().getCenter().addRandom(Random.create(), 0.1F)), new RotationConfig(true, false), TaskPriority.HIGH_IMPORTANCE_2, this);
                  });
               }
               break;
            case 2:
               this.getEntities(0.2).findFirst().ifPresent((entity) -> {
                  boolean horizontal = PlayerIntersectionUtil.isBox(SimulatedPlayer.simulateLocalPlayer(1).boundingBox.expand(0.3, -0.001, 0.3), (pos) -> !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty());
                  double[] speed = MovingUtil.calculateDirection((double)(this.speed.getValue() / 10.0F));
                  if (!horizontal) {
                     mc.player.addVelocity(speed[0], (double)0.0F, speed[1]);
                  }

               });
         }
      }

   }

   private Stream<LivingEntity> getEntities(double size) {
      EntityStory story = EntityStory.INSTANCE;
      List<Box> boxes = new ArrayList();
      boxes.add(SimulatedPlayer.simulateLocalPlayer(1).boundingBox.expand(size));
      story.getEntities(mc.player, 2).forEach((living) -> boxes.add(living.getBoundingBox().expand(size)));
      Stream<LivingEntity> var10000 = PlayerIntersectionUtil.streamEntities().filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast);
      return var10000.filter((ent) -> !(ent instanceof ArmorStandEntity) && ent.isPushable() && ent != mc.player && ent.getEyePos().distanceTo(mc.player.getEyePos()) <= (double)8.0F && story.getEntities(ent, 3).anyMatch((living) -> boxes.stream().anyMatch((box) -> box.intersects(living.getBoundingBox()))));
   }
}
