package ru.zov.implement.features.modules.combat.killaura.type;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

/**
 * Neuro rotation: a procedural human-aim model (see {@link NeuroAimModel}), not a trained network.
 * RotationManager calls process() once per client tick; entity == null is the return-to-camera phase,
 * where {@code to} is the player's real rotation. A hit is detected from Aura.hitCounter().
 */
public final class NeuroRotationMode extends RotationMode {
   private final NeuroAimModel model = new NeuroAimModel(System.nanoTime());
   private Entity tracked;
   private int lastHitCounter = Integer.MIN_VALUE;

   public NeuroRotationMode() {
      this("Neuro");
   }

   public NeuroRotationMode(String name) {
      super(name);
   }

   @Override
   public Rotation process(Rotation from, Rotation to, Vec3d vec, Entity entity) {
      if (from == null || to == null) {
         return to;
      }

      boolean engaged = entity != null;
      boolean newTarget = engaged && entity != this.tracked;
      this.tracked = entity;

      boolean hit = false;
      Aura aura = (Aura)AuraRuntime.INSTANCE.moduleRepository().get(Aura.class);
      if (aura != null) {
         int counter = aura.hitCounter();
         hit = this.lastHitCounter != Integer.MIN_VALUE && counter != this.lastHitCounter;
         this.lastHitCounter = counter;
      }

      NeuroAimModel.Output out = this.model.step(new NeuroAimModel.Input(
         from.getYaw(), from.getPitch(),
         to.getYaw(), to.getPitch(),
         to.getYaw(), to.getPitch(),
         to.getYaw(), to.getPitch(),
         to.getYaw(), to.getPitch(),
         engaged, newTarget, hit));
      return new Rotation(MathHelper.wrapDegrees(out.yaw()), MathHelper.clamp(out.pitch(), -90.0F, 90.0F));
   }

   /** The model adds its own motor noise; a random aim point offset per tick would be white jitter. */
   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }

   public void reset() {
      this.model.reset();
      this.tracked = null;
      this.lastHitCounter = Integer.MIN_VALUE;
   }
}
