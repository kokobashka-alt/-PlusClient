package ru.zov.implement.features.modules.combat.killaura.util;
import ru.zov.implement.features.modules.combat.killaura.math.DirectionalInput;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.module.Module;
import ru.zov.implement.features.modules.combat.killaura.ui.ModuleTab;
import ru.zov.implement.features.modules.combat.killaura.event.MovementInputEvent;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.config.RotationConfig;
import ru.zov.implement.features.modules.combat.killaura.math.RotationVector;
import ru.zov.implement.features.modules.combat.killaura.math.SimulatedPlayer;

import java.util.Objects;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.PlayerInput;
import ru.zov.implement.features.modules.movement.ScreenWalk;

public final class GrimDelayHandler {
    public static final ActionScheduler script = new ArizonaScript();
    public static boolean canMove = true;
    public static final Mc mc = Mc.INSTANCE;
    static final Module rotationModule = new Module(ModuleTab.MISC, "GrimDelayHandler").setStateSilent(true);

    public static void onWorldChange() {
        canMove = true;
        script.cleanup();
        refreshPressedKeys();
    }

    public static void tick() {
        try {
            script.update().cleanupIfFinished();
        } catch (RuntimeException | Error throwable) {
            onWorldChange();
            throw throwable;
        }
    }

    public static void input(MovementInputEvent class040Var) {
        if (canMove) {
            return;
        }
        class040Var.setStopProgression(true);
        class040Var.setJumping(false);
        class040Var.setSprinting(false);
        class040Var.setSneaking(false);
        class040Var.setInput(DirectionalInput.NONE);
    }

    public static void addTask(Runnable runnable) {
        addTask(RotationManager.INSTANCE.getCurrentRotation(), runnable);
    }

    public static void addTask(boolean z, Runnable runnable) {
        addTask(RotationManager.INSTANCE.getCurrentRotation(), z, runnable);
    }

    public static int getTaskRunnableTime() {
            switch (ScreenWalk.getBypassMode()) {
            case "FunTime":
                return (ServerUtil.getAnarchy() >= 100 || ServerUtil.getProtocolVersion() <= 767) ? 1 : 2;
            case "SpookyTime":
                if (!MovementInputHelper.hasPlayerMovement()) {
                    return 1;
                }
                SimulatedPlayer class136VarSimulateLocalPlayer = SimulatedPlayer.simulateLocalPlayer(2);
                return (class136VarSimulateLocalPlayer.onGround || class136VarSimulateLocalPlayer.touchingWater) ? 1 : 2;
            default:
                return 1;
        }
    }

    public static void addTask(Rotation class007Var, Runnable runnable) {
        boolean shouldRotate =
                RotationManager.computeRotationDifference(
                                class007Var,
                                RotationManager.INSTANCE.getCurrentRotation())
                        > 1.0d;
        if (MovementInputHelper.hasPlayerMovement()
                && ScreenWalk.isBypassMode("SpookyTime")) {
            SimulatedPlayer simulatedPlayer =
                    SimulatedPlayer.simulateLocalPlayer(2);
            if (!simulatedPlayer.onGround && !simulatedPlayer.touchingWater) {
                ActionScheduler queued = script.addTickStep(1, () -> {
                    disableMoveKeys();
                    rotateToAngle(shouldRotate, class007Var);
                });
                Objects.requireNonNull(runnable);
                queued.addTickStep(2, runnable::run)
                        .addTickStep(3, GrimDelayHandler::enableMoveKeys);
                return;
            }
        }

        script.addTickStep(0, () -> {
                    disableMoveKeys();
                    rotateToAngle(shouldRotate, class007Var);
                })
                .addTickStep(1, () -> {
                    runnable.run();
                    enableMoveKeys();
                });
    }

    public static void addTask(Rotation class007Var, boolean z, Runnable runnable) {
        if (z) {
            addTask(class007Var, runnable);
            return;
        }
        boolean z2 = RotationManager.computeRotationDifference(class007Var, RotationManager.INSTANCE.getCurrentRotation()) > 1.0d;
        ActionScheduler queued =
                script.addTickStep(0, () -> rotateToAngle(z2, class007Var));
        Objects.requireNonNull(runnable);
        queued.addTickStep(1, runnable::run);
    }

    public static void rotateToAngle(boolean z, Rotation class007Var) {
        if (z) {
            rotateToAngle(class007Var);
        }
    }

    public static void rotateToAngle(Rotation class007Var) {
        RotationManager.INSTANCE.clear();
        RotationManager.INSTANCE.scheduleRotation(new RotationVector(class007Var, class007Var.getDirectionVector()), (LivingEntity) mc.getPlayer(), RotationConfig.BLOCK_WITH_CORRECTION, 3, rotationModule, 5);
    }

    public static void disableMoveKeys() {
        canMove = false;
        releaseKeys();
        if (mc.isWorldLoaded() && mc.getPlayer() != null) {
            // Let the vanilla movement sender flush this state on its normal
            // tick. Sending an extra PlayerInput packet here trips BadPacketsZ.
            mc.getPlayer().input.playerInput = PlayerInput.DEFAULT;
        }
    }

    public static void enableMoveKeys() {
        if (mc.getMinecraft().currentScreen != null) {
            mc.getMinecraft().setScreen(null);
        }
        enableMoveKeysWithoutClosingScreen();
    }

    public static void enableMoveKeysWithoutClosingScreen() {
        canMove = true;
        refreshPressedKeys();
    }

    public static void releaseKeys() {
        for (KeyBinding keyBinding : MovementInputHelper.getMovementKeys(true, true)) {
            keyBinding.setPressed(false);
        }
    }

    public static void refreshPressedKeys() {
        // Uses the currently configured binds (including remapped sneak/jump),
        // unlike getDefaultKey(), which always reads physical Left Shift.
        KeyBinding.updatePressedStates();
    }

    public GrimDelayHandler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
