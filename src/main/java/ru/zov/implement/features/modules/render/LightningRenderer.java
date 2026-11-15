package ru.zov.implement.features.modules.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;
import ru.zov.common.QuickImports;
import ru.zov.common.util.render.Render3DUtil;

/**
 * Lightning bolts jumping off the target's body (TargetESP mode "Молния"), ported from the fun.rich client.
 * All geometry is in world coordinates, which is what Render3DUtil's queues expect (they apply the camera
 * transform themselves). Bolts are short randomly displaced polylines drawn with a white core line and a
 * soft bloom blob on every vertex; the glow takes the given color and turns red while the target is hurt.
 */
public class LightningRenderer implements QuickImports {
    private static final int MAX_BOLTS = 16;
    private static final int SPAWN_PER_TICK = 2;
    private static final long SPAWN_INTERVAL_MS = 42L;
    private static final int LIGHTNING_DEPTH = 3;

    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF3B30;

    private static final Identifier BLOOM = Identifier.of("textures/bloom.png");

    private final Random random = new Random();
    private final Bolt[] bolts = new Bolt[MAX_BOLTS];
    private int boltCount;
    private long lastSpawn;

    public void reset() {
        boltCount = 0;
    }

    /**
     * @param basePos       target position in world coordinates (interpolated feet position)
     * @param anim          overall fade, 0..1
     * @param hitProgress   0..1, how strongly the glow is tinted red
     * @param glowRgb       glow color as 0xRRGGBB (alpha ignored)
     * @param depth         true to hide the effect behind blocks
     */
    public void render(LivingEntity target, Vec3d basePos, float anim, float hitProgress, int glowRgb, boolean depth) {
        if (target == null || anim <= 0.01F) {
            boltCount = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSpawn > SPAWN_INTERVAL_MS && boltCount < MAX_BOLTS) {
            for (int s = 0; s < SPAWN_PER_TICK && boltCount < MAX_BOLTS; s++) {
                bolts[boltCount++] = spawnBolt(target, basePos);
            }
            lastSpawn = now;
        }

        int baseGlow = glowRgb & 0xFFFFFF;
        for (int i = 0; i < boltCount; i++) {
            Bolt bolt = bolts[i];
            if (now - bolt.spawnTime > bolt.lifetimeMs) {
                bolts[i] = bolts[--boltCount];
                bolts[boltCount] = null;
                i--;
                continue;
            }

            float life = (now - bolt.spawnTime) / (float) bolt.lifetimeMs;
            float flicker = 0.65F + random.nextFloat() * 0.35F;
            float boltAlpha = anim * (1.0F - life) * flicker;
            if (boltAlpha <= 0.02F) {
                continue;
            }

            int glowColor = withAlpha(lerpRgb(baseGlow, RED, hitProgress), (int) (boltAlpha * 150));
            int coreColor = withAlpha(lerpRgb(WHITE, RED, hitProgress), (int) (boltAlpha * 255));

            for (Vec3d point : bolt.points) {
                float blobScale = 0.16F + random.nextFloat() * 0.10F;
                drawGlowBlob(point, blobScale, glowColor, depth);
            }
            drawPath(bolt.points, coreColor, 1.6F, depth);
        }
    }

    private void drawPath(List<Vec3d> points, int color, float width, boolean depth) {
        for (int i = 0; i < points.size() - 1; i++) {
            Render3DUtil.drawLine(points.get(i), points.get(i + 1), color, width, depth);
        }
    }

    private void drawGlowBlob(Vec3d pos, float scale, int color, boolean depth) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(Render3DUtil.lastWorldSpaceMatrix.getPositionMatrix());
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.multiply(camera.getRotation());
        MatrixStack.Entry entry = matrices.peek().copy();
        Render3DUtil.drawTexture(entry, BLOOM, -scale / 2.0F, -scale / 2.0F, scale, scale, new Vector4i(color), depth);
    }

    private Bolt spawnBolt(LivingEntity target, Vec3d basePos) {
        double width = target.getWidth();
        double height = target.getHeight();
        double surfaceRadius = width * 0.5 + 0.05;

        double angle = random.nextDouble() * Math.PI * 2;
        double h = random.nextDouble() * height;
        Vec3d start = basePos.add(Math.cos(angle) * surfaceRadius, h, Math.sin(angle) * surfaceRadius);

        double theta = random.nextDouble() * Math.PI * 2;
        double phi = Math.toRadians((random.nextDouble() - 0.5) * 140.0);
        double dx = Math.cos(phi) * Math.cos(theta);
        double dy = Math.sin(phi);
        double dz = Math.cos(phi) * Math.sin(theta);

        double spikeLength = 0.25 + random.nextDouble() * 0.3;
        Vec3d end = start.add(dx * spikeLength, dy * spikeLength, dz * spikeLength);

        Bolt bolt = new Bolt();
        bolt.points = generateLightningPath(start, end, LIGHTNING_DEPTH, spikeLength * 0.4);
        bolt.spawnTime = System.currentTimeMillis();
        bolt.lifetimeMs = 130 + random.nextInt(140);
        return bolt;
    }

    private List<Vec3d> generateLightningPath(Vec3d start, Vec3d end, int depth, double maxOffset) {
        if (depth <= 0) {
            List<Vec3d> result = new ArrayList<>(2);
            result.add(start);
            result.add(end);
            return result;
        }

        Vec3d dir = end.subtract(start);
        Vec3d mid = start.add(dir.multiply(0.5));
        Vec3d perp = randomPerpendicular(dir);
        double offset = (random.nextDouble() - 0.5) * 2.0 * maxOffset;
        Vec3d displacedMid = mid.add(perp.multiply(offset));

        List<Vec3d> left = generateLightningPath(start, displacedMid, depth - 1, maxOffset * 0.5);
        List<Vec3d> right = generateLightningPath(displacedMid, end, depth - 1, maxOffset * 0.5);
        left.remove(left.size() - 1);
        left.addAll(right);
        return left;
    }

    private Vec3d randomPerpendicular(Vec3d dir) {
        Vec3d normDir = dir.normalize();
        Vec3d arbitrary = Math.abs(normDir.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d perp1 = normDir.crossProduct(arbitrary).normalize();
        Vec3d perp2 = normDir.crossProduct(perp1).normalize();
        double angle = random.nextDouble() * Math.PI * 2;
        return perp1.multiply(Math.cos(angle)).add(perp2.multiply(Math.sin(angle)));
    }

    private static int lerpRgb(int rgbA, int rgbB, float t) {
        if (t <= 0.0F) {
            return rgbA;
        }
        if (t >= 1.0F) {
            return rgbB;
        }
        int rA = (rgbA >> 16) & 0xFF, gA = (rgbA >> 8) & 0xFF, bA = rgbA & 0xFF;
        int rB = (rgbB >> 16) & 0xFF, gB = (rgbB >> 8) & 0xFF, bB = rgbB & 0xFF;
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(alpha, 255)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static final class Bolt {
        List<Vec3d> points;
        long spawnTime;
        long lifetimeMs;
    }
}
