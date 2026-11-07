package ru.zov.implement.features.modules.combat.killaura.util;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.math.RotationMath;
import ru.zov.implement.features.modules.combat.killaura.type.ShapeType;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class HitPointResolver {
    private static final int STABLE_VERTICAL_SAMPLES = 10;

    public record StableAimPoint(
            int entityId,
            double xRatio,
            double yRatio,
            double zRatio,
            Vec3d point,
            double angularCost
    ) {
    }

    public final Mc wrapper = Mc.INSTANCE;
    public final Random random = new SecureRandom();
    public Vec3d offset = Vec3d.ZERO;

    public Pair<Vec3d, Box> computeVector(LivingEntity livingEntity, float f, Rotation class007Var, Vec3d vec3d, boolean z) {
        Pair<List<Vec3d>, Box> pairGenerateCandidatePoints = generateCandidatePoints(livingEntity, f, z);
        randomizeOffset(vec3d);
        Vec3d vec3dMethod003 = findBestPoint((List) pairGenerateCandidatePoints.getLeft(), class007Var);
        return new Pair<>((vec3dMethod003 == null ? livingEntity.getEyePos() : vec3dMethod003).add(this.offset), (Box) pairGenerateCandidatePoints.getRight());
    }

    public Pair<Vec3d, Box> computeVector(Box box, float f, Rotation class007Var, Vec3d vec3d, boolean z) {
        Pair<List<Vec3d>, Box> pairGenerateCandidatePointsForBox = generateCandidatePointsForBox(box, f, z);
        Vec3d vec3dMethod003 = findBestPoint((List) pairGenerateCandidatePointsForBox.getLeft(), class007Var);
        randomizeOffset(vec3d);
        return new Pair<>((vec3dMethod003 == null ? getClosestVec(this.wrapper.getPlayer().getEyePos(), box) : vec3dMethod003).add(this.offset), (Box) pairGenerateCandidatePointsForBox.getRight());
    }

    public Pair<List<Vec3d>, Box> generateCandidatePointsForBox(Box box, float f, boolean z) {
        double lengthY = box.getLengthY() / 10.0d;
        Vec3d eyePos = this.wrapper.getPlayer().getEyePos();
        ArrayList arrayList = new ArrayList();
        double d = box.minY;
        while (true) {
            double d2 = d;
            if (d2 > box.maxY) {
                break;
            }
            Vec3d vec3d = new Vec3d(box.getCenter().x, d2, box.getCenter().z);
            if (isReachable(eyePos, vec3d, f, z)) {
                arrayList.add(vec3d);
            }
            d = d2 + lengthY;
        }
        if (arrayList.isEmpty()) {
            Vec3d closestVec = getClosestVec(eyePos, box);
            if (isReachable(eyePos, closestVec, f, z)) {
                arrayList.add(closestVec);
            }
        }
        return new Pair<>(arrayList, box);
    }

    public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity livingEntity, float f, boolean z) {
        Box boundingBox = livingEntity.getBoundingBox();
        double lengthY = boundingBox.getLengthY() / 10.0d;
        Vec3d eyePos = this.wrapper.getPlayer().getEyePos();
        ArrayList arrayList = new ArrayList();
        double d = boundingBox.minY;
        while (true) {
            double d2 = d;
            if (d2 > boundingBox.maxY) {
                break;
            }
            Vec3d vec3d = new Vec3d(boundingBox.getCenter().x, d2, boundingBox.getCenter().z);
            if (isReachable(eyePos, vec3d, f, z)) {
                arrayList.add(vec3d);
            }
            d = d2 + lengthY;
        }
        if (arrayList.isEmpty()) {
            Vec3d closestVec = getClosestVec(eyePos, boundingBox);
            if (isReachable(eyePos, closestVec, f, z)) {
                arrayList.add(closestVec);
            }
        }
        return new Pair<>(arrayList, boundingBox);
    }

    public Vec3d getClosestVec(Vec3d vec3d, Box box) {
        return new Vec3d(MathUtil.clamp(vec3d.x, box.minX, box.maxX), MathUtil.clamp(vec3d.y, box.minY, box.maxY), MathUtil.clamp(vec3d.z, box.minZ, box.maxZ));
    }

    public boolean hasValidPoint(LivingEntity livingEntity, float f, boolean z) {
        if (livingEntity == null) {
            return false;
        }
        Box boundingBox = livingEntity.getBoundingBox();
        double lengthY = boundingBox.getLengthY() / 10.0d;
        Vec3d eyePos = this.wrapper.getPlayer().getEyePos();
        double d = boundingBox.minY;
        while (true) {
            double d2 = d;
            if (d2 >= boundingBox.maxY) {
                return isReachable(eyePos, getClosestVec(eyePos, boundingBox), f, z);
            }
            if (isReachable(eyePos, new Vec3d(boundingBox.getCenter().x, d2, boundingBox.getCenter().z), f, z)) {
                return true;
            }
            d = d2 + lengthY;
        }
    }

    public boolean isReachable(Vec3d vec3d, Vec3d vec3d2, float f, boolean z) {
        return vec3d.distanceTo(vec3d2) <= ((double) f) && (z || !RaycastUtil.raycast(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER).getType().equals(HitResult.Type.BLOCK));
    }

    public Vec3d findBestPoint(List<Vec3d> list, Rotation class007Var) {
        return list.stream().min(Comparator.comparing(vec3d -> {
            return Double.valueOf(computeRotationCost(this.wrapper.getPlayer().getEyePos(), vec3d, class007Var));
        })).orElse(null);
    }

    public Optional<StableAimPoint> resolveStablePoint(
            LivingEntity target,
            ClientPlayerEntity observer,
            float maxDistance,
            Rotation referenceRotation,
            boolean ignoreWalls,
            StableAimPoint previous,
            double switchMarginDegrees
    ) {
        if (target == null
                || observer == null
                || !target.isAlive()
                || !Float.isFinite(maxDistance)
                || maxDistance <= 0.0F
                || referenceRotation == null
                || !Float.isFinite(referenceRotation.getYaw())
                || !Float.isFinite(referenceRotation.getPitch())) {
            return Optional.empty();
        }

        Box box = target.getBoundingBox();
        Vec3d eyePosition = observer.getEyePos();
        StableAimPoint best = null;
        for (int sample = 0; sample <= STABLE_VERTICAL_SAMPLES; sample++) {
            double yRatio = (double) sample / STABLE_VERTICAL_SAMPLES;
            Vec3d point = projectToBox(box, 0.5D, yRatio, 0.5D);
            StableAimPoint candidate = createStablePoint(
                    target,
                    eyePosition,
                    point,
                    box,
                    maxDistance,
                    referenceRotation,
                    ignoreWalls,
                    observer
            );
            if (candidate != null
                    && (best == null
                    || candidate.angularCost() < best.angularCost()
                    || (candidate.angularCost() == best.angularCost() && candidate.yRatio() < best.yRatio()))) {
                best = candidate;
            }
        }

        if (best == null) {
            Vec3d closestPoint = getClosestVec(eyePosition, box);
            best = createStablePoint(
                    target,
                    eyePosition,
                    closestPoint,
                    box,
                    maxDistance,
                    referenceRotation,
                    ignoreWalls,
                    observer
            );
        }
        if (best == null) {
            return Optional.empty();
        }

        if (previous != null && previous.entityId() == target.getId()) {
            Vec3d retainedPoint = projectToBox(
                    box,
                    previous.xRatio(),
                    previous.yRatio(),
                    previous.zRatio()
            );
            StableAimPoint retained = createStablePoint(
                    target,
                    eyePosition,
                    retainedPoint,
                    box,
                    maxDistance,
                    referenceRotation,
                    ignoreWalls,
                    observer
            );
            double margin = Math.max(0.0D, switchMarginDegrees);
            if (retained != null && best.angularCost() + margin >= retained.angularCost()) {
                return Optional.of(retained);
            }
        }
        return Optional.of(best);
    }

    private StableAimPoint createStablePoint(
            LivingEntity target,
            Vec3d eyePosition,
            Vec3d point,
            Box box,
            float maxDistance,
            Rotation referenceRotation,
            boolean ignoreWalls,
            Entity observer
    ) {
        if (eyePosition.distanceTo(point) > maxDistance
                || !RaycastUtil.hasExactLineOfSight(eyePosition, point, observer, ignoreWalls)) {
            return null;
        }
        return new StableAimPoint(
                target.getId(),
                ratio(point.x, box.minX, box.maxX),
                ratio(point.y, box.minY, box.maxY),
                ratio(point.z, box.minZ, box.maxZ),
                point,
                computeRotationCost(eyePosition, point, referenceRotation)
        );
    }

    private static Vec3d projectToBox(Box box, double xRatio, double yRatio, double zRatio) {
        return new Vec3d(
                MathHelper.lerp(MathUtil.clamp(xRatio, 0.0D, 1.0D), box.minX, box.maxX),
                MathHelper.lerp(MathUtil.clamp(yRatio, 0.0D, 1.0D), box.minY, box.maxY),
                MathHelper.lerp(MathUtil.clamp(zRatio, 0.0D, 1.0D), box.minZ, box.maxZ)
        );
    }

    private static double ratio(double value, double minimum, double maximum) {
        double size = maximum - minimum;
        if (!Double.isFinite(size) || Math.abs(size) <= 1.0E-9D) {
            return 0.5D;
        }
        return MathUtil.clamp((value - minimum) / size, 0.0D, 1.0D);
    }

    public double computeRotationCost(Vec3d vec3d, Vec3d vec3d2, Rotation class007Var) {
        Rotation class007VarCalculateRotationDifference = RotationMath.INSTANCE.calculateRotationDifference(RotationMath.INSTANCE.fromVec3d(vec3d2.subtract(vec3d)), class007Var);
        return Math.hypot(class007VarCalculateRotationDifference.getYaw(), class007VarCalculateRotationDifference.getPitch());
    }

    public void randomizeOffset(Vec3d vec3d) {
        this.offset = this.offset.add(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).multiply(vec3d);
    }

    public Mc getWrapper() {
        return this.wrapper;
    }

    public Random getRandom() {
        return this.random;
    }

    public Vec3d getOffset() {
        return this.offset;
    }
}
