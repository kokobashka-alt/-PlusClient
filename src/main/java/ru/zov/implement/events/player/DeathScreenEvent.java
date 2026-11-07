package ru.zov.implement.events.player;

import ru.zov.api.event.events.Event;

public record DeathScreenEvent(int ticksSinceDeath) implements Event {
}
