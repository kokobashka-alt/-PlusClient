package ru.zov.implement.events.chat;

import ru.zov.api.event.events.callables.EventCancellable;

public class ChatEvent extends EventCancellable {
   private String message;

   public ChatEvent(String message) {
      this.message = message;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }
}
