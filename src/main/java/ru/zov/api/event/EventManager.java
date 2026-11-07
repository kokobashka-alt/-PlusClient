package ru.zov.api.event;

import java.lang.reflect.Method;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.zov.api.event.events.Event;
import ru.zov.api.event.events.EventStoppable;
import ru.zov.api.event.types.Priority;
import ru.zov.api.feature.module.exception.ModuleException;
import ru.zov.api.system.logger.implement.ConsoleLogger;
import ru.zov.api.system.logger.implement.MinecraftLogger;

public final class EventManager {
   private static final Map<Class<? extends Event>, List<EventManager.MethodData>> REGISTRY_MAP = new HashMap<>();

   public void register(Object object) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!isMethodBad(method)) {
            this.register(method, object);
         }
      }
   }

   public void register(Object object, Class<? extends Event> eventClass) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!isMethodBad(method, eventClass)) {
            this.register(method, object);
         }
      }
   }

   public void unregister(Object object) {
      for (List<EventManager.MethodData> dataList : REGISTRY_MAP.values()) {
         for (EventManager.MethodData data : dataList) {
            if (data.source().equals(object)) {
               dataList.remove(data);
            }
         }
      }

      cleanMap(true);
   }

   public void unregister(Object object, Class<? extends Event> eventClass) {
      if (REGISTRY_MAP.containsKey(eventClass)) {
         for (EventManager.MethodData data : REGISTRY_MAP.get(eventClass)) {
            if (data.source().equals(object)) {
               REGISTRY_MAP.get(eventClass).remove(data);
            }
         }

         cleanMap(true);
      }
   }

   private void register(Method method, Object object) {
      Class<? extends Event> indexClass = (Class<? extends Event>)method.getParameterTypes()[0];
      if (!method.isAccessible()) {
         method.setAccessible(true);
      }

      MethodHandle invoker;
      try {
         invoker = MethodHandles.lookup()
            .unreflect(method)
            .bindTo(object)
            .asType(MethodType.methodType(void.class, Event.class));
      } catch (IllegalAccessException e) {
         throw new IllegalStateException("Unable to register event handler " + method, e);
      }
      final EventManager.MethodData data = new EventManager.MethodData(object, method, method.getAnnotation(EventHandler.class).value(), invoker);

      if (REGISTRY_MAP.containsKey(indexClass)) {
         boolean alreadyRegistered = REGISTRY_MAP.get(indexClass).stream()
            .anyMatch(existing -> existing.source() == object && existing.target().equals(method));
         if (!alreadyRegistered) {
            REGISTRY_MAP.get(indexClass).add(data);
            sortListValue(indexClass);
         }
      } else {
         REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<EventManager.MethodData>() {
            private static final long serialVersionUID = 666L;

            {
               this.add(data);
            }
         });
      }
   }

   public void removeEntry(Class<? extends Event> indexClass) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (mapIterator.next().getKey().equals(indexClass)) {
            mapIterator.remove();
            break;
         }
      }
   }

   public static void cleanMap(boolean onlyEmptyEntries) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty()) {
            mapIterator.remove();
         }
      }
   }

   private static void sortListValue(Class<? extends Event> indexClass) {
      List<EventManager.MethodData> sortedList = new CopyOnWriteArrayList<>();

      for (byte priority : Priority.VALUE_ARRAY) {
         for (EventManager.MethodData data : REGISTRY_MAP.get(indexClass)) {
            if (data.priority() == priority) {
               sortedList.add(data);
            }
         }
      }

      REGISTRY_MAP.put(indexClass, sortedList);
   }

   private static boolean isMethodBad(Method method) {
      return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventHandler.class);
   }

   private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
      return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
   }

   public static Event callEvent(Event event) {
      List<EventManager.MethodData> dataList = REGISTRY_MAP.get(event.getClass());
      if (dataList != null) {
         if (event instanceof EventStoppable stoppable) {
            for (EventManager.MethodData data : dataList) {
               invoke(data, event);
               if (stoppable.isStopped()) {
                  break;
               }
            }
         } else {
            for (EventManager.MethodData data : dataList) {
               try {
                  invoke(data, event);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
      }

      return event;
   }

   private static void invoke(EventManager.MethodData data, Event argument) {
      try {
         data.invoker().invokeExact(argument);
      } catch (Throwable cause) {
         ConsoleLogger consoleLogger = new ConsoleLogger();
         MinecraftLogger minecraftLogger = new MinecraftLogger();
         if (cause instanceof ModuleException moduleException) {
            minecraftLogger.minecraftLog(Text.literal("[" + moduleException.getModuleName() + "] " + Formatting.RED + moduleException.getMessage()));
         } else {
            String errorMessage = "Exception occurred within invoked method. ";
            errorMessage = errorMessage + "Method: " + data.target().getName() + ", ";
            errorMessage = errorMessage + "Argument: " + argument.toString() + ", ";
            errorMessage = errorMessage + "Log: " + cause;
            consoleLogger.log(errorMessage);
         }
      }
   }

   private record MethodData(Object source, Method target, byte priority, MethodHandle invoker) {
   }
}
