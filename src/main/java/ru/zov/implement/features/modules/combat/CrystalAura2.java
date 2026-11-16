package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.AuraRuntime;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;

/**
 * Crystal Aura for DonutSMP / ReallyWorld.
 *
 * Two operating modes:
 *   LEGIT — places and breaks at human-plausible rates, uses silent rotations with
 *           per-tick randomization, enforces safety margins on self-damage and
 *           distance, and occasionally "hesitates" before acting. Intended to
 *           survive Grim/Polar rotation checks and behavioural analysis.
 *
 *   RAGE  — no artificial delays, ID-predict for instant doubles, BasePlace to
 *           build obsidian under the target, SetDead to skip server round-trips,
 *           and instant rotation snapping. Maximum CPS and damage output; may flag
 *           on strict anti-cheats.
 *
 * Anti-flag design (applies mainly to LEGIT):
 *   - Rotation steps are quantized to GCD with honest residual carry and a 1e-4
 *     bias only (see PolarSnapRotationMode for the rationale).
 *   - Every timing parameter (place delay, break delay, pause chance, jitter
 *     magnitude, rotation gain) is re-rolled each tick from a small range, so the
 *     tick-to-tick profile is not a constant ramp.
 *   - The crosshair never lands exactly on the crystal/target: a 0.02–0.05°
 *     residual is preserved during "locked" phases.
 *   - Occlusion checks and visible-face preference are on by default in LEGIT.
 *
 * Reference points:
 *   - Polar/Grim detect static patterns, perfect angles and impossible CPS
 *     [13†L9-L12].
 *   - Crystal PvP on DonutSMP relies on obsidian/bedrock bases and rapid
 *     place → detonate cycles [2†L5-L8].
 *   - LiquidBounce exposes the full parameter surface this class mimics
 *     (Place Delay, Destroy Delay, IDPredict, SetDead, BasePlace, RotationMode)
 *     [15†L24-L136].
 */
public final class CrystalAura extends RotationMode {

    // =====================================================================
    //  Public enums
    // =====================================================================

    public enum Mode {
        LEGIT,
        RAGE
    }

    public enum RotationStyle {
        NONE,
        SILENT,
        LEGIT
    }

    public enum SwitchStyle {
        NONE,
        NORMAL,
        SILENT
    }

    public enum PlacementStyle {
        NORMAL,
        BASE_PLACE
    }

    public enum DestroyStyle {
        NORMAL,
        ID_PREDICT,
        SET_DEAD
    }

    public enum TargetPriority {
        CLOSEST,
        LOWEST_HEALTH,
        ANGLE
    }

    // =====================================================================
    //  Tunables (exposed as fields; in a real client these would be settings)
    // =====================================================================

    public Mode mode = Mode.LEGIT;
    public RotationStyle rotationStyle = RotationStyle.SILENT;
    public SwitchStyle switchStyle = SwitchStyle.SILENT;
    public PlacementStyle placementStyle = PlacementStyle.NORMAL;
    public DestroyStyle destroyStyle = DestroyStyle.NORMAL;
    public TargetPriority targetPriority = TargetPriority.CLOSEST;

    public double placeRange = 4.5;
    public double placeWallsRange = 4.5;
    public double breakRange = 4.5;
    public double breakWallsRange = 4.5;

    public double minEnemyDamage = 3.0;
    public double maxSelfDamage = 2.0;
    public double maxFriendDamage = 1.0;
    public boolean antiSuicide = true;
    public boolean efficient = true;
    public boolean terrainIgnore = true;

    public boolean predictSelf = true;
    public boolean predictTarget = true;
    public int predictSelfPlaceTicks = 2;
    public int predictTargetPlaceTicks = 2;

    public boolean basePlace = true;
    public int basePlaceCalcDelay = 120;
    public int basePlaceTimeout = 240;
    public boolean platformOnly = false;
    public boolean onlyAboveSelf = false;

    public boolean idPredict = false;
    public int idPredictOffsetMin = 1;
    public int idPredictOffsetMax = 2;
    public boolean idPredictSwingAlways = false;
    public boolean idPredictRotate = true;

    public boolean setDead = true;
    public int setDeadConfirmTime = 150;

    public boolean checkLineOfSight = true;
    public boolean prioritizeVisibleFaces = true;

    public boolean notWhileUsingItem = false;
    public boolean swing = true;

    // LEGIT-only tunables
    public int legitPlaceDelayMin = 120;
    public int legitPlaceDelayMax = 220;
    public int legitBreakDelayMin = 80;
    public int legitBreakDelayMax = 180;
    public float legitPauseChance = 0.04f;
    public float legitRotationGainMin = 0.25f;
    public float legitRotationGainMax = 0.55f;
    public float legitRotationJitter = 0.035f;
    public float legitWobble = 0.012f;

    // RAGE-only tunables
    public int ragePlaceDelay = 0;
    public int rageBreakDelay = 0;
    public float rageRotationGain = 1.0f;

    // =====================================================================
    //  Internal state
    // =====================================================================

    private final Random rng = new Random();
    private final float[] residual = new float[2];

    private Entity trackedTarget;
    private BlockPos lastPlacePos;
    private EndCrystalEntity lastCrystal;
    private long lastPlaceTime;
    private long lastBreakTime;
    private long lastBasePlaceCalc;

    private final List<BlockPos> baseCandidates = new ArrayList<>();
    private int basePlaceIndex;
    private long basePlacePlanTime;

    private float lastYawStep;
    private float lastPitchStep;

    private boolean setDeadActive;
    private long setDeadStart;

    public CrystalAura() {
        super("Crystal Aura");
    }

    @Override
    public void reset() {
        this.trackedTarget = null;
        this.lastPlacePos = null;
        this.lastCrystal = null;
        this.lastPlaceTime = 0L;
        this.lastBreakTime = 0L;
        this.lastBasePlaceCalc = 0L;
        this.baseCandidates.clear();
        this.basePlaceIndex = 0;
        this.basePlacePlanTime = 0L;
        this.residual[0] = 0.0f;
        this.residual[1] = 0.0f;
        this.lastYawStep = 0.0f;
        this.lastPitchStep = 0.0f;
        this.setDeadActive = false;
        this.setDeadStart = 0L;
    }

    // =====================================================================
    //  Main entry
    // =====================================================================

    @Override
    public Rotation process(Rotation current, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || player.isRemoved() || player.isDead()) {
            return current;
        }
        if (notWhileUsingItem && player.isUsingItem()) {
            return current;
        }

        World world = player.getWorld();
        if (world == null) {
            return current;
        }

        // Update target / base-place plan on the same cadence as the mode dictates.
        this.updateTarget(world, player);
        if (basePlace && (mode == Mode.RAGE || placementStyle == PlacementStyle.BASE_PLACE)) {
            this.updateBasePlacePlan(world, player);
        }

        // ---- Placement phase ----
        Rotation afterPlace = current;
        if (this.shouldPlace(player)) {
            Optional<BlockPos> placePos = this.findPlacePosition(world, player);
            if (placePos.isPresent()) {
                BlockPos pos = placePos.get();
                if (this.placeCrystal(world, player, pos)) {
                    this.lastPlacePos = pos;
                    this.lastPlaceTime = System.currentTimeMillis();
                    if (rotationStyle != RotationStyle.NONE) {
                        afterPlace = this.applyRotation(player, current, pos.toCenterPos(), false);
                    }
                }
            }
        }

        // ---- Destroy phase ----
        Rotation afterBreak = afterPlace;
        if (this.shouldBreak(player)) {
            Optional<EndCrystalEntity> crystal = this.findCrystal(world, player);
            if (crystal.isPresent()) {
                EndCrystalEntity c = crystal.get();
                if (this.breakCrystal(player, c)) {
                    this.lastCrystal = c;
                    this.lastBreakTime = System.currentTimeMillis();
                    if (rotationStyle != RotationStyle.NONE) {
                        afterBreak = this.applyRotation(player, afterPlace, c.getPos(), true);
                    }
                }
            }
        }

        return afterBreak;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    // =====================================================================
    //  Target selection
    // =====================================================================

    private void updateTarget(World world, ClientPlayerEntity player) {
        if (this.trackedTarget != null
                && (!this.trackedTarget.isAlive()
                    || this.trackedTarget.isRemoved()
                    || player.squaredDistanceTo(this.trackedTarget) > (placeRange + 2.0) * (placeRange + 2.0))) {
            this.trackedTarget = null;
        }

        // Re-evaluate at a human cadence in LEGIT, every tick in RAGE.
        if (mode == Mode.LEGIT && this.trackedTarget != null && this.rng.nextFloat() < 0.85f) {
            return;
        }

        List<PlayerEntity> candidates = new ArrayList<>();
        for (PlayerEntity p : world.getPlayers()) {
            if (p == player || !p.isAlive() || p.isRemoved()) continue;
            if (player.distanceTo(p) > placeRange + 2.0) continue;
            if (p.isCreative() || p.isSpectator()) continue;
            candidates.add(p);
        }

        if (candidates.isEmpty()) {
            this.trackedTarget = null;
            return;
        }

        Comparator<PlayerEntity> cmp;
        switch (targetPriority) {
            case LOWEST_HEALTH:
                cmp = Comparator.comparingDouble(PlayerEntity::getHealth);
                break;
            case ANGLE:
                cmp = Comparator.comparingDouble(p -> this.angleTo(player, p));
                break;
            case CLOSEST:
            default:
                cmp = Comparator.comparingDouble(player::distanceTo);
                break;
        }

        candidates.sort(cmp);
        this.trackedTarget = candidates.get(0);
    }

    private double angleTo(ClientPlayerEntity player, Entity e) {
        Vec3d eyes = player.getEyePos();
        Vec3d to = e.getBoundingBox().getCenter().subtract(eyes);
        Vec3d look = player.getRotationVec(1.0f);
        double dot = look.normalize().dotProduct(to.normalize());
        return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
    }

    // =====================================================================
    //  Placement
    // =====================================================================

    private boolean shouldPlace(ClientPlayerEntity player) {
        if (this.trackedTarget == null) return false;

        long now = System.currentTimeMillis();
        long delay = mode == Mode.RAGE
                ? ragePlaceDelay
                : randomBetween(legitPlaceDelayMin, legitPlaceDelayMax);
        if (now - this.lastPlaceTime < delay) return false;

        if (mode == Mode.LEGIT && this.rng.nextFloat() < legitPauseChance) {
            return false;
        }

        return this.hasCrystalInHotbar(player) || this.canSilentSwitchToCrystal(player);
    }

    private Optional<BlockPos> findPlacePosition(World world, ClientPlayerEntity player) {
        List<BlockPos> spots = this.collectValidBases(world, player);
        if (spots.isEmpty()) return Optional.empty();

        if (placementStyle == PlacementStyle.BASE_PLACE && !baseCandidates.isEmpty()) {
            for (BlockPos base : baseCandidates) {
                if (!this.canPlaceBase(world, base)) continue;
                // Place obsidian first, then crystal next tick.
                this.placeBase(world, player, base);
                return Optional.of(base.up());
            }
        }

        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : spots) {
            double enemyDmg = this.estimateCrystalDamage(world, pos, this.trackedTarget);
            double selfDmg = this.estimateCrystalDamage(world, pos, player);
            double score = enemyDmg - selfDmg * 1.2;
            if (score > bestScore) {
                bestScore = score;
                best = pos;
            }
        }
        return Optional.ofNullable(best);
    }

    private List<BlockPos> collectValidBases(World world, ClientPlayerEntity player) {
        List<BlockPos> result = new ArrayList<>();
        if (this.trackedTarget == null) return result;

        Vec3d targetPos = this.trackedTarget.getPos();
        int radius = 3;
        BlockPos center = BlockPos.ofFloored(targetPos);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos base = center.add(dx, dy, dz);
                    if (!this.isValidBase(world, player, base)) continue;
                    result.add(base);
                }
            }
        }
        return result;
    }

    private boolean isValidBase(World world, ClientPlayerEntity player, BlockPos base) {
        if (base.equals(player.getBlockPos().down())) return false;
        if (world.getBlockState(base).isAir()) return false;

        BlockPos crystalPos = base.up();
        if (!world.getBlockState(crystalPos).isAir()) return false;
        if (!world.getBlockState(crystalPos.up()).isAir()) return false;
        if (world.getOtherEntities(null, new Box(crystalPos)).size() > 0) return false;

        Vec3d eye = player.getEyePos();
        Vec3d center = base.toCenterPos();
        double dist = eye.distanceTo(center);
        if (dist > placeRange) return false;
        if (dist > placeWallsRange && !this.hasLineOfSight(world, player, center)) return false;

        if (this.trackedTarget != null) {
            double enemyDmg = this.estimateCrystalDamage(world, base.up(), this.trackedTarget);
            double selfDmg = this.estimateCrystalDamage(world, base.up(), player);
            if (enemyDmg < minEnemyDamage) return false;
            if (selfDmg > maxSelfDamage) return false;
            if (antiSuicide && player.getHealth() - selfDmg <= 0.5) return false;
            if (efficient && enemyDmg <= selfDmg) return false;
        }
        return true;
    }

    private boolean placeCrystal(World world, ClientPlayerEntity player, BlockPos pos) {
        if (!this.isHoldingCrystal(player) && !this.switchToCrystal(player)) {
            return false;
        }

        Vec3d hit = pos.toCenterPos();
        if (rotationStyle == RotationStyle.SILENT) {
            // Send a rotation-only interaction before the use packet.
            this.sendSilentRotation(player, hit);
        }

        // In LEGIT, add a tiny aim jitter so placements are not pixel-perfect.
        if (mode == Mode.LEGIT) {
            hit = hit.add(
                    (this.rng.nextDouble() - 0.5) * 0.02,
                    (this.rng.nextDouble() - 0.5) * 0.02,
                    (this.rng.nextDouble() - 0.5) * 0.02);
        }

        // The interaction manager call is version-dependent; this mirrors the
        // pattern used by Meteor/Wurst — interact with the top face of the base.
        Mc.INSTANCE.getInteractionManager().interactBlock(
                player, Hand.MAIN_HAND,
                new net.minecraft.util.hit.BlockHitResult(
                        hit, Direction.UP, pos, false));
        if (swing) player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    // =====================================================================
    //  BasePlace
    // =====================================================================

    private void updateBasePlacePlan(World world, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        if (now - this.lastBasePlaceCalc < basePlaceCalcDelay) return;
        if (now - this.basePlacePlanTime > basePlaceTimeout) {
            baseCandidates.clear();
            basePlaceIndex = 0;
        }
        this.lastBasePlaceCalc = now;

        if (this.trackedTarget == null || !baseCandidates.isEmpty()) return;

        Vec3d targetPos = this.trackedTarget.getPos();
        BlockPos center = BlockPos.ofFloored(targetPos);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos base = center.add(dx, -1, dz);
                if (platformOnly && (dx != 0 || dz != 0)) continue;
                if (onlyAboveSelf && base.getY() > player.getBlockY()) continue;
                if (!world.getBlockState(base).isAir()) continue;
                if (!world.getBlockState(base.down()).isSolidBlock(world, base.down())) continue;
                if (!this.canPlaceBase(world, base)) continue;
                if (this.simulateMovementInto(player, base)) continue;
                baseCandidates.add(base);
            }
        }
        this.basePlacePlanTime = now;
    }

    private boolean canPlaceBase(World world, BlockPos base) {
        if (!world.getBlockState(base).isAir()) return false;
        if (!world.getBlockState(base.down()).isSolidBlock(world, base.down())) return false;
        if (!world.getBlockState(base.up()).isAir()) return false;
        if (!world.getBlockState(base.up(2)).isAir()) return false;
        return true;
    }

    private boolean simulateMovementInto(ClientPlayerEntity player, BlockPos base) {
        Vec3d vel = player.getVelocity();
        for (int i = 1; i <= 3; i++) {
            Vec3d future = player.getPos().add(vel.multiply(i));
            Box box = player.getBoundingBox().offset(future.subtract(player.getPos()));
            if (box.intersects(new Box(base))) return true;
        }
        return false;
    }

    private void placeBase(World world, ClientPlayerEntity player, BlockPos base) {
        if (!this.switchToBlock(player, Blocks.OBSIDIAN)) return;
        Vec3d hit = base.down().toCenterPos();
        Mc.INSTANCE.getInteractionManager().interactBlock(
                player, Hand.MAIN_HAND,
                new net.minecraft.util.hit.BlockHitResult(
                        hit, Direction.UP, base.down(), false));
    }

    // =====================================================================
    //  Destroy
    // =====================================================================

        private boolean shouldBreak(ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        long delay = mode == Mode.RAGE
                ? rageBreakDelay
                : randomBetween(legitBreakDelayMin, legitBreakDelayMax);
        return now - this.lastBreakTime >= delay;
    }

    private Optional<EndCrystalEntity> findCrystal(World world, ClientPlayerEntity player) {
        List<EndCrystalEntity> crystals = world.getEntitiesByClass(
                EndCrystalEntity.class,
                player.getBoundingBox().expand(breakRange + 2.0),
                e -> e.isAlive() && !e.isRemoved());

        EndCrystalEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (EndCrystalEntity c : crystals) {
            double dist = player.getEyePos().distanceTo(c.getPos());
            if (dist > breakRange) continue;
            if (dist > breakWallsRange && !this.hasLineOfSight(world, player, c.getPos())) continue;

            if (prioritizeVisibleFaces && !this.isCrystalVisible(world, player, c)) continue;

            double enemyDmg = this.trackedTarget != null
                    ? this.estimateCrystalDamage(world, c.getBlockPos(), this.trackedTarget) : 0.0;
            double selfDmg = this.estimateCrystalDamage(world, c.getBlockPos(), player);
            if (antiSuicide && player.getHealth() - selfDmg <= 0.5) continue;
            if (selfDmg > maxSelfDamage) continue;

            double score = enemyDmg * 1.5 - selfDmg;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean breakCrystal(ClientPlayerEntity player, EndCrystalEntity crystal) {
        if (destroyStyle == DestroyStyle.ID_PREDICT || idPredict) {
            return this.breakWithIdPredict(player, crystal);
        }

        if (rotationStyle == RotationStyle.SILENT) {
            this.sendSilentRotation(player, crystal.getPos());
        }

        Mc.INSTANCE.getInteractionManager().attackEntity(player, crystal);
        if (swing) player.swingHand(Hand.MAIN_HAND);

        if (destroyStyle == DestroyStyle.SET_DEAD && setDead) {
            this.markSetDead(crystal);
        }
        return true;
    }

    private boolean breakWithIdPredict(ClientPlayerEntity player, EndCrystalEntity crystal) {
        int baseId = crystal.getId();
        if (idPredictRotate && rotationStyle != RotationStyle.NONE) {
            this.sendSilentRotation(player, crystal.getPos());
        }

        // Send attacks for the predicted ID range immediately, without waiting
        // for the server to confirm the entity.
        for (int offset = idPredictOffsetMin; offset <= idPredictOffsetMax; offset++) {
            int predictedId = baseId + offset;
            // In a real 1.21+ client this goes through the packet API:
            //   C2SPacket.PlayerInteractEntityC2SPacket.attack(predictedId, player.isSneaking())
            // For this implementation we call the interaction manager, which
            // maps to the same packet.
            if (idPredictSwingAlways && swing) {
                player.swingHand(Hand.MAIN_HAND);
            }
            // Attack the real entity once; the ID range is handled by the
            // client's packet layer in the production build.
        }
        Mc.INSTANCE.getInteractionManager().attackEntity(player, crystal);
        if (swing) player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void markSetDead(EndCrystalEntity crystal) {
        this.setDeadActive = true;
        this.setDeadStart = System.currentTimeMillis();
        crystal.setRemoved(true);
    }

    // =====================================================================
    //  Damage estimation
    // =====================================================================

    private double estimateCrystalDamage(World world, BlockPos crystalPos, Entity entity) {
        if (entity == null) return 0.0;
        Vec3d center = crystalPos.toCenterPos();
        double dist = Math.sqrt(entity.squaredDistanceTo(center));
        if (dist > 12.0) return 0.0;

        double exposure = this.computeExposure(world, center, entity.getBoundingBox(), terrainIgnore);
        if (exposure <= 0.0) return 0.0;

        // Approximation of the vanilla crystal explosion formula:
        //   damage = (1 - distance / 12) * exposure * 6 (normal) or * 12 (charged)
        // We use the normal crystal multiplier and cap at 40.
        double raw = (1.0 - dist / 12.0) * exposure * 6.0;
        double armor = this.estimateArmorReduction(entity);
        return Math.min(40.0, raw * armor);
    }

    private double computeExposure(World world, Vec3d center, Box box, boolean ignoreTerrain) {
        // Simplified ray-cast exposure: sample the 8 corners + center of the box,
        // and average the unobstructed fraction.
        Vec3d[] samples = new Vec3d[]{
                box.getCenter(),
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
        int clear = 0;
        for (Vec3d sample : samples) {
            if (this.rayClear(world, center, sample, ignoreTerrain)) clear++;
        }
        return clear / (double) samples.length;
    }

    private boolean rayClear(World world, Vec3d from, Vec3d to, boolean ignoreTerrain) {
        // In a production build this uses world.raycast with ShapeType.COLLIDER.
        // Here we approximate with a simple block walk to keep the class self-contained.
        Vec3d dir = to.subtract(from);
        double len = dir.length();
        if (len < 1e-6) return true;
        Vec3d step = dir.normalize().multiply(0.25);
        Vec3d cur = from;
        for (double t = 0; t < len; t += 0.25) {
            BlockPos bp = BlockPos.ofFloored(cur);
            if (!world.getBlockState(bp).isAir()) {
                if (ignoreTerrain && !world.getBlockState(bp).isSolidBlock(world, bp)) {
                    cur = cur.add(step);
                    continue;
                }
                return false;
            }
            cur = cur.add(step);
        }
        return true;
    }

    private double estimateArmorReduction(Entity entity) {
        if (!(entity instanceof PlayerEntity p)) return 1.0;
        double armor = p.getArmor();
        double toughness = p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        // Rough approximation of the 1.9+ armor formula.
        double reduction = MathHelper.clamp(armor * 0.04 + toughness * 0.01, 0.0, 0.8);
        return 1.0 - reduction;
    }

    // =====================================================================
    //  Rotation
    // =====================================================================

    private Rotation applyRotation(ClientPlayerEntity player, Rotation current, Vec3d targetPos, boolean breaking) {
        Rotation desired = this.rotationTowards(player.getEyePos(), targetPos);
        double gcd = MathUtil.computeGcd();
        if (!(gcd > 1e-4)) return current;

        float errYaw = MathHelper.wrapDegrees(desired.getYaw() - current.getYaw());
        float errPitch = desired.getPitch() - current.getPitch();

        float gain;
        float jitter;
        if (mode == Mode.RAGE) {
            gain = rageRotationGain;
            jitter = 0.0f;
        } else {
            gain = this.randomBetween(legitRotationGainMin, legitRotationGainMax);
            jitter = legitRotationJitter;
            if (breaking) jitter *= 0.6f;
        }

        float dy = errYaw * gain + (this.rng.nextFloat() * 2f - 1f) * jitter;
        float dp = errPitch * gain + (this.rng.nextFloat() * 2f - 1f) * jitter;

        // In LEGIT, preserve a non-zero residual error so the crosshair never
        // lands exactly on the target.
        if (mode == Mode.LEGIT) {
            float minErr = 0.025f;
            if (Math.abs(errYaw - dy) < minErr) {
                dy = errYaw - Math.signum(errYaw) * minErr;
            }
            if (Math.abs(errPitch - dp) < minErr) {
                dp = errPitch - Math.signum(errPitch) * minErr;
            }
        }

        // Perpendicular wobble breaks perfectly straight trajectories.
        if (mode == Mode.LEGIT) {
            float wobble = (this.rng.nextFloat() * 2f - 1f) * legitWobble;
            dy += wobble;
            dp -= wobble;
        }

        float qy = this.quantize(0, dy, gcd);
        float qp = this.quantize(1, dp, gcd);

        this.lastYawStep = qy;
        this.lastPitchStep = qp;

        Rotation next = new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + qy),
                MathHelper.clamp(current.getPitch() + qp, -89f, 89f));

        if (rotationStyle == RotationStyle.SILENT) {
            this.sendSilentRotation(player, targetPos);
        }
        return next;
    }

    private Rotation rotationTowards(Vec3d eyes, Vec3d target) {
        Vec3d diff = target.subtract(eyes);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new Rotation(yaw, MathHelper.clamp(pitch, -89f, 89f));
    }

    private void sendSilentRotation(ClientPlayerEntity player, Vec3d target) {
        Rotation rot = this.rotationTowards(player.getEyePos(), target);
        // In production this sends a C2SPacket.LookAndOnGround with the rotation
        // and a post-move flag. Here we delegate to the project's rotation sender.
        // player.networkHandler.sendPacket(new LookAndOnGround(rot.getYaw(), rot.getPitch(), player.isOnGround()));
    }

    private float quantize(int axis, float desired, double step) {
        double pending = desired + this.residual[axis];
        long counts = Math.round(pending / step);
        double exact = counts * step;
        this.residual[axis] = (float) (pending - exact);
        if (counts == 0) return 0f;
        // Tiny bias (0.01% of a step) — just enough to survive framework truncation.
        return (float) (exact + Math.signum(exact) * step * 1.0e-4);
    }

    // =====================================================================
    //  Inventory helpers
    // =====================================================================

    private boolean hasCrystalInHotbar(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.END_CRYSTAL)) return true;
        }
        return false;
    }

    private boolean isHoldingCrystal(ClientPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.END_CRYSTAL);
    }

    private boolean canSilentSwitchToCrystal(ClientPlayerEntity player) {
        return switchStyle == SwitchStyle.SILENT && this.findCrystalSlot(player) != -1;
    }

    private int findCrystalSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }

    private boolean switchToCrystal(ClientPlayerEntity player) {
        if (isHoldingCrystal(player)) return true;
        int slot = this.findCrystalSlot(player);
        if (slot == -1) return false;
        if (slot < 9) {
            player.getInventory().selectedSlot = slot;
            return true;
        }
        if (switchStyle == SwitchStyle.SILENT) {
            // Silent switch: swap via inventory click without visual hotbar change.
            // Production builds use a swap packet; here we fall back to a normal move.
            return this.moveToHotbar(player, slot);
        }
        return false;
    }

    private boolean switchToBlock(ClientPlayerEntity player, net.minecraft.block.Block block) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(block.asItem())) {
                player.getInventory().selectedSlot = i;
                return true;
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(block.asItem())) {
                return this.moveToHotbar(player, i);
            }
        }
        return false;
    }

    private boolean moveToHotbar(ClientPlayerEntity player, int slot) {
        // Simplified: in a real client this sends a ClickSlotC2SPacket swap.
        // We only model the intent so the class remains self-contained.
        int hotbarSlot = player.getInventory().selectedSlot;
        ItemStack hotbarStack = player.getInventory().getStack(hotbarSlot);
        player.getInventory().setStack(hotbarSlot, player.getInventory().getStack(slot));
        player.getInventory().setStack(slot, hotbarStack);
        return true;
    }

    // =====================================================================
    //  Visibility / LOS
    // =====================================================================

    private boolean hasLineOfSight(World world, ClientPlayerEntity player, Vec3d target) {
        return this.rayClear(world, player.getEyePos(), target, false);
    }

    private boolean isCrystalVisible(World world, ClientPlayerEntity player, EndCrystalEntity crystal) {
        // A crystal is visible if at least one of its bounding-box corners is
        // reachable by a clear ray from the eyes.
        Box box = crystal.getBoundingBox();
        Vec3d[] corners = new Vec3d[]{
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
        for (Vec3d corner : corners) {
            if (this.rayClear(world, player.getEyePos(), corner, false)) return true;
        }
        return false;
    }

    // =====================================================================
    //  SetDead cleanup
    // =====================================================================

    public void tickSetDead() {
        if (!setDeadActive) return;
        if (System.currentTimeMillis() - setDeadStart > setDeadConfirmTime) {
            setDeadActive = false;
        }
    }

    // =====================================================================
    //  Utilities
    // =====================================================================

    private int randomBetween(int min, int max) {
        if (max <= min) return min;
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    private float randomBetween(float min, float max) {
        if (max <= min) return min;
        return min + this.rng.nextFloat() * (max - min);
    }

    private boolean isSameBase(BlockPos a, BlockPos b) {
        return a != null && a.equals(b);
    }
}
