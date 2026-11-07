package ru.zov.common.util.other;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.module.Module;
import ru.zov.common.QuickImports;

public final class Instance implements QuickImports {
   private static final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
   private static final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

   public static <T extends Module> T get(Class<T> clazz) {
      return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> main.getModuleProvider().get((Class<? extends Module>)instance)));
   }

   public static <T extends Module> T get(String module) {
      return main.getModuleProvider().get(module);
   }

   public static <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
      return clazz.cast(instanceDraggables.computeIfAbsent(clazz, instance -> main.getDraggableRepository().get((Class<? extends AbstractDraggable>)instance)));
   }

   public static <T extends AbstractDraggable> T getDraggable(String draggable) {
      return main.getDraggableRepository().get(draggable);
   }

   private Instance() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
