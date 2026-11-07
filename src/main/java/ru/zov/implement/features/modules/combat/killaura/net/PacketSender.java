package ru.zov.implement.features.modules.combat.killaura.net;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.util.SlotSyncHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public final class PacketSender {
    private static final ThreadLocal<Integer> EVENT_BYPASS_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    public static void sendPacket(Packet<?> packet) {
        if (packet instanceof UpdateSelectedSlotC2SPacket) {
            UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket = (UpdateSelectedSlotC2SPacket) packet;
            if (MinecraftClient.getInstance().getCurrentServerEntry() != null) {
                if (updateSelectedSlotC2SPacket.getSelectedSlot() == SlotSyncHandler.selectedSlot) {
                    return;
                } else {
                    SlotSyncHandler.selectedSlot = updateSelectedSlotC2SPacket.getSelectedSlot();
                }
            }
        }
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return;
        }
        player.networkHandler.getConnection().send(packet);
    }

    public static void sendSequencedNotSilentPacket(SequencedPacketCreator sequencedPacketCreator) {
        Mc.INSTANCE.getInteractionManager().sendSequencedPacket(Mc.INSTANCE.getWorld(), sequencedPacketCreator);
    }

    public static void sendPacketWithoutEvent(Packet<?> packet) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || packet == null) {
            return;
        }
        EVENT_BYPASS_DEPTH.set(EVENT_BYPASS_DEPTH.get() + 1);
        try {
            player.networkHandler.getConnection().send(packet);
        } finally {
            int depth = EVENT_BYPASS_DEPTH.get() - 1;
            if (depth <= 0) {
                EVENT_BYPASS_DEPTH.remove();
            } else {
                EVENT_BYPASS_DEPTH.set(depth);
            }
        }
    }

    public static boolean isBypassingEvents() {
        return EVENT_BYPASS_DEPTH.get() > 0;
    }

    public static void sendSequencedPacket(SequencedPacketCreator sequencedPacketCreator) {
        Mc class815Var = Mc.INSTANCE;
        ClientWorld world = class815Var.getWorld();
        ClientPlayerEntity player = class815Var.getPlayer();
        if (world == null || player == null) {
            return;
        }
        try (PendingUpdateManager pendingUpdateManagerIncrementSequence = world.getPendingUpdateManager().incrementSequence()) {
            player.networkHandler.getConnection().send(sequencedPacketCreator.predict(pendingUpdateManagerIncrementSequence.getSequence()));
        }
    }

    public PacketSender() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
