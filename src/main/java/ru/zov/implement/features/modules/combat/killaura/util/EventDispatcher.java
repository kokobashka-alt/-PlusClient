package ru.zov.implement.features.modules.combat.killaura.util;

import ru.zov.implement.features.modules.combat.killaura.event.CancellableEvent;
import ru.zov.implement.features.modules.combat.killaura.event.Event;
import ru.zov.implement.features.modules.combat.killaura.event.EventCallback;
import ru.zov.implement.features.modules.combat.killaura.event.RegisteredListener;
import ru.zov.implement.features.modules.combat.killaura.type.EventPriority;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispatcher {
    private final Map<Class<? extends Event>, List<RegisteredListener<?>>> listeners = new HashMap<>();

    public <T extends Event> void dispatch(T event) {
        List<RegisteredListener<?>> registered = listeners.get(event.getClass());
        if (registered == null) return;
        for (RegisteredListener<?> listener : List.copyOf(registered)) {
            @SuppressWarnings("unchecked")
            EventCallback<T> callback = (EventCallback<T>) listener.callback();
            callback.call(event);
            if (event instanceof CancellableEvent cancellable && cancellable.isStopProgression()) break;
        }
    }

    public <T extends Event> void register(Class<T> type, EventCallback<T> callback) {
        register(type, callback, EventPriority.NORMAL);
    }

    public <T extends Event> void register(Class<T> type, EventCallback<T> callback, EventPriority priority) {
        List<RegisteredListener<?>> registered = listeners.computeIfAbsent(type, ignored -> new ArrayList<>());
        registered.add(new RegisteredListener<>(callback, priority));
        registered.sort(Comparator.comparing(RegisteredListener::priority));
    }
}
