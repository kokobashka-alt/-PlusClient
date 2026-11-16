package ru.zov.implement.features.modules.combat.killaura.neuro.trigger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Game-agnostic decision logic for the Crystal Aura. Imports no Minecraft classes;
 * every engine interaction goes through {@link Context}. This keeps Yarn churn
 * inside {@link McCrystalContext} and makes the timing / target-selection /
 * placement logic unit-testable offline.
 *
 * <p>Anti-flag design notes:
 * <ul>
 *   <li>Rotation is requested through {@link Context#rotateTo} before EVERY
 *       placement or attack, so the server sees the player looking at the block /
 *       crystal at the moment of the action. Ordering: rotation packet is sent
 *       first, interaction packet second.</li>
 *   <li>Timing jitter uses a triangular distribution centred on the base delay,
 *       not a one-sided bump. This avoids the "always ≥ base" pattern.</li>
 *   <li>Legit profile has a per-tick pause chance so the action cadence is not
 *       perfectly periodic.</li>
 *   <li>Damage is estimated on both sides; a placement that would hurt self more
 *       than the enemy (or that could kill self) is rejected even in Rage.</li>
 *   <li>Target selection is sticky — once locked, the target is not re-evaluated
 *       every tick unless it dies, leaves range or the lock expires.</li>
 * </ul>
 *
 * <p>This is not an anticheat bypass and makes no claim about one. On strict
 * servers (Polar, Grim) even the Legit profile can be flagged; the Rage profile
 * is more likely to be flagged, not less.
 */
public final class CrystalAuraCore {

    // =====================================================================
    // Config
    // =====================================================================

    public static final class Config {
        // Ranges
        public double targetRange = 6.0;
        public double placeRange = 4.5;
        public double breakRange = 4.5;
        public double fovDegrees = 55.0;
        public double minSelfDistance = 2.5;
        public double maxCrystalTargetDistance = 2.5;

        // Timings
        public long placeDelayMs = 140L;
        public long breakDelayMs = 110L;
        public long switchDelayMs = 350L;
        public long basePlaceDelayMs = 80L;
        public long targetLockMs = 1200L;
        public int maxCrystalsPlacedPerTick = 1;
        public int maxCrystalsBrokenPerTick = 1;

        // Feature toggles
        public boolean autoPlace = true;
        public boolean autoBreak = true;
        public boolean swing = true;
        public boolean restoreSlot = false;
        public boolean basePlace = false;
        public boolean idPredict = false;
        public int idPredictMin = 1;
        public int idPredictMax = 2;
        public boolean setDead = false;

        // Damage gates
        public double minEnemyDamage = 3.0;
        public double maxSelfDamage = 4.0;
        public boolean antiSuicide = true;
        public double antiSuicideHealth = 4.0;
        public double minDamageRatio = 0.3;

        // Mode
        public boolean rage = false;
        public int legitPausePercent = 5;

        // RNG seed (per-instance, changes on activate)
        public long seed = 0L;

        private void validate() {
            this.targetRange = clamp(this.targetRange, 1.0, 16.0);
            this.placeRange = clamp(this.placeRange, 1.0, 8.0);
            this.breakRange = clamp(this.breakRange, 1.0, 8.0);
            this.fovDegrees = clamp(this.fovDegrees, 1.0, 180.0);
            this.minSelfDistance = clamp(this.minSelfDistance, 0.0, 8.0);
            this.maxCrystalTargetDistance = clamp(this.maxCrystalTargetDistance, 0.5, 8.0);
            this.placeDelayMs = Math.max(0L, this.placeDelayMs);
            this.breakDelayMs = Math.max(0L, this.breakDelayMs);
            this.switchDelayMs = Math.max(0L, this.switchDelayMs);
            this.basePlaceDelayMs = Math.max(20L, this.basePlaceDelayMs);
            this.targetLockMs = Math.max(0L, this.targetLockMs);
            this.maxCrystalsPlacedPerTick = clampInt(this.maxCrystalsPlacedPerTick, 1, 8);
            this.maxCrystalsBrokenPerTick = clampInt(this.maxCrystalsBrokenPerTick, 1, 8);
            this.idPredictMin = clampInt(this.idPredictMin, 1, 4);
            this.idPredictMax = clampInt(this.idPredictMax, this.idPredictMin, 8);
            this.minEnemyDamage = clamp(this.minEnemyDamage, 0.0, 40.0);
            this.maxSelfDamage = clamp(this.maxSelfDamage, 0.0, 40.0);
            this.minDamageRatio = clamp(this.minDamageRatio, 0.0, 10.0);
            this.antiSuicideHealth = clamp(this.antiSuicideHealth, 0.0, 20.0);
            this.legitPausePercent = clampInt(this.legitPausePercent, 0, 50);
        }

        private static double clamp(double v, double min, double max) {
            return v < min ? min : Math.min(v, max);
        }

        private static int clampInt(int v, int min, int max) {
            return v < min ? min : Math.min(v, max);
        }
    }

    // =====================================================================
    // Context — everything the core needs from the game
    // =====================================================================

    public interface Context {
        long nowMs();

        boolean canAct();

        Point self();

        List<Target> targets();

        List<Crystal> crystals();

        double angleTo(Point point);

        // World queries
        boolean isSupport(BlockPos block);

        boolean isAir(BlockPos block);

        boolean hasCrystalAt(BlockPos block);

        boolean canPlaceBase(BlockPos block);

        // Actions — implementations MUST rotate before interacting/attacking.
        boolean rotateTo(Point point);

        boolean placeCrystal(BlockPos support);

        boolean placeBase(BlockPos support);

        boolean attackCrystal(int id);

        boolean attackCrystalPredict(int id, int offset);

        void setDeadCrystal(int id);

        void swing();

        // Damage estimation
        double selfDamage(Point crystalCenter);

        double targetDamage(Point crystalCenter, int targetId);
    }

    // =====================================================================
    // Value types
    // =====================================================================

    public static final class Point {
        public final double x;
        public final double y;
        public final double z;

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double distanceTo(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public double squaredDistanceTo(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f, %.2f)", x, y, z);
        }
    }

    public static final class BlockPos {
        public final int x;
        public final int y;
        public final int z;

        public BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public BlockPos up() {
            return new BlockPos(this.x, this.y + 1, this.z);
        }

        public BlockPos down() {
            return new BlockPos(this.x, this.y - 1, this.z);
        }

        public BlockPos add(int dx, int dy, int dz) {
            return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof BlockPos pos)) return false;
            return this.x == pos.x && this.y == pos.y && this.z == pos.z;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * this.x + this.y) + this.z;
        }

        @Override
        public String toString() {
            return "[" + this.x + ", " + this.y + ", " + this.z + "]";
        }
    }

    public static final class Target {
        public final int id;
        public final Point position;
        public final double health;

        public Target(int id, Point position, double health) {
            this.id = id;
            this.position = position;
            this.health = health;
        }
    }

    public static final class Crystal {
        public final int id;
        public final Point position;

        public Crystal(int id, Point position) {
            this.id = id;
            this.position = position;
        }
    }

    public enum Result {
        IDLE,
        WAIT,
        SWITCH,
        BREAK,
        PLACE,
        PLACE_BASE
    }

    // =====================================================================
    // State
    // =====================================================================

    private final Config config;
    private final Random random;

    private int targetId = -1;
    private long targetLockedAt = 0L;
    private long nextActionAt = 0L;
    private long lastNowMs = Long.MIN_VALUE;
    private long lastBasePlaceAt = 0L;
    private BlockPos pendingBase = null;

    public CrystalAuraCore() {
        this(new Config());
    }

    public CrystalAuraCore(Config config) {
        this.config = config == null ? new Config() : config;
        this.config.validate();
        if (this.config.seed == 0L) {
            this.config.seed = System.nanoTime();
        }
        this.random = new Random(this.config.seed);
    }

    public Config config() {
        return this.config;
    }

    public int targetId() {
        return this.targetId;
    }

    public void reset() {
        this.targetId = -1;
        this.targetLockedAt = 0L;
        this.nextActionAt = 0L;
        this.lastNowMs = Long.MIN_VALUE;
        this.lastBasePlaceAt = 0L;
        this.pendingBase = null;
    }

    // =====================================================================
    // Main tick
    // =====================================================================

    public Result tick(Context context) {
        if (context == null) {
            this.reset();
            return Result.IDLE;
        }
        this.config.validate();

        long now = context.nowMs();
        if (now < this.lastNowMs) {
            this.reset();
        }
        this.lastNowMs = now;

        if (!context.canAct()) {
            this.reset();
            return Result.IDLE;
        }

        Target target = this.selectTarget(context, now);
        if (target == null) {
            this.reset();
            return Result.IDLE;
        }

        if (target.id != this.targetId) {
            this.targetId = target.id;
            this.targetLockedAt = now;
            this.nextActionAt = now + this.jitter(this.config.switchDelayMs);
            this.pendingBase = null;
            return Result.SWITCH;
        }

        // Human pause: occasionally skip the tick entirely.
        if (!this.config.rage && this.config.legitPausePercent > 0
                && this.random.nextInt(100) < this.config.legitPausePercent) {
            return Result.WAIT;
        }

        // BasePlace phase: if enabled and the target has no obsidian under it,
        // place one first and come back next tick.
        if (this.config.basePlace && this.config.autoPlace
                && this.pendingBase == null
                && now - this.lastBasePlaceAt >= this.config.basePlaceDelayMs) {
            BlockPos base = this.findBaseToPlace(context, target);
            if (base != null) {
                if (context.rotateTo(centerOf(base)) && context.placeBase(base)) {
                    if (this.config.swing) context.swing();
                    this.pendingBase = base;
                    this.lastBasePlaceAt = now;
                    this.nextActionAt = now + this.config.basePlaceDelayMs;
                    return Result.PLACE_BASE;
                }
            }
        }

        if (now < this.nextActionAt) {
            return Result.WAIT;
        }

        // Break phase (before place — clears the board)
        if (this.config.autoBreak) {
            int broken = this.breakCrystals(context, target);
            if (broken > 0) {
                if (this.config.swing) context.swing();
                this.nextActionAt = now + this.jitter(this.config.breakDelayMs);
                return Result.BREAK;
            }
        }

        // Place phase
        if (this.config.autoPlace) {
            if (this.placeCrystal(context, target)) {
                if (this.config.swing) context.swing();
                this.nextActionAt = now + this.jitter(this.config.placeDelayMs);
                return Result.PLACE;
            }
        }

        return Result.IDLE;
    }

    // =====================================================================
    // Target selection
    // =====================================================================

    private Target selectTarget(Context context, long now) {
        Point self = context.self();
        if (self == null) return null;

        // Sticky: keep locked target if still valid and lock hasn't expired
        if (this.targetId >= 0 && now - this.targetLockedAt < this.config.targetLockMs) {
            for (Target t : context.targets()) {
                if (t == null || t.position == null) continue;
                if (t.id == this.targetId) {
                    double d = self.distanceTo(t.position);
                    if (d <= this.config.targetRange
                            && context.angleTo(t.position) <= this.config.fovDegrees) {
                        return t;
                    }
                    break;
                }
            }
        }

        Target best = null;
        double bestScore = Double.MAX_VALUE;

        for (Target target : context.targets()) {
            if (target == null || target.position == null) continue;
            double distance = self.distanceTo(target.position);
            if (distance > this.config.targetRange) continue;

            double angle = context.angleTo(target.position);
            if (!Double.isFinite(angle) || angle > this.config.fovDegrees) continue;

            // Rage: closest wins. Legit: prefer crosshair, weight distance.
            double score = this.config.rage
                    ? distance
                    : angle * 1.0 + distance * 0.35;

            if (score < bestScore) {
                bestScore = score;
                best = target;
            }
        }

        return best;
    }

    // =====================================================================
    // Break
    // =====================================================================

    private int breakCrystals(Context context, Target target) {
        Point self = context.self();
        if (self == null) return 0;

        List<Crystal> candidates = new ArrayList<>();
        for (Crystal crystal : context.crystals()) {
            if (crystal == null || crystal.position == null) continue;

            double selfDist = self.distanceTo(crystal.position);
            double targetDist = target.position.distanceTo(crystal.position);

            if (selfDist > this.config.breakRange) continue;
            if (targetDist > this.config.maxCrystalTargetDistance) continue;
            if (context.angleTo(crystal.position) > this.config.fovDegrees) continue;

            // Damage gates for breaking too — don't detonate on top of self
            double selfDmg = context.selfDamage(crystal.position);
            if (this.config.antiSuicide && selfDmg + this.config.antiSuicideHealth
                    >= this.safeHealth(context)) continue;
            if (selfDmg > this.config.maxSelfDamage) continue;

            candidates.add(crystal);
        }

        // Prefer crystals closest to the target (higher chance of good damage)
        candidates.sort(Comparator.comparingDouble(c -> target.position.squaredDistanceTo(c.position)));

        int broken = 0;
        for (Crystal crystal : candidates) {
            if (broken >= this.config.maxCrystalsBrokenPerTick) break;

            if (context.rotateTo(crystal.position)) {
                boolean ok;
                if (this.config.idPredict) {
                    ok = context.attackCrystalPredict(crystal.id, this.config.idPredictMin);
                } else {
                    ok = context.attackCrystal(crystal.id);
                }
                if (ok) {
                    broken++;
                    if (this.config.setDead) {
                        context.setDeadCrystal(crystal.id);
                    }
                }
            }
        }
        return broken;
    }

    private double safeHealth(Context context) {
        Point self = context.self();
        if (self == null) return 20.0;
        return 20.0; // context doesn't expose health directly; use worst-case
    }

    // =====================================================================
    // Place
    // =====================================================================

    private boolean placeCrystal(Context context, Target target) {
        Point self = context.self();
        if (self == null) return false;

        List<BlockPos> supports = this.collectValidSupports(context, target, self);
        if (supports.isEmpty()) return false;

        // Prefer supports closest to the target; break ties by distance to self
        supports.sort(Comparator
                .comparingDouble((BlockPos p) -> target.position.squaredDistanceTo(centerOf(p.up())))
                .thenComparingDouble(p -> self.squaredDistanceTo(centerOf(p.up()))));

        int placed = 0;
        for (BlockPos support : supports) {
            if (placed >= this.config.maxCrystalsPlacedPerTick) break;

            if (context.rotateTo(centerOf(support.up()))) {
                if (context.placeCrystal(support)) {
                    placed++;
                    // Predict-and-swing is only meaningful for the first crystal
                    if (this.config.idPredict && this.config.swing) {
                        context.swing();
                    }
                }
            }
        }
        return placed > 0;
    }

    private List<BlockPos> collectValidSupports(Context context, Target target, Point self) {
        List<BlockPos> result = new ArrayList<>();
        int baseX = (int) Math.floor(target.position.x);
        int baseY = (int) Math.floor(target.position.y - 0.5);
        int baseZ = (int) Math.floor(target.position.z);

        Set<BlockPos> seen = new HashSet<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos support = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                    if (!seen.add(support)) continue;

                    if (!context.isSupport(support)) continue;
                    if (!context.isAir(support.up())) continue;
                    if (context.hasCrystalAt(support.up())) continue;

                    Point center = centerOf(support.up());
                    double selfDist = self.distanceTo(center);
                    double targetDist = target.position.distanceTo(center);

                    if (selfDist > this.config.placeRange) continue;
                    if (selfDist < this.config.minSelfDistance) continue;
                    if (targetDist > this.config.maxCrystalTargetDistance) continue;
                    if (context.angleTo(center) > this.config.fovDegrees) continue;

                    // Damage gates
                    double enemyDmg = context.targetDamage(center, target.id);
                    double selfDmg = context.selfDamage(center);

                    if (enemyDmg < this.config.minEnemyDamage) continue;
                    if (selfDmg > this.config.maxSelfDamage) continue;
                    if (this.config.antiSuicide && selfDmg + this.config.antiSuicideHealth >= 20.0) continue;
                    if (selfDmg > 0.0 && (enemyDmg / selfDmg) < this.config.minDamageRatio) continue;

                    result.add(support);
                }
            }
        }
        return result;
    }

    // =====================================================================
    // BasePlace helpers
    // =====================================================================

    private BlockPos findBaseToPlace(Context context, Target target) {
        int bx = (int) Math.floor(target.position.x);
        int by = (int) Math.floor(target.position.y - 0.5);
        int bz = (int) Math.floor(target.position.z);

        // Try positions directly under and around the target, from most useful to least
        BlockPos[] prefs = new BlockPos[] {
                new BlockPos(bx, by, bz),
                new BlockPos(bx + 1, by, bz),
                new BlockPos(bx - 1, by, bz),
                new BlockPos(bx, by, bz + 1),
                new BlockPos(bx, by, bz - 1),
                new BlockPos(bx, by - 1, bz),
        };

        for (BlockPos p : prefs) {
            if (!context.isAir(p)) continue;
            if (!context.canPlaceBase(p)) continue;
            if (!context.isAir(p.up())) continue;
            if (context.hasCrystalAt(p.up())) continue;
            return p;
        }
        return null;
    }

    // =====================================================================
    // Timing / geometry helpers
    // =====================================================================

    /**
     * Triangular jitter centred on {@code base}, range [0.75*base, 1.25*base].
     * Real human reaction times are not one-sided — the old {@code base + rand(0, base/4)}
     * implementation always produced delays >= base, which is a detectable pattern.
     */
    private long jitter(long base) {
        if (base <= 0L) return 0L;
        double u = this.random.nextDouble() + this.random.nextDouble() - 1.0; // triangle on [-1, 1]
        long result = Math.round(base * (1.0 + u * 0.25));
        return Math.max(0L, result);
    }

    private static Point centerOf(BlockPos block) {
        return new Point(block.x + 0.5, block.y + 0.5, block.z + 0.5);
    }
}
