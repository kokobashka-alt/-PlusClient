package ru.zov.implement.features.modules.misc;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

final class HpReader {
    private HpReader() {
    }

    static float health(LivingEntity entity, boolean fixerActive) {
        if (entity == null) return 0f;
        if (fixerActive && entity instanceof PlayerEntity player) {
            Float score = HpScoreboard.read(player);
            if (score == null) score = HpOverhead.read(player);
            if (score != null) return clamp(score, entity);
        }
        return clamp(entity.getHealth() + entity.getAbsorptionAmount(), entity);
    }

    private static float clamp(float v, LivingEntity e) {
        return Math.max(0f, Math.min(v, Math.max(1f, e.getMaxHealth() + e.getAbsorptionAmount())));
    }
}
