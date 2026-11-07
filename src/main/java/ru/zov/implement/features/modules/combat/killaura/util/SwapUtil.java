package ru.zov.implement.features.modules.combat.killaura.util;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.type.RotationDispatchMode;
import ru.zov.implement.features.modules.combat.killaura.internal.RotationDispatchModeSwitchMap;
import ru.zov.implement.features.modules.movement.ScreenWalk;

import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class SwapUtil {
    public static RotationDispatchMode resolvedMethod() {
        return resolve(switch (ScreenWalk.getBypassMode()) {
            case "FunTime" -> RotationDispatchMode.DELAYED;
            case "SpookyTime" -> RotationDispatchMode.GRIM;
            default -> RotationDispatchMode.AUTO;
        });
    }

    public static RotationDispatchMode resolve(RotationDispatchMode class389Var) {
        if (class389Var != RotationDispatchMode.AUTO) {
            return class389Var;
        }
        switch (ServerUtil.server()) {
            case "FunTime":
                if (ServerUtil.getAnarchy() < 1000 && ServerUtil.getProtocolVersion() > 767) {
                    return RotationDispatchMode.DELAYED;
                }
                break;
            case "DexLand":
            case "MineBlaze":
                if (ServerUtil.getProtocolVersion() > 767) {
                    return RotationDispatchMode.DELAYED;
                }
                break;
        }
        return RotationDispatchMode.GRIM;
    }

    public static void swapAndExecute(int i, Rotation class007Var, boolean z, Runnable runnable) {
        RotationDispatchMode class389VarResolvedMethod = resolvedMethod();
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || !isIdle()) {
            return;
        }
        int[] iArr = {player.getInventory().getSelectedSlot()};
        switch (RotationDispatchModeSwitchMap.dispatchModeSwitchMap[class389VarResolvedMethod.ordinal()]) {
            case 1:
                GrimDelayHandler.addTask(class007Var, false, () -> {
                    int selectedSlot = player.getInventory().getSelectedSlot();
                    PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, selectedSlot, SlotActionType.SWAP, true);
                    try {
                        runnable.run();
                    } finally {
                        PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, selectedSlot, SlotActionType.SWAP, true);
                        PlayerActionUtil.INSTANCE.updateSlots(true);
                    }
                });
                break;
            case 2:
                GrimDelayHandler.addTask(class007Var, z, () -> {
                    int selectedSlot = player.getInventory().getSelectedSlot();
                    PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, selectedSlot, SlotActionType.SWAP, true);
                    try {
                        runnable.run();
                    } finally {
                        PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, selectedSlot, SlotActionType.SWAP, true);
                        PlayerActionUtil.INSTANCE.updateSlots(true);
                    }
                });
                break;
            case 3:
                boolean z2 = RotationManager.computeRotationDifference(class007Var, RotationManager.INSTANCE.getCurrentRotation()) > 1.0d;
                GrimDelayHandler.rotateToAngle(z2, class007Var);
                GrimDelayHandler.script.addTickStep(0, () -> {
                    GrimDelayHandler.disableMoveKeys();
                    GrimDelayHandler.rotateToAngle(z2, class007Var);
                }).addTickStep(1, () -> {
                    iArr[0] = player.getInventory().getSelectedSlot();
                    PlayerActionUtil.INSTANCE.swapHand(i, Hand.MAIN_HAND, false);
                    try {
                        runnable.run();
                    } catch (RuntimeException | Error throwable) {
                        PlayerActionUtil.INSTANCE.swapHand(i, iArr[0], true, true);
                        GrimDelayHandler.enableMoveKeys();
                        throw throwable;
                    }
                }).addTickStep(2, () -> {
                    PlayerActionUtil.INSTANCE.swapHand(i, iArr[0], true, true);
                    GrimDelayHandler.enableMoveKeys();
                });
                break;
            case 4:
                boolean z3 = RotationManager.computeRotationDifference(class007Var, RotationManager.INSTANCE.getCurrentRotation()) > 1.0d;
                GrimDelayHandler.rotateToAngle(z3, class007Var);
                ActionScheduler class265VarAddTickStep = GrimDelayHandler.script.addTickStep(0, () -> {
                    iArr[0] = player.getInventory().getSelectedSlot();
                    PlayerActionUtil.INSTANCE.swapHand(i, Hand.MAIN_HAND, false);
                    GrimDelayHandler.rotateToAngle(z3, class007Var);
                });
                Objects.requireNonNull(runnable);
                class265VarAddTickStep.addTickStep(1, () -> {
                    try {
                        runnable.run();
                    } catch (RuntimeException | Error throwable) {
                        PlayerActionUtil.INSTANCE.swapHand(i, iArr[0], true, true);
                        throw throwable;
                    }
                }).addTickStep(2, () -> {
                    PlayerActionUtil.INSTANCE.swapHand(i, iArr[0], true, true);
                });
                break;
        }
    }

    public static boolean needsStop() {
        RotationDispatchMode method = resolvedMethod();
        return method != RotationDispatchMode.VANILLA && method != RotationDispatchMode.SEQUENTIAL;
    }

    public static void swapAction(Runnable runnable) {
        swapAction(RotationManager.INSTANCE.getCurrentRotation(), true, runnable);
    }

    public static void swapAction(Rotation class007Var, boolean z, Runnable runnable) {
        RotationDispatchMode class389VarResolvedMethod = resolvedMethod();
        if (GrimDelayHandler.script.isFinished()) {
            boolean shouldStopMovement = z && class389VarResolvedMethod != RotationDispatchMode.VANILLA && class389VarResolvedMethod != RotationDispatchMode.SEQUENTIAL;
            GrimDelayHandler.addTask(class007Var, shouldStopMovement, runnable);
        }
    }

    public static void swapToOffhand(int i) {
        RotationDispatchMode class389VarResolvedMethod = resolvedMethod();
        if (Mc.INSTANCE.getPlayer() != null && GrimDelayHandler.script.isFinished()) {
            if (Objects.requireNonNull(class389VarResolvedMethod) != RotationDispatchMode.VANILLA) {
                GrimDelayHandler.addTask(class389VarResolvedMethod != RotationDispatchMode.SEQUENTIAL, () -> {
                    PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, i, 40, false);
                    PlayerActionUtil.INSTANCE.updateSlots(true);
                });
            } else {
                PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, i, 40, false);
                PlayerActionUtil.INSTANCE.updateSlots(true);
            }
        }
    }

    public static boolean isIdle() {
        return GrimDelayHandler.script.isFinished();
    }

    public SwapUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
