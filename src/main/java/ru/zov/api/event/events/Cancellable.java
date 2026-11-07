package ru.zov.api.event.events;

public interface Cancellable {
   boolean isCancelled();

   void cancel();
}
