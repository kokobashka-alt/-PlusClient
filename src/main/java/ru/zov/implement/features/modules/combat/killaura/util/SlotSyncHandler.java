package ru.zov.implement.features.modules.combat.killaura.util;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;

public final class SlotSyncHandler {
    public static boolean lastSprinting;
    public static int selectedSlot;

    public static void packet(Packet<?> packet) {
        if (packet instanceof ClientCommandC2SPacket command) {
            if (command.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) lastSprinting = true;
            if (command.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) lastSprinting = false;
        } else if (packet instanceof UpdateSelectedSlotC2SPacket slot) {
            selectedSlot = slot.getSelectedSlot();
        } else if (packet instanceof UpdateSelectedSlotS2CPacket slot) {
            selectedSlot = slot.slot();
        }
    }

    private SlotSyncHandler() {
    }
}
