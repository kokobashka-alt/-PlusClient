package ru.zov.implement.features.modules.combat.killaura.module;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class AntiBotModule {
    public static boolean isBot(LivingEntity entity) {
        return entity instanceof PlayerEntity player
                && ru.zov.implement.features.modules.combat.AntiBot.getInstance().isBot(player);
    }

    private AntiBotModule() {
    }
}
