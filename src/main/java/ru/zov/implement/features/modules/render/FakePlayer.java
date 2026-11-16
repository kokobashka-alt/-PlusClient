package ru.zov.implement.features.modules.render;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.client.network.OtherClientPlayerEntity;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.implement.events.player.TickEvent;

public class FakePlayer extends Module {
    
    private final BooleanSetting copyEquipment =
            new BooleanSetting("Копировать экипировку", "Дублирует броню и предмет в руках игрока")
                    .setValue(true);

    private final BooleanSetting copySkin =
            new BooleanSetting("Копировать скин", "Использует текущий профиль и скин игрока")
                    .setValue(true);

    private OtherClientPlayerEntity fakePlayer;

    public FakePlayer() {
        super("FakePlayer", "Fake Player", ModuleCategory.RENDER);
        this.setup(new Setting[]{copyEquipment, copySkin});
    }

    @Override
    public void activate() {
        spawnFakePlayer();
    }

    @Override
    public void deactivate() {
        removeFakePlayer();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.world == null || mc.player == null) {
            removeFakePlayer();
            return;
        }

        // Если энтити по какой-то причине пропала из мира, пересоздаем
        if (fakePlayer == null || fakePlayer.isRemoved() || mc.world.getEntityById(fakePlayer.getId()) != fakePlayer) {
            if (this.isState()) {
                spawnFakePlayer();
            }
        }
    }

    private void spawnFakePlayer() {
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;
        if (world == null || player == null) return;

        removeFakePlayer();

        int entityId = findFreeEntityId(world);
        if (entityId == -1) return;

        GameProfile profile = copySkin.isValue()
                ? new GameProfile(UUID.randomUUID(), player.getGameProfile().name(), player.getGameProfile().properties())
                : new GameProfile(UUID.randomUUID(), "FakePlayer");

        OtherClientPlayerEntity dummy = new OtherClientPlayerEntity(world, profile);
        dummy.setId(entityId);
        dummy.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        dummy.setHeadYaw(player.getHeadYaw());
        dummy.setBodyYaw(player.getBodyYaw());

        if (copyEquipment.isValue()) {
            dummy.getInventory().clone(player.getInventory());
        }

        dummy.getAbilities().invulnerable = true;
        dummy.setHealth(20.0F);

        world.addEntity(dummy);
        this.fakePlayer = dummy;
    }

    private void removeFakePlayer() {
        if (fakePlayer != null && mc.world != null) {
            if (!fakePlayer.isRemoved()) {
                mc.world.removeEntity(fakePlayer.getId(), RemovalReason.DISCARDED);
            }
            fakePlayer = null;
        }
    }

    private int findFreeEntityId(ClientWorld world) {
        // Безопасный диапазон свободных ID для клиентских фейк-энтити
        for (int i = 0; i < 512; i++) {
            int id = 2147483646 - i;
            if (world.getEntityById(id) == null) {
                return id;
            }
        }
        return -1;
    }

    public OtherClientPlayerEntity getFakePlayer() {
        return fakePlayer;
    }
}
