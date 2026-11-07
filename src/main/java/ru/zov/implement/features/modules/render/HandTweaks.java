package ru.zov.implement.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.implement.events.item.HandAnimationEvent;
import ru.zov.implement.events.item.HandOffsetEvent;
import ru.zov.implement.events.item.SwingDurationEvent;

public class HandTweaks extends Module {
   private final SelectSetting swingType = new SelectSetting("Swing Type", "Select the type of swing").value("Swipe", "Down", "Smooth", "Power", "Feast");
   private final ValueSetting mainHandXSetting = new ValueSetting("Main Hand X", "Main Hand X value setting").setValue(0.0F).range(-1.0F, 1.0F);
   private final ValueSetting mainHandYSetting = new ValueSetting("Main Hand Y", "Main Hand Y value setting").setValue(0.0F).range(-1.0F, 1.0F);
   private final ValueSetting mainHandZSetting = new ValueSetting("Main Hand Z", "Main Hand Z value setting").setValue(0.0F).range(-2.5F, 2.5F);
   private final ValueSetting offHandXSetting = new ValueSetting("Off Hand X", "Off Hand X value setting").setValue(0.0F).range(-1.0F, 1.0F);
   private final ValueSetting offHandYSetting = new ValueSetting("Off Hand Y", "Off Hand Y value setting").setValue(0.0F).range(-1.0F, 1.0F);
   private final ValueSetting offHandZSetting = new ValueSetting("Off Hand Z", "Off Hand Z value setting").setValue(0.0F).range(-2.5F, 2.5F);
   private final GroupSetting swingGroup = new GroupSetting("Animation", "Custom Swing").settings(this.swingType).setValue(true);
   private final GroupSetting offsetGroup = new GroupSetting("Offsets", "Custom Hands offset")
      .settings(this.mainHandXSetting, this.mainHandYSetting, this.mainHandZSetting, this.offHandXSetting, this.offHandYSetting, this.offHandZSetting)
      .setValue(true);
   private final ValueSetting swingSpeedSetting = new ValueSetting("Swing Duration", "Duration of the hit animation").setValue(1.0F).range(0.5F, 2.0F);

   public HandTweaks() {
      super("HandTweaks", "Hand Tweaks", ModuleCategory.RENDER);
      this.setup(this.swingGroup, this.offsetGroup, this.swingSpeedSetting);
   }

   @EventHandler
   public void onSwingDuration(SwingDurationEvent e) {
      e.setAnimation(this.swingSpeedSetting.getValue());
      e.cancel();
   }

   @EventHandler
   public void onHandAnimation(HandAnimationEvent e) {
      if (e.getHand().equals(Hand.MAIN_HAND) && this.swingGroup.isValue()) {
         MatrixStack matrix = e.getMatrices();
         float swingProgress = e.getSwingProgress();
         int i = mc.player.getMainArm().equals(Arm.RIGHT) ? 1 : -1;
         float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
         float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
         float sinSmooth = (float)(Math.sin(swingProgress * Math.PI) * 0.5);
         switch (this.swingType.getSelected()) {
            case "Swipe":
               matrix.translate(0.56F * i, -0.32F, -0.72F);
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60 * i));
               matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60 * i));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -120.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
               break;
            case "Down":
               matrix.translate(i * 0.56F, -0.32F, -0.72F);
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F));
               matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
               break;
            case "Smooth":
               matrix.translate(i * 0.56F, -0.42F, -0.72F);
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + sin1 * -20.0F)));
               matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
               matrix.translate(0.0, -0.1, 0.0);
               break;
            case "Power":
               matrix.translate(i * 0.56F, -0.32F, -0.72F);
               matrix.translate(-sinSmooth * sinSmooth * sin1 * i, 0.0F, 0.0F);
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
               matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -30.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F));
               break;
            case "Feast":
               matrix.translate(i * 0.56F, -0.32F, -0.72F);
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
               matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
               matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
         }

         e.cancel();
      }
   }

   @EventHandler
   public void onHandOffset(HandOffsetEvent e) {
      Hand hand = e.hand();
      if (!hand.equals(Hand.MAIN_HAND) || !(e.stack().getItem() instanceof CrossbowItem)) {
         if (this.offsetGroup.isValue()) {
            MatrixStack matrix = e.matrices();
            if (hand.equals(Hand.MAIN_HAND)) {
               matrix.translate(this.mainHandXSetting.getValue(), this.mainHandYSetting.getValue(), this.mainHandZSetting.getValue());
            } else {
               matrix.translate(this.offHandXSetting.getValue(), this.offHandYSetting.getValue(), this.offHandZSetting.getValue());
            }
         }
      }
   }
}
