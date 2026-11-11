package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Decides which {@link ShardProfile} is active and when it changes.
 *
 * Switch times are drawn from a triangular distribution between the configured minimum and maximum
 * number of minutes, and consecutive intervals are forced to differ, so the changes do not land on a
 * fixed period. A due switch waits for a gap in combat (up to {@link #DEFERRAL_CAP_MS}); after that
 * it happens anyway, but always through a smooth blend so no parameter jumps.
 *
 * Not thread-safe; it is driven from the client tick thread only.
 */
public final class ShardPatternScheduler {
    public static final long DEFERRAL_CAP_MS = 45_000L;
    public static final long EARLY_SWITCH_COOLDOWN_MS = 60_000L;
    private static final int HISTORY_LIMIT = 16;
    private static final float VARIANT_AMOUNT = 0.10F;

    public record SwitchEvent(long atMillis, String from, String to, boolean deferred) {
    }

    private final LongSupplier clock;
    private final Random rng;
    private final List<ShardProfile> catalog;
    private final Deque<Integer> recentIndices = new ArrayDeque<>();
    private final Deque<SwitchEvent> history = new ArrayDeque<>();

    private long minIntervalMs = 3L * 60_000L;
    private long maxIntervalMs = 9L * 60_000L;
    private long blendMs = 12_000L;

    private boolean started;
    private ShardProfile current;
    private ShardProfile previous;
    private int currentIndex = -1;
    private long nextSwitchAt;
    private long blendStartedAt;
    private long lastSwitchAt;
    private long lastIntervalMs;

    public ShardPatternScheduler(long seed, LongSupplier clock) {
        this(seed, clock, ShardProfile.CATALOG);
    }

    public ShardPatternScheduler(long seed, LongSupplier clock, List<ShardProfile> catalog) {
        if (catalog == null || catalog.size() < 2) {
            throw new IllegalArgumentException("catalog needs at least two profiles");
        }
        this.rng = new Random(seed);
        this.clock = clock;
        this.catalog = List.copyOf(catalog);
    }

    /** Interval bounds in minutes. Swapped or non-finite input is repaired, never rejected. */
    public void setIntervalMinutes(float minMinutes, float maxMinutes) {
        float lo = Float.isFinite(minMinutes) ? Math.max(0.05F, minMinutes) : 3.0F;
        float hi = Float.isFinite(maxMinutes) ? Math.max(0.05F, maxMinutes) : 9.0F;
        if (hi < lo) {
            float t = lo;
            lo = hi;
            hi = t;
        }
        this.minIntervalMs = (long) (lo * 60_000.0F);
        this.maxIntervalMs = (long) (hi * 60_000.0F);
    }

    public void setBlendMillis(long millis) {
        this.blendMs = Math.max(0L, millis);
    }

    public void reset() {
        started = false;
        current = null;
        previous = null;
        currentIndex = -1;
        recentIndices.clear();
        history.clear();
        lastIntervalMs = 0L;
    }

    /**
     * Returns the profile to use right now. {@code inCombat} is true while a target is being tracked;
     * it only affects when a due switch is allowed to happen.
     */
    public ShardProfile profileFor(boolean inCombat) {
        long now = clock.getAsLong();
        if (!started) {
            start(now);
        } else if (now >= nextSwitchAt) {
            boolean overdueTooLong = now - nextSwitchAt >= DEFERRAL_CAP_MS;
            if (!inCombat || overdueTooLong) {
                switchProfile(now, inCombat);
            }
        }
        if (previous != null) {
            long elapsed = now - blendStartedAt;
            if (blendMs <= 0L || elapsed >= blendMs) {
                previous = null;
            } else {
                float t = (float) elapsed / (float) blendMs;
                float eased = t * t * (3.0F - 2.0F * t);
                return ShardProfile.lerp(previous, current, eased);
            }
        }
        return current;
    }

    /** Pulls the next switch forward, at most once per {@link #EARLY_SWITCH_COOLDOWN_MS}. */
    public boolean requestEarlySwitch() {
        long now = clock.getAsLong();
        if (!started || now - lastSwitchAt < EARLY_SWITCH_COOLDOWN_MS) {
            return false;
        }
        nextSwitchAt = now;
        return true;
    }

    public String activeName() {
        return current == null ? "none" : current.name();
    }

    public long millisUntilSwitch() {
        return started ? Math.max(0L, nextSwitchAt - clock.getAsLong()) : -1L;
    }

    public List<SwitchEvent> history() {
        return new ArrayList<>(history);
    }

    private void start(long now) {
        started = true;
        currentIndex = rng.nextInt(catalog.size());
        current = catalog.get(currentIndex).mutate(rng, VARIANT_AMOUNT);
        previous = null;
        recentIndices.clear();
        recentIndices.addFirst(currentIndex);
        lastSwitchAt = now;
        nextSwitchAt = now + drawInterval();
    }

    private void switchProfile(long now, boolean wasInCombat) {
        int next = pickNextIndex();
        ShardProfile variant = catalog.get(next).mutate(rng, VARIANT_AMOUNT);
        String from = current.name();
        previous = current;
        current = variant;
        currentIndex = next;
        blendStartedAt = now;
        lastSwitchAt = now;
        nextSwitchAt = now + drawInterval();
        recentIndices.addFirst(next);
        int keep = Math.min(2, catalog.size() - 1);
        while (recentIndices.size() > keep) {
            recentIndices.removeLast();
        }
        history.addLast(new SwitchEvent(now, from, variant.name(), wasInCombat));
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    private int pickNextIndex() {
        List<Integer> allowed = new ArrayList<>();
        for (int i = 0; i < catalog.size(); i++) {
            if (!recentIndices.contains(i)) {
                allowed.add(i);
            }
        }
        if (allowed.isEmpty()) {
            for (int i = 0; i < catalog.size(); i++) {
                if (i != currentIndex) {
                    allowed.add(i);
                }
            }
        }
        return allowed.get(rng.nextInt(allowed.size()));
    }

    private long drawInterval() {
        long span = maxIntervalMs - minIntervalMs;
        if (span <= 0L) {
            lastIntervalMs = minIntervalMs;
            return minIntervalMs;
        }
        long minGap = span / 10L;
        long candidate = minIntervalMs;
        for (int attempt = 0; attempt < 6; attempt++) {
            double triangular = (rng.nextDouble() + rng.nextDouble()) * 0.5;
            candidate = minIntervalMs + (long) (triangular * span);
            if (lastIntervalMs == 0L || Math.abs(candidate - lastIntervalMs) >= minGap) {
                break;
            }
        }
        lastIntervalMs = candidate;
        return candidate;
    }
}
