package ru.zov.implement.features.modules.combat.killaura.trigger;

import java.util.Random;

/**
 * A deliberately boring, "legit" triggerbot.
 *
 * <p>It never rotates the view and never picks a target itself: it only clicks when the vanilla crosshair already
 * points at an entity that the adapter reports as alive, in reach and visible. On top of that it waits a randomized
 * reaction time after the target appears, respects the attack cooldown, caps clicks per second, occasionally skips a
 * window and occasionally takes a short break.
 *
 * <p>The core is game-agnostic and tested with a fake {@link Context}. All Minecraft calls live in
 * {@link McTriggerContext}; adjust the mappings there if your client differs.
 *
 * <p>This does not bypass any anticheat and makes no claim about one. Measure it on your server.
 */
public final class TriggerBot {
    /** Times are milliseconds, chances are 0..1. */
    public static final class Config {
        /** Randomized reaction after the crosshair first lands on a valid entity. */
        public long reactionMinMs = 90L;
        public long reactionMaxMs = 180L;

        /** Upper bound on click rate; the weapon cooldown is usually the real limit. */
        public long attackIntervalMinMs = 105L;
        public long attackIntervalMaxMs = 165L;

        /** Only click when the attack cooldown is nearly full, with a little random jitter. */
        public float cooldownThreshold = 0.92F;
        public float cooldownJitter = 0.02F;

        /** Chance to take the current cooldown window; below 1.0 this creates honest misses. */
        public float attackChance = 0.96F;

        /** After a successful click, rare chance to pause for a moment like a distracted human. */
        public float pauseChance = 0.02F;
        public long pauseMinMs = 200L;
        public long pauseMaxMs = 450L;

        public long seed = 20261115L;

        private void validate() {
            this.reactionMinMs = Math.max(0L, this.reactionMinMs);
            this.reactionMaxMs = Math.max(this.reactionMinMs, this.reactionMaxMs);
            this.attackIntervalMinMs = Math.max(0L, this.attackIntervalMinMs);
            this.attackIntervalMaxMs = Math.max(this.attackIntervalMinMs, this.attackIntervalMaxMs);
            this.cooldownThreshold = clamp(this.cooldownThreshold, 0.0F, 1.0F);
            this.cooldownJitter = clamp(this.cooldownJitter, 0.0F, 0.5F);
            this.attackChance = clamp(this.attackChance, 0.0F, 1.0F);
            this.pauseChance = clamp(this.pauseChance, 0.0F, 1.0F);
            this.pauseMinMs = Math.max(0L, this.pauseMinMs);
            this.pauseMaxMs = Math.max(this.pauseMinMs, this.pauseMaxMs);
        }

        private static float clamp(float value, float min, float max) {
            return value < min ? min : (value > max ? max : value);
        }
    }

    /** Everything the core needs from the game. Implement it once for your client. */
    public interface Context {
        /** Entity id under the vanilla crosshair, or -1 when the crosshair is not on an entity. */
        int targetId();

        /** The same entity is still alive, within reach and visible (no wall between eye and hitbox). */
        boolean valid(int id);

        /** The attack cooldown progress is at least {@code cooldownThreshold}. */
        boolean ready(float cooldownThreshold);

        /** A screen is open, the player is using an item, is dead, etc. No clicking while true. */
        boolean busy();

        /** Perform a normal main-hand attack on the entity with this id. */
        void attack(int id);
    }

    private final Config config;
    private final Random random;

    private int targetId = -1;
    private long nextAttackAt;
    private long pauseUntil;
    private long lastNowMs;

    public TriggerBot() {
        this(new Config());
    }

    public TriggerBot(Config config) {
        this.config = config == null ? new Config() : config;
        this.config.validate();
        this.random = new Random(this.config.seed);
    }

    /** Live config; changes are picked up on the next tick. */
    public Config config() {
        return this.config;
    }

    /**
     * Call once per client tick.
     *
     * @return true if this tick performed an attack
     */
    public boolean tick(long nowMs, Context context) {
        if (context == null) {
            this.reset();
            return false;
        }
        this.config.validate();
        if (nowMs < this.lastNowMs) {
            // The clock jumped back (world change, pause menu); forget pending timers.
            this.reset();
        }
        this.lastNowMs = nowMs;

        if (context.busy()) {
            this.reset();
            return false;
        }

        int id = context.targetId();
        if (id < 0 || !context.valid(id)) {
            this.reset();
            return false;
        }

        if (id != this.targetId) {
            this.targetId = id;
            this.nextAttackAt = nowMs + this.randomBetween(this.config.reactionMinMs, this.config.reactionMaxMs);
            this.pauseUntil = 0L;
        }

        if (nowMs < this.pauseUntil || nowMs < this.nextAttackAt) {
            return false;
        }

        float threshold = clamp(this.config.cooldownThreshold
                + (float) this.random.nextGaussian() * this.config.cooldownJitter, 0.0F, 1.0F);
        if (!context.ready(threshold)) {
            return false;
        }

        if (this.random.nextFloat() >= this.config.attackChance) {
            // Skip this window on purpose. Look again shortly; the cooldown may still not be ready.
            this.nextAttackAt = nowMs + this.randomBetween(55L, 130L);
            return false;
        }

        context.attack(id);
        this.nextAttackAt = nowMs + this.randomBetween(
                this.config.attackIntervalMinMs, this.config.attackIntervalMaxMs);
        if (this.config.pauseChance > 0.0F && this.random.nextFloat() < this.config.pauseChance) {
            this.pauseUntil = nowMs + this.randomBetween(this.config.pauseMinMs, this.config.pauseMaxMs);
        }
        return true;
    }

    /** Forget the target and all pending timers. Safe to call when the module is disabled. */
    public void reset() {
        this.targetId = -1;
        this.nextAttackAt = 0L;
        this.pauseUntil = 0L;
    }

    public int targetId() {
        return this.targetId;
    }

    private long randomBetween(long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + this.random.nextLong(max - min + 1L);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
