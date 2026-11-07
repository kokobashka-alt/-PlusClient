package ru.zov.implement.events.block;

import net.minecraft.util.math.BlockPos;
import ru.zov.api.event.events.Event;

public record BreakBlockEvent(BlockPos blockPos) implements Event {
}
