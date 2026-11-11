package ru.zov.implement.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.implement.events.item.HandAnimationEvent;
import ru.zov.implement.events.item.SwingDurationEvent;
import ru.zov.implement.events.render.ItemRendererEvent;
import ru.zov.implement.features.modules.combat.Aura;

public class SwingAnimations extends Module {
   private static final float VANILLA_SWING_FACTOR = 6.0F;
   private static final float TRUNCATION_GUARD = 0.001F;

   private static boolean pouchDirection = true;
   private static boolean pouchWasSwinging = false;

   private final BooleanSetting swingEnabled = new BooleanSetting("Анимация свинга", "Заменять анимацию удара").setValue(true);
   private final SelectSetting swingType = new SelectSetting("Тип свинга", "Вид анимации удара")
      .value("Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack", "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant")
      .selected("Smooth")
      .visible(this.swingEnabled::isValue);
   private final ValueSetting swingStrength = new ValueSetting("Сила анимации", "Множитель амплитуды анимации")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(() -> this.swingEnabled.isValue() && !this.swingType.isSelected("Pander"));
   private final ValueSetting corner = new ValueSetting("Угол DropDown", "Начальный угол наклона руки в DropDown")
      .setValue(12.0F)
      .range(1, 360)
      .visible(() -> this.swingEnabled.isValue() && this.swingType.isSelected("DropDown"));
   private final ValueSetting slant = new ValueSetting("Наклон DropDown", "Размах наклона руки в DropDown")
      .setValue(12.0F)
      .range(1, 360)
      .visible(() -> this.swingEnabled.isValue() && this.swingType.isSelected("DropDown"));
   private final BooleanSetting smoothEnabled = new BooleanSetting("Плавная анимация", "Задать длительность удара в тиках").setValue(false);
   private final ValueSetting slowAnimationSpeed = new ValueSetting("Скорость анимации", "Длительность удара в тиках")
      .setValue(12.0F)
      .range(1, 50)
      .visible(this.smoothEnabled::isValue);
   private final BooleanSetting auraTargetOnly = new BooleanSetting("Только при Aura", "Анимация только пока у Aura есть живая цель").setValue(false);
   private final BooleanSetting swapHands = new BooleanSetting("Свап рук", "Менять руки местами при рендере").setValue(false);
   private final BooleanSetting pouchMode = new BooleanSetting("каст режим", "Чередовать сторону удара при каждом новом ударе")
      .setValue(false)
      .visible(this.swingEnabled::isValue);

   public SwingAnimations() {
      super("SwingAnimations", ModuleCategory.RENDER);
      this.setup(
         this.swingEnabled,
         this.swingType,
         this.swingStrength,
         this.corner,
         this.slant,
         this.smoothEnabled,
         this.slowAnimationSpeed,
         this.auraTargetOnly,
         this.swapHands,
         this.pouchMode
      );
   }

   @EventHandler
   public void onItemRenderer(ItemRendererEvent e) {
      if (this.swapHands.isValue() && mc.player != null && e.getPlayer() == mc.player) {
         e.setHand(e.getHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
      }
   }

   @EventHandler
   public void onSwingDuration(SwingDurationEvent e) {
      if (!this.smoothEnabled.isValue() || mc.player == null) {
         return;
      }

      e.setAnimation(this.slowAnimationSpeed.getValue() / this.swingDurationFactor(mc.player) + TRUNCATION_GUARD);
      e.cancel();
   }

   @EventHandler
   public void onHandAnimation(HandAnimationEvent e) {
      if (!this.swingEnabled.isValue() || mc.player == null) {
         return;
      }

      float swingProgress = e.getSwingProgress();
      if (this.pouchMode.isValue()) {
         this.updatePouchSwing(swingProgress);
      }

      if (this.auraTargetOnly.isValue() && !this.auraHasTarget()) {
         return;
      }

      Arm mainArm = mc.player.getMainArm();
      Arm arm = e.getHand() == Hand.MAIN_HAND ? mainArm : mainArm.getOpposite();
      Arm swingArm = this.swapHands.isValue() ? mainArm.getOpposite() : mainArm;
      if (arm != swingArm) {
         return;
      }

      int side = this.pouchMode.isValue() ? (pouchDirection ? 1 : -1) : (arm == Arm.RIGHT ? 1 : -1);
      if (this.applySwing(e.getMatrices(), side, swingProgress, e.getEquipProgress())) {
         e.cancel();
      }
   }

   private boolean applySwing(MatrixStack matrices, int i, float swingProgress, float equipProgress) {
      float strength = this.swingStrength.getValue();
      float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
      float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
      float anim = (float) Math.sin((double) swingProgress * (Math.PI / 2) * 2.0);

      switch (this.swingType.getSelected()) {
         case "Down":
            matrices.translate(i * 0.56F, -0.32F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            return true;
         case "Poke": {
            float tilt = strength / 3.0F;
            matrices.translate(i * 0.56F, -0.52F, -0.72F);
            matrices.translate(0.0F, 0.0F, tilt * -anim);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-75.0F * (strength / 4.0F) * anim - 60.0F) * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75.0F));
            return true;
         }
         case "Static":
            matrices.translate(i * 0.56F, -0.42F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60.0F * strength));
            matrices.translate(0.0, -0.1, 0.0);
            return true;
         case "Feast":
            matrices.translate(i * 0.56F, -0.32F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -65.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * i));
            return true;
         case "Akrien":
            matrices.translate(i * 0.65F, -0.32F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 25.0F * strength));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -25.0F * strength));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin1 * 15.0F * strength));
            matrices.translate(sin2 * 0.18F * strength, sin2 * 0.59F * strength, 0.0F);
            return true;
         case "Smooth":
            this.applySwingOffset(matrices, i, swingProgress, strength);
            return true;
         case "Block":
            if (swingProgress > 0.0F) {
               matrices.translate(0.56F * i, equipProgress * -0.2F - 0.5F, -0.7F);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -85.0F * strength));
               matrices.translate(-0.1F * i, 0.28F, 0.2F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
            } else {
               float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
               float m = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
               float f = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
               matrices.translate(n * i * strength, m * strength, f * strength);
               this.applyEquipOffset(matrices, i, equipProgress);
               this.applySwingOffset(matrices, i, swingProgress, strength);
            }
            return true;
         case "ToBack":
            matrices.translate(0.65F * i, -0.45F, -0.9F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30.0F * (1.0F - sin2 * strength) - 30.0F) * i));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0F * i));
            return true;
         case "SelfBack":
            matrices.translate(0.65F * i, -0.3F, -0.8F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F - 60.0F * strength * anim));
            return true;
         case "Break":
            matrices.translate(0.66F * i, -0.3F, -0.38F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 10.0F * strength));
            matrices.scale(0.5F, 0.5F, 0.5F);
            matrices.translate(-0.1F * i, 0.2F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-105.0F * i));
            return true;
         case "DropDown":
            this.applyEquipOffset(matrices, i, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.corner.getValue()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.slant.getValue() * anim * strength));
            return true;
         case "Pander": {
            float panderAnim = MathHelper.sin(swingProgress * (float) Math.PI);
            float panderF = 1.0F - equipProgress;
            matrices.translate(i * 0.56F, -0.52F, -0.72F);
            matrices.translate((0.3F - panderAnim * 0.15F) * i, 0.2F - panderF * 0.12F, -0.15F - panderAnim * 0.13F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((76.0F - 10.0F * panderAnim) * i));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-16.0F - 8.0F * panderAnim) * i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * panderAnim));
            return true;
         }
         case "Slant": {
            float rotate = 35.0F * strength;
            matrices.translate(i * 0.56F, -0.52F, -0.72F);
            matrices.translate(0.0F, 0.0F, -0.3F * anim * strength);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -rotate));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * rotate));
            return true;
         }
         default:
            return false;
      }
   }

   private void applyEquipOffset(MatrixStack matrices, int i, float equipProgress) {
      matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(MatrixStack matrices, int i, float swingProgress, float strength) {
      float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
      matrices.translate(0.56F * i, -0.52F, -0.72F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F * strength)));
      float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F * strength));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F * strength));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
   }

   private void updatePouchSwing(float swingProgress) {
      boolean swinging = swingProgress > 0.01F;
      if (swinging && !pouchWasSwinging) {
         pouchDirection = !pouchDirection;
      }

      pouchWasSwinging = swinging;
   }

   private boolean auraHasTarget() {
      Aura aura = Aura.getInstance();
      return aura != null && aura.isState() && aura.getTarget() != null && aura.getTarget().isAlive();
   }

   private float swingDurationFactor(LivingEntity player) {
      float factor;
      if (StatusEffectUtil.hasHaste(player)) {
         factor = 6 - (1 + StatusEffectUtil.getHasteAmplifier(player));
      } else if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         factor = 6 + (1 + player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2;
      } else {
         factor = VANILLA_SWING_FACTOR;
      }

      return factor > 0.0F ? factor : VANILLA_SWING_FACTOR;
   }
}
