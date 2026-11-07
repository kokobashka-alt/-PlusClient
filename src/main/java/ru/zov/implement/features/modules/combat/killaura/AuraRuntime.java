package ru.zov.implement.features.modules.combat.killaura;

import ru.zov.implement.features.modules.combat.killaura.net.InventoryService;
import ru.zov.implement.features.modules.combat.killaura.util.EventDispatcher;

public final class AuraRuntime {
    public static final AuraRuntime INSTANCE = new AuraRuntime();

    private final EventDispatcher eventDispatcher = new EventDispatcher();
    private final InventoryService inventoryService = new InventoryService();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ModuleRepository moduleRepository = new ModuleRepository();

    public EventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    public InventoryService inventoryService() {
        return inventoryService;
    }

    public NotificationRepository notificationRepository() {
        return notificationRepository;
    }

    public ModuleRepository moduleRepository() {
        return moduleRepository;
    }

    public static final class ModuleRepository {
        public <T> T get(Class<T> type) {
            Object value;
            if (type == ru.zov.implement.features.modules.combat.Aura.class) {
                value = ru.zov.implement.features.modules.combat.Aura.getInstance();
            } else if (type == ru.zov.implement.features.modules.combat.killaura.module.AutoJumpModule.class) {
                value = ru.zov.implement.features.modules.combat.killaura.module.AutoJumpModule.INSTANCE;
            } else if (type == ru.zov.implement.features.modules.combat.killaura.module.AutoTotemModule.class) {
                value = ru.zov.implement.features.modules.combat.killaura.module.AutoTotemModule.INSTANCE;
            } else {
                value = null;
            }
            return type.cast(value);
        }
    }

    public static final class NotificationRepository {
        public void post(ru.zov.implement.features.modules.combat.killaura.type.NotificationType type, net.minecraft.text.Text text, long duration, java.util.concurrent.TimeUnit unit) {
            ru.zov.implement.features.draggables.Notifications.getInstance().addList(text, unit.toMillis(duration));
        }
    }
}
