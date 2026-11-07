package ru.zov.implement.features.modules.combat.killaura.net;

import ru.zov.implement.features.modules.combat.killaura.util.InventoryItemFinder;
import ru.zov.implement.features.modules.combat.killaura.util.ItemInteractionHelper;

public final class InventoryService {
    private final InventoryItemFinder searcher = new InventoryItemFinder();
    private final HotbarSlotSwapper hotbarSlotSwapper = new HotbarSlotSwapper();
    private final ItemInteractionHelper itemInteractor = new ItemInteractionHelper();

    public ItemInteractionHelper itemInteractor() {
        return itemInteractor;
    }

    public InventoryItemFinder searcher() {
        return searcher;
    }

    public HotbarSlotSwapper hotbarSlotSwapper() {
        return hotbarSlotSwapper;
    }

    public static final class HotbarSlotSwapper {
        public void cancel(Object owner) {
        }
    }
}
