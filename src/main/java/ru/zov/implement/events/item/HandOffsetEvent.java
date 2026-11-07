package ru.zov.implement.events.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import ru.zov.api.event.events.Event;

public record HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) implements Event {
}
