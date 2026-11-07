package ru.zov.common.util.coroutine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoroutineContext {
   private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

   public void launch(Runnable task) {
      this.executor.submit(task);
   }

   public void shutdown() {
      this.executor.shutdown();
   }
}
