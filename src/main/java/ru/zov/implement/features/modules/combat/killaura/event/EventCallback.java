package ru.zov.implement.features.modules.combat.killaura.event;


public interface EventCallback<T extends Event> {
    void call(T t);
}
