package ru.zov.implement.features.modules.combat.killaura.util;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;

import java.util.Objects;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.mutable.MutableObject;

public class ItemInteractionHelper {
    public ActionResult interactItem(PlayerEntity playerEntity, Hand hand, Rotation class007Var) {
        return interactItem(playerEntity, hand, class007Var, true);
    }

    public ActionResult interactItemWithoutSlotSync(
            PlayerEntity playerEntity,
            Hand hand,
            Rotation class007Var) {
        return interactItem(playerEntity, hand, class007Var, false);
    }

    private ActionResult interactItem(
            PlayerEntity playerEntity,
            Hand hand,
            Rotation class007Var,
            boolean syncSelectedSlot) {
        ClientPlayerInteractionManager interactionManager = Mc.INSTANCE.getInteractionManager();
        if (interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
            return ActionResult.PASS;
        }
        if (syncSelectedSlot) {
            interactionManager.syncSelectedSlot();
        }
        MutableObject mutableObject = new MutableObject();
        interactionManager.sendSequencedPacket(Mc.INSTANCE.getWorld(), i -> {
            PlayerInteractItemC2SPacket playerInteractItemC2SPacket = new PlayerInteractItemC2SPacket(hand, i, class007Var.getYaw(), class007Var.getPitch());
            ItemStack stackInHand = playerEntity.getStackInHand(hand);
            if (playerEntity.getItemCooldownManager().isCoolingDown(stackInHand)) {
                mutableObject.setValue(ActionResult.PASS);
                return playerInteractItemC2SPacket;
            }
            ActionResult actionResult = stackInHand.use(Mc.INSTANCE.getWorld(), playerEntity, hand);
            if (actionResult instanceof ActionResult.Success successUse) {
                ItemStack stackInHand2 = (ItemStack) Objects.requireNonNullElseGet(successUse.getNewHandStack(), () -> {
                    return playerEntity.getStackInHand(hand);
                });
                if (stackInHand2 != stackInHand) {
                    playerEntity.setStackInHand(hand, stackInHand2);
                }
            }
            // Custom server items often return FAIL/PASS from vanilla client-side
            // prediction and are handled only after the packet reaches the server.
            // The old unconditional cast to Success aborted packet creation and,
            // consequently, the swap-back step.
            mutableObject.setValue(actionResult);
            return playerInteractItemC2SPacket;
        });
        return (ActionResult) mutableObject.getValue();
    }
}
