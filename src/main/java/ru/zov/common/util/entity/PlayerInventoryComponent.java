package ru.zov.common.util.entity;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.QuickImports;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.task.TaskPriority;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.player.InputEvent;
import ru.zov.implement.features.modules.movement.ScreenWalk;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.RotationConfig;
import ru.zov.common.util.rotation.RotationController;
import ru.zov.common.util.rotation.angle.BlockSmoothMode;
import ru.zov.implement.screens.menu.MenuScreen;

public final class PlayerInventoryComponent implements QuickImports {
   public static final List<KeyBinding> moveKeys;
   public static final Script script;
   public static boolean canMove;

   public static void tick() {
      script.update().cleanupIfFinished();
   }

   public static void input(InputEvent e) {
      if (!canMove) {
         e.inputNone();
      }

   }

   public static void addTask(Runnable task) {
      addTask(RotationController.INSTANCE.getRotation(), task);
   }

   public static int getTaskTime() {
      switch (ScreenWalk.getBypassMode()) {
         case "FunTime":
            if (ServerUtil.getAnarchy() < 100 && PlayerIntersectionUtil.getProtocolVersion() > 767) {
               return 2;
            }
            break;
         case "SpookyTime":
            if (MovingUtil.hasPlayerMovement()) {
               SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(2);
               if (!simulatedPlayer.onGround && !simulatedPlayer.touchingWater) {
                  return 2;
               }
            }
      }

      return 1;
   }

   public static void addTask(Angle angle, Runnable task) {
      boolean rotate = RotationController.computeRotationDifference(angle, RotationController.INSTANCE.getRotation()) > (double)1.0F;
      if (MovingUtil.hasPlayerMovement() && ScreenWalk.isBypassMode("SpookyTime")) {
         SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(2);
         if (!simulatedPlayer.onGround && !simulatedPlayer.touchingWater) {
            Script var10000 = script.addTickStep(1, () -> {
               disableMoveKeys();
               rotateToAngle(rotate, angle);
            });
            Objects.requireNonNull(task);
            var10000.addTickStep(2, task::run).addTickStep(3, PlayerInventoryComponent::enableMoveKeys);
            return;
         }
      }

      script.addTickStep(0, () -> {
         disableMoveKeys();
         rotateToAngle(rotate, angle);
      }).addTickStep(1, () -> {
         task.run();
         enableMoveKeys();
      });
   }

   public static void rotateToAngle(boolean bool, Angle angle) {
      if (bool) {
         rotateToAngle(angle);
      }

   }

   public static void rotateToAngle(Angle angle) {
      RotationController.INSTANCE.clear();
      RotationController.INSTANCE.rotateTo(Angle.VecRotation.of(angle), mc.player, 2, new RotationConfig(new BlockSmoothMode(), true, true), TaskPriority.HIGH_IMPORTANCE_3, (new Module("InventoryComponent", ModuleCategory.PLAYER)).silentEnable());
   }

   public static void disableMoveKeys() {
      canMove = false;
      unPressMoveKeys();
   }

   public static void enableMoveKeys() {
      PlayerInventoryUtil.closeScreen(true);
      canMove = true;
      updateMoveKeys();
   }

   public static void unPressMoveKeys() {
      moveKeys.forEach((keyBinding) -> keyBinding.setPressed(false));
   }

   public static void updateMoveKeys() {
       moveKeys.forEach((keyBinding) -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow(), keyBinding.getDefaultKey().getCode())));
   }

   public static boolean shouldSkipExecution() {
      return mc.currentScreen != null && !PlayerIntersectionUtil.isChat(mc.currentScreen) && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen) && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !(mc.currentScreen instanceof MenuScreen);
   }

   private PlayerInventoryComponent() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey);
      script = new Script();
      canMove = true;
   }
}
