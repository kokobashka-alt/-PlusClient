package ru.zov.implement.features.modules.combat.killaura.util;

import ru.zov.implement.features.modules.combat.killaura.model.PlayerSnapshot;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class PlayerSnapshotManager {
    public static final PlayerSnapshotManager INSTANCE = new PlayerSnapshotManager();
    public final Int2ObjectMap<ArrayDeque<PlayerSnapshot>> historyById = new Int2ObjectOpenHashMap<>();
    private final IntOpenHashSet seenEntityIds = new IntOpenHashSet(512);

    public void tick() {
        if (Mc.INSTANCE.getWorld() == null) {
            historyById.clear();
            return;
        }
        seenEntityIds.clear();
        for (Entity entity : Mc.INSTANCE.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            int id = living.getId();
            seenEntityIds.add(id);
            ArrayDeque<PlayerSnapshot> history = historyById.computeIfAbsent(id, ignored -> new ArrayDeque<>(41));
            history.addFirst(PlayerSnapshot.from(living));
            while (history.size() > 41) history.removeLast();
        }
        IntIterator ids = historyById.keySet().iterator();
        while (ids.hasNext()) {
            if (!seenEntityIds.contains(ids.nextInt())) ids.remove();
        }
    }

    public Optional<PlayerSnapshot> getSnapshot(LivingEntity entity, int ticks) {
        ArrayDeque<PlayerSnapshot> history = historyById.get(entity.getId());
        if (history == null || ticks < 0 || ticks > 40) return Optional.empty();
        int index = 0;
        for (PlayerSnapshot snapshot : history) {
            if (index++ == ticks) return Optional.of(snapshot);
        }
        return Optional.empty();
    }

    public PlayerSnapshot getSnapshotOrDefault(LivingEntity entity, int ticks) {
        return getSnapshot(entity, ticks).orElse(PlayerSnapshot.from(entity));
    }

    public Stream<PlayerSnapshot> getSnapshots(LivingEntity entity, int ticks) {
        ArrayDeque<PlayerSnapshot> history = historyById.get(entity.getId());
        return history == null ? Stream.empty() : history.stream().limit(Math.min(Math.max(ticks, 0), 40) + 1L);
    }
}
