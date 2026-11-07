package ru.zov.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.common.util.rotation.RotationPlan;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.event.MovementInputEvent;
import ru.zov.implement.features.modules.combat.killaura.event.MovementUpdateEvent;
import ru.zov.implement.features.modules.combat.killaura.event.MovementUpdateSource;
import ru.zov.implement.features.modules.combat.killaura.math.DirectionalInput;

@Mixin({KeyboardInput.class})
public class KeyboardInputMixin implements QuickImports {
   @ModifyExpressionValue(
      method = {"tick"},
      at = {@At(
   value = "NEW",
   target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"
)}
   )
   private PlayerInput tickHook(PlayerInput original) {
      InputEvent event = new InputEvent(original);
      EventManager.callEvent(event);
      PlayerInventoryComponent.input(event);
      PlayerInput current = event.getInput();
      MovementInputEvent donorInput = new MovementInputEvent(
         DirectionalInput.fromInput(current), current.jump(), current.sneak(), current.sprint()
      );
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(donorInput);
      ru.zov.implement.features.modules.combat.killaura.util.GrimDelayHandler.input(donorInput);
      MovementUpdateEvent donorUpdate = new MovementUpdateEvent(
         donorInput.getInput(), donorInput.isSprinting(), MovementUpdateSource.INPUT
      );
      AuraRuntime.INSTANCE.eventDispatcher().dispatch(donorUpdate);
      event.setInput(new PlayerInput(
         donorInput.getInput().forward(), donorInput.getInput().backward(),
         donorInput.getInput().left(), donorInput.getInput().right(),
         donorInput.isJumping(), donorInput.isSneaking(), donorInput.isSprinting()
      ));
      return this.transformInput(event.getInput());
   }

   @Unique
   private PlayerInput transformInput(PlayerInput input) {
      RotationController rotationController = RotationController.INSTANCE;
      Angle angle = rotationController.getCurrentAngle();
      RotationPlan configurable = rotationController.getCurrentRotationPlan();
      if (mc.player != null && angle != null && configurable != null && configurable.moveCorrection() && configurable.freeCorrection()) {
         float deltaYaw = mc.player.getYaw() - angle.getYaw();
         float z = KeyboardInput.getMovementMultiplier(input.forward(), input.backward());
         float x = KeyboardInput.getMovementMultiplier(input.left(), input.right());
         float newX = x * MathHelper.cos(deltaYaw * ((float)Math.PI / 180F)) - z * MathHelper.sin(deltaYaw * ((float)Math.PI / 180F));
         float newZ = z * MathHelper.cos(deltaYaw * ((float)Math.PI / 180F)) + x * MathHelper.sin(deltaYaw * ((float)Math.PI / 180F));
         int movementSideways = Math.round(newX);
         int movementForward = Math.round(newZ);
         return new PlayerInput((float)movementForward > 0.0F, (float)movementForward < 0.0F, (float)movementSideways > 0.0F, (float)movementSideways < 0.0F, input.jump(), input.sneak(), input.sprint());
      } else {
         return input;
      }
   }
}
