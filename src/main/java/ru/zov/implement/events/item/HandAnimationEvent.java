package ru.zov.implement.events.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import ru.zov.api.event.events.callables.EventCancellable;

public class HandAnimationEvent extends EventCancellable {
   private MatrixStack matrices;
   private Hand hand;
   private float swingProgress;
   private float equipProgress;

   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
   }

   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress, float equipProgress) {
      this(matrices, hand, swingProgress);
      this.equipProgress = equipProgress;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public Hand getHand() {
      return this.hand;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }

   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }
}
