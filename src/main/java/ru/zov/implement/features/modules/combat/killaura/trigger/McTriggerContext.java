package ru.zov.implement.features.modules.combat.killaura.trigger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;

/** Minecraft interaction helpers for the crosshair target. */
public final class McTriggerContext {
    public static final double DEFAULT_REACH = 4.0;

    private final double reachSquared;
    private LivingEntity cached;
    private int cachedId = -1;

    public McTriggerContext() {
        this(DEFAULT_REACH);
    }

    public McTriggerContext(double reach) {
        double safeReach = reach > 0.0 ? reach : DEFAULT_REACH;
        this.reachSquared = safeReach * safeReach;
    }

    public int targetId() {
        this.cached = null;
        this.cachedId = -1;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return -1;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) {
            return -1;
        }
        Entity entity = entityHit.getEntity();
        if (!(entity instanceof LivingEntity living)) {
            return -1;
        }

        this.cached = living;
        this.cachedId = living.getId();
        return this.cachedId;
    }

    public boolean valid(int id) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        LivingEntity target = this.cached;
        return player != null
                && target != null
                && id == this.cachedId
                && target.isAlive()
                && player.isAlive()
                && player.squaredDistanceTo(target) <= this.reachSquared;
    }

    public boolean ready(float cooldownThreshold) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        return player != null && player.getAttackCooldownProgress(0.5F) >= cooldownThreshold;
    }

    public boolean busy() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        return client == null
                || player == null
                || !player.isAlive()
                || player.isUsingItem()
                || client.currentScreen != null;
    }

    public void attack(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        LivingEntity target = this.cached;
        if (client == null
                || client.interactionManager == null
                || player == null
                || target == null
                || id != this.cachedId) {
            return;
        }
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
    }
}
