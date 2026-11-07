package ru.zov.implement.events.render;

import net.minecraft.client.gui.DrawContext;
import ru.zov.api.event.events.Event;

public record DrawEvent(DrawContext context) implements Event {
}
