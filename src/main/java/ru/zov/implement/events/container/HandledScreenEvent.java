package ru.zov.implement.events.container;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import ru.zov.api.event.events.Event;

public record HandledScreenEvent(HandledScreen<?> screen, DrawContext context) implements Event {
}
