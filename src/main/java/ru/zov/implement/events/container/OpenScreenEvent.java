package ru.zov.implement.events.container;

import net.minecraft.client.gui.screen.Screen;
import ru.zov.api.event.events.Event;

public class OpenScreenEvent implements Event {
   public Screen screen;

   public Screen getScreen() {
      return this.screen;
   }

   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   public OpenScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
