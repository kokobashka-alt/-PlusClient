package ru.zov.implement.events.render;

import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.event.events.Event;

public record WorldRenderEvent(MatrixStack stack, float partialTicks) implements Event {
}
