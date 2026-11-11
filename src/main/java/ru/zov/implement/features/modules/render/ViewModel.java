package ru.zov.implement.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.implement.events.item.HandOffsetEvent;
import ru.zov.implement.features.modules.combat.Aura;

public class ViewModel extends Module {
   private final ValueSetting rightHandX = new ValueSetting("Правая рука X", "Смещение правой руки по X").setValue(0.0F).range(-2.0F, 2.0F);
   private final ValueSetting rightHandY = new ValueSetting("Правая рука Y", "Смещение правой руки по Y").setValue(0.0F).range(-2.0F, 2.0F);
   private final ValueSetting rightHandZ = new ValueSetting("Правая рука Z", "Смещение правой руки по Z").setValue(0.0F).range(-2.0F, 2.0F);
   private final ValueSetting leftHandX = new ValueSetting("Левая рука X", "Смещение левой руки по X").setValue(0.0F).range(-2.0F, 2.0F);
   private final ValueSetting leftHandY = new ValueSetting("Левая рука Y", "Смещение левой руки по Y").setValue(0.0F).range(-2.0F, 2.0F);
   private final ValueSetting leftHandZ = new ValueSetting("Левая рука Z", "Смещение левой руки по Z").setValue(0.0F).range(-2.0F, 2.0F);
   private final BooleanSetting onlyAura = new BooleanSetting("Только с аурой", "Смещать руки, только пока у Aura есть цель").setValue(false);

   public ViewModel() {
      super("ViewModel", ModuleCategory.RENDER);
      this.setup(this.rightHandX, this.rightHandY, this.rightHandZ, this.leftHandX, this.leftHandY, this.leftHandZ, this.onlyAura);
   }

   @EventHandler
   public void onHandOffset(HandOffsetEvent e) {
      if (mc.player == null || this.onlyAura.isValue() && !this.auraHasTarget()) {
         return;
      }

      Arm arm = e.hand() == Hand.MAIN_HAND ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
      MatrixStack matrices = e.matrices();
      if (arm == Arm.RIGHT) {
         matrices.translate(this.rightHandX.getValue(), this.rightHandY.getValue(), this.rightHandZ.getValue());
      } else {
         matrices.translate(this.leftHandX.getValue(), this.leftHandY.getValue(), this.leftHandZ.getValue());
      }
   }

   private boolean auraHasTarget() {
      Aura aura = Aura.getInstance();
      return aura != null && aura.isState() && aura.getTarget() != null;
   }
}
