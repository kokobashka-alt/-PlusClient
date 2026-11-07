package ru.zov.implement.events.render;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import ru.zov.api.event.events.Event;

public record DrawableChildEvent(List<ClickableWidget> list, Screen screen) implements Event {
   public void add(ClickableWidget widget) {
      this.list.add(widget);
   }
}
