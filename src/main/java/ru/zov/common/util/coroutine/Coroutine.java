package ru.zov.common.util.coroutine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Coroutine {
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private final CompletableFuture<Void> future;

   public Coroutine(Runnable task) {
      this.future = CompletableFuture.runAsync(task, this.executor);
   }

   public void await() throws Exception {
      this.future.get();
   }

   public void shutdown() {
      this.executor.shutdown();
   }
}
