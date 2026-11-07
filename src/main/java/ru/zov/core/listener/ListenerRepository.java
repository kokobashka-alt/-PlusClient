package ru.zov.core.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.QuickImports;
import ru.zov.core.Main;
import ru.zov.core.listener.impl.EventListener;

public class ListenerRepository implements QuickImports {
   private final List<Listener> listeners = new ArrayList<>();

   @Native
   public void setup() {
      this.registerListeners(new EventListener());
   }

   @Native
   public void registerListeners(Listener... listeners) {
      this.listeners.addAll(List.of(listeners));
      Arrays.stream(listeners).forEach(listener -> Main.getInstance().getEventManager().register(listener));
   }

   public List<Listener> getListeners() {
      return this.listeners;
   }
}
