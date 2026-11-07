package ru.zov.implement.events.player;

import net.minecraft.util.PlayerInput;
import ru.zov.api.event.events.callables.EventCancellable;

public class InputEvent extends EventCancellable {
   private PlayerInput input;

   public void setJump(boolean jump) {
      this.input = new PlayerInput(
         this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), jump, this.input.sneak(), this.input.sprint()
      );
   }

   public void setSprint(boolean sprint) {
      this.input = new PlayerInput(
         this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), this.input.sneak(), sprint
      );
   }

   public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
      this.input = new PlayerInput(forward, backward, left, right, this.input.jump(), this.input.sneak(), this.input.sprint());
   }

   public void setForward(boolean forward) {
      this.input = new PlayerInput(
         forward, this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), this.input.sneak(), this.input.sprint()
      );
   }

   public void inputNone() {
      this.input = new PlayerInput(false, false, false, false, false, false, false);
   }

   public int forward() {
      return this.input.forward() ? 1 : (this.input.backward() ? -1 : 0);
   }

   public int sideways() {
      return this.input.left() ? 1 : (this.input.right() ? -1 : 0);
   }

   public PlayerInput getInput() {
      return this.input;
   }

   public void setInput(PlayerInput input) {
      this.input = input;
   }

   public InputEvent(PlayerInput input) {
      this.input = input;
   }
}
