package ru.zov.implement.features.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.rotation.Angle;
import ru.zov.implement.events.player.TickEvent;

public class TargetPearl extends Module {

    private static final int MAX_SIMULATION_TICKS = 160;
    private static final double MAX_TARGET_ERROR = 3.0;
    private static final float PEARL_SPEED = 1.5f;
    private static final double PEARL_GRAVITY = 0.03;
    private static final double PEARL_DRAG = 0.99;
    private static final double MIN_FOLLOW_OFFSET = 1.5;
    private static final int SAFE_SEARCH_RADIUS = 4;
    private static final int MAX_SAFE_TARGETS = 4;

    private final ValueSetting distance = new ValueSetting(
    "Мин. дистанция", "Минимальная дальность приземления")
    .setValue(10.0f)
    .range(8.0f, 20.0f);

    private long lastThrow;
    private long lastScan;

    public TargetPearl() {
        super("TargetPearl",
              "Бросает жемчуг к прогнозируемой точке приземления чужого жемчуга",
              ModuleCategory.COMBAT);
        this.setup(new Setting[]{ ignoreFriends, distance });
    }

    public static TargetPearl getInstance() {
        return (TargetPearl) Instance.get(TargetPearl.class);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        long now = System.currentTimeMillis();
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isGliding()) return;
        if (now - lastThrow < 2000L || now - lastScan < 70L) return;
        if (mc.player.getItemCooldownManager()
                .isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) return;

        Slot pearlSlot = findPearlSlot();
        if (pearlSlot == null) return;

        lastScan = now;

        Vec3d bestLanding = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) continue;
            if (pearl.getOwner() == mc.player) continue;
            if (ignoreFriends.isValue()
                && pearl.getOwner() != null
                && FriendUtils.isFriend(pearl.getOwner())) continue;

            Vec3d landing = predictLanding(pearl);
            if (landing == null) continue;

            double d = mc.player.getPos().distanceTo(landing);
            if (d >= distance.getValue() && d <= 120.0 && d < bestDistance) {
                bestDistance = d;
                bestLanding = landing;
            }
        }

        if (bestLanding == null) return;

        ThrowSolution solution = findBestThrowSolution(bestLanding);
        if (solution == null || solution.error() > MAX_TARGET_ERROR) return;

        float cameraYaw = mc.player.getYaw();
        float cameraPitch = mc.player.getPitch();
        boolean onGround = mc.player.isOnGround();
        boolean horizontalCollision = mc.player.horizontalCollision;

        mc.getNetworkHandler().sendPacket(
            new LookAndOnGround(solution.yaw(), solution.pitch(),
                                onGround, horizontalCollision));

        PlayerInventoryUtil.swapAndUse(
            pearlSlot, new Angle(solution.yaw(), solution.pitch()), true);

        mc.getNetworkHandler().sendPacket(
            new LookAndOnGround(cameraYaw, cameraPitch,
                                onGround, horizontalCollision));

        lastThrow = now;
    }

    // ---------- предсказание чужого жемчуга ----------

    private Vec3d predictLanding(EnderPearlEntity pearl) {
        Vec3d position = pearl.getPos();
        Vec3d velocity = pearl.getVelocity();

        for (int tick = 0; tick < MAX_SIMULATION_TICKS; tick++) {
            velocity = velocity.add(0.0, -PEARL_GRAVITY, 0.0).multiply(PEARL_DRAG);
            Vec3d next = position.add(velocity);
            BlockHitResult collision = raycast(position, next, pearl);
            if (collision != null) return collision.getPos();
            if (next.y <= mc.world.getBottomY()) return next;
            position = next;
        }
        return null;
    }

    // ---------- подбор броска ----------

    private ThrowSolution findBestThrowSolution(Vec3d impact) {
        ThrowSolution best = null;
        for (Vec3d target : findSafeTargets(impact)) {
            ThrowSolution s = findThrowSolution(target);
            if (s == null) continue;
            if (best == null
                || s.ticks() < best.ticks()
                || (s.ticks() == best.ticks() && s.error() < best.error())) {
                best = s;
            }
        }
        return best;
    }

    private ThrowSolution findThrowSolution(Vec3d target) {
        Vec3d eye = getThrowPosition();
        float yaw = MathHelper.wrapDegrees(
            (float) Math.toDegrees(Math.atan2(target.z - eye.z, target.x - eye.x)) - 90.0f);

        ThrowSolution best = null;
        for (float pitch = -55.0f; pitch <= 80.0f; pitch += 0.5f) {
            TrajectoryResult r = simulateOwnPearl(yaw, pitch);
            if (r == null) continue;
            double error = r.landing().distanceTo(target);
            if (error > MAX_TARGET_ERROR) continue;
            if (best == null
                || r.ticks() < best.ticks()
                || (r.ticks() == best.ticks() && error < best.error())) {
                best = new ThrowSolution(yaw, pitch, error, r.ticks());
            }
        }
        return best;
    }

    private List<Vec3d> findSafeTargets(Vec3d impact) {
        BlockPos center = BlockPos.ofFloored(impact);
        List<LandingCandidate> candidates = new ArrayList<>();

        for (int dx = -SAFE_SEARCH_RADIUS; dx <= SAFE_SEARCH_RADIUS; dx++) {
            for (int dz = -SAFE_SEARCH_RADIUS; dz <= SAFE_SEARCH_RADIUS; dz++) {
                double horizontal = Math.hypot(dx, dz);
                if (horizontal < MIN_FOLLOW_OFFSET || horizontal > 4.0) continue;

                for (int y = center.getY() + 4; y >= center.getY() - 6; y--) {
                    BlockPos floor = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    if (!isSafeLandingFloor(floor)) continue;
                    Vec3d point = new Vec3d(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5);
                    double score = point.squaredDistanceTo(impact) + horizontal * 0.35;
                    candidates.add(new LandingCandidate(point, score));
                    break;
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(LandingCandidate::score));
        return candidates.stream()
            .limit(MAX_SAFE_TARGETS)
            .map(LandingCandidate::position)
            .toList();
    }

    private boolean isSafeLandingFloor(BlockPos floor) {
        BlockState floorState = mc.world.getBlockState(floor);
        if (floorState.getCollisionShape(mc.world, floor).isEmpty()) return false;
        if (floorState.isIn(BlockTags.LOGS) || floorState.isIn(BlockTags.LEAVES)) return false;

        BlockPos feet = floor.up();
        BlockPos head = feet.up();
        return mc.world.getBlockState(feet).getCollisionShape(mc.world, feet).isEmpty()
            && mc.world.getBlockState(head).getCollisionShape(mc.world, head).isEmpty()
            && mc.world.getFluidState(feet).isEmpty()
            && mc.world.getFluidState(head).isEmpty();
    }

    private TrajectoryResult simulateOwnPearl(float yaw, float pitch) {
        Vec3d position = getThrowPosition();
        Vec3d velocity = getThrowVelocity(yaw, pitch);

        for (int tick = 0; tick < MAX_SIMULATION_TICKS; tick++) {
            velocity = velocity.add(0.0, -PEARL_GRAVITY, 0.0).multiply(PEARL_DRAG);
            Vec3d next = position.add(velocity);
            BlockHitResult collision = raycast(position, next, mc.player);
            if (collision != null) {
                if (collision.getSide() == Direction.UP
                    && isSafeLandingFloor(collision.getBlockPos())) {
                    return new TrajectoryResult(collision.getPos(), tick + 1);
                }
                return null;
            }
            if (next.y <= mc.world.getBottomY()) {
                return new TrajectoryResult(next, tick + 1);
            }
            position = next;
        }
        return null;
    }

    private Vec3d getThrowPosition() {
        return new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
    }

    private Vec3d getThrowVelocity(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRad) * PEARL_SPEED;
        Vec3d playerMovement = mc.player.getVelocity();
        return new Vec3d(
            -Math.sin(yawRad) * horizontal + playerMovement.x,
            -Math.sin(pitchRad) * PEARL_SPEED
                + (mc.player.isOnGround() ? 0.0 : playerMovement.y),
            Math.cos(yawRad) * horizontal + playerMovement.z
        );
    }

    private BlockHitResult raycast(Vec3d start, Vec3d end, Entity source) {
        BlockHitResult hit = mc.world.raycast(
            new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, source));
        return hit.getType() == Type.MISS ? null : hit;
    }

    // ---------- хелперы ----------

    private Slot findPearlSlot() {
        return PlayerInventoryUtil.slots()
            .filter(s -> s.getStack().isOf(Items.ENDER_PEARL))
            .findFirst()
            .orElse(null);
    }

    private record LandingCandidate(Vec3d position, double score) {}
    private record ThrowSolution(float yaw, float pitch, double error, int ticks) {}
    private record TrajectoryResult(Vec3d landing, int ticks) {}
}
