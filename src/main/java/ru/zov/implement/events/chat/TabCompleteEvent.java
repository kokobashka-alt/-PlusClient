package ru.zov.implement.events.chat;

import ru.zov.api.event.events.callables.EventCancellable;

public class TabCompleteEvent extends EventCancellable {
   public final String prefix;
   public String[] completions;

   public TabCompleteEvent(String prefix) {
      this.prefix = prefix;
      this.completions = null;
   }
}
