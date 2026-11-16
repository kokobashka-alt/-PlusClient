package ru.zov.implement.features.modules.combat.killaura.neuro.trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.repository.FriendUtils;

/**
 * Minecraft glue for {@link CrystalAuraCore}. Every Yarn call lives in this file.
 * If your fork renames a method, only this file needs editing.
 *
 * <p>Rotations: before every {@code placeCrystal}/{@code placeBase}/{@code attackCrystal},
 * {@link #rotateTo} sends a {@link PlayerMoveC2SPacket.LookAndOnGround} packet with
 * the computed yaw/pitch and updates the client-side rotation. Ordering matters —
 * servers that ray-trace attacks (Polar, Grim) check whether the player was
 * looking at the target on the same tick as the interaction packet. If your cheat
 * already has a rotation manager (e.g. the one used by {@code PolarSnapRotationMode}),
 * replace the body of {@link #rotateTo} with a call into that manager.
 */
public final class McCrystalContext implements CrystalAuraCore.Context {
    private static final double SCAN_RADIUS = 14.0;

    private final Random random = new Random();
    private boolean restoreSlot = false;
    private boolean rage = false;

    public void setRestoreSlot(boolean restoreSlot) {
        this.restoreSlot = restoreSlot;
    }

    public void setRage(boolean rage) {
        this.rage = rage;
    }

    // =====================================================================
    // Basic queries
    // =====================================================================

    @Override
    public long nowMs() {
        return System.currentTimeMillis();
    }

    @Override
    public boolean canAct() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null
                && client.player != null
                && client.world != null
                && client.currentScreen == null
                && !client.isPaused()
                && client.player.isAlive()
                && !client.player.isUsingItem();
    }

    @Override
    public CrystalAuraCore.Point self() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return null;
        Vec3d center = player.getBoundingBox().getCenter();
        return new CrystalAuraCore.Point(center.x, center.y, center.z);
    }

    @Override
    public List<CrystalAuraCore.Target> targets() {
        MinecraftClient client = MinecraftClient.getInstance();
        List<CrystalAuraCore.Target> result = new ArrayList<>();
        if (client == null || client.player == null || client.world == null) return result;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == null || player == client.player) continue;
            if (!player.isAlive() || player.isSpectator()) continue;
            if (player.isCreative()) continue;
            if (FriendUtils.isFriend(player)) continue;

            Vec3d center = player.getBoundingBox().getCenter();
            result.add(new CrystalAuraCore.Target(
                    player.getId(),
                    new CrystalAuraCore.Point(center.x, center.y, center.z),
                    player.getHealth()));
        }
        return result;
    }

    @Override
    public List<CrystalAuraCore.Crystal> crystals() {
        MinecraftClient client = MinecraftClient.getInstance();
        List<CrystalAuraCore.Crystal> result = new ArrayList<>();
        if (client == null || client.player == null || client.world == null) return result;

        List<EndCrystalEntity> found = client.world.getEntitiesByClass(
                EndCrystalEntity.class,
                client.player.getBoundingBox().expand(SCAN_RADIUS),
                c -> c != null && c.isAlive() && !c.isRemoved());

        for (EndCrystalEntity crystal : found) {
            Vec3d center = crystal.getBoundingBox().getCenter();
            result.add(new CrystalAuraCore.Crystal(
                    crystal.getId(),
                    new CrystalAuraCore.Point(center.x, center.y, center.z)));
        }
        return result;
    }

    @Override
    public double angleTo(CrystalAuraCore.Point point) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || point == null) return Double.MAX_VALUE;

        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d dir = new Vec3d(point.x - eye.x, point.y - eye.y, point.z - eye.z);
        double len = dir.length();
        if (len < 1e-6) return 0.0;
        double dot = look.dotProduct(dir.normalize());
        return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
    }

    // =====================================================================
    // World queries
    // =====================================================================

    @Override
    public boolean isSupport(CrystalAuraCore.BlockPos block) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return false;
        BlockPos pos = toMinecraft(block);
        return world.getBlockState(pos).isOf(Blocks.OBSIDIAN)
                || world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    @Override
    public boolean isAir(CrystalAuraCore.BlockPos block) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return false;
        return world.isAir(toMinecraft(block));
    }

    @Override
    public boolean hasCrystalAt(CrystalAuraCore.BlockPos block) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return false;
        BlockPos pos = toMinecraft(block);
        Vec3d center = new Vec3d(block.x + 0.5, block.y + 0.5, block.z + 0.5);
        Box box = new Box(center.x - 0.7, center.y - 0.7, center.z - 0.7,
                          center.x + 0.7, center.y + 0.7, center.z + 0.7);
        List<EndCrystalEntity> found = client.world.getEntitiesByClass(
                EndCrystalEntity.class, box,
                c -> c != null && c.isAlive() && c.getBlockPos().equals(pos));
        return !found.isEmpty();
    }

    @Override
    public boolean canPlaceBase(CrystalAuraCore.BlockPos block) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return false;
        BlockPos pos = toMinecraft(block);
        BlockPos below = pos.down();
        // Must have a solid face to attach to
        return world.getBlockState(below).isSolidBlock(world, below)
                || world.getBlockState(pos.north()).isSolidBlock(world, pos.north())
                || world.getBlockState(pos.south()).isSolidBlock(world, pos.south())
                || world.getBlockState(pos.east()).isSolidBlock(world, pos.east())
                || world.getBlockState(pos.west()).isSolidBlock(world, pos.west());
    }

    // =====================================================================
    // Rotation
    // =====================================================================

    /**
     * Sends a rotation packet to the server and mirrors it client-side, so the
     * subsequent interaction packet (same tick) is processed with the correct look
     * vector. Replace this body if the cheat already has a rotation manager.
     */
    @Override
    public boolean rotateTo(CrystalAuraCore.Point point) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || point == null) return false;

        Vec3d eye = player.getEyePos();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float desiredYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float desiredPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        desiredPitch = MathHelper.clamp(desiredPitch, -89.0f, 89.0f);

        float newYaw;
        float newPitch;

        if (this.rage) {
            newYaw = desiredYaw;
            newPitch = desiredPitch;
        } else {
            // Legit: smooth toward desired. If we're already close, snap — real
            // players don't make tiny 2° turns frame by frame.
            float errYaw = MathHelper.wrapDegrees(desiredYaw - player.getYaw());
            float errPitch = desiredPitch - player.getPitch();
            if (Math.abs(errYaw) < 3.0f && Math.abs(errPitch) < 3.0f) {
                newYaw = desiredYaw;
                newPitch = desiredPitch;
            } else {
                float gain = 0.35f + this.random.nextFloat() * 0.25f;
                newYaw = player.getYaw() + errYaw * gain;
                newPitch = MathHelper.clamp(player.getPitch() + errPitch * gain, -89.0f, 89.0f);
            }
        }

        player.setYaw(newYaw);
        player.setPitch(newPitch);
        this.sendRotationPacket(newYaw, newPitch);
        return true;
    }

    private void sendRotationPacket(float yaw, float pitch) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null) return;
        player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), false));
    }

    // =====================================================================
    // Actions
    // =====================================================================

    @Override
    public boolean placeCrystal(CrystalAuraCore.BlockPos support) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) return false;

        int slot = findItemSlot(client.player, Items.END_CRYSTAL);
        if (slot < 0) return false;

        int oldSlot = client.player.getInventory().getSelectedSlot();
        if (slot != oldSlot) {
            client.player.getInventory().selectedSlot = slot;
        }

        BlockPos pos = toMinecraft(support);
        Vec3d hitPos = new Vec3d(support.x + 0.5, support.y + 1.0, support.z + 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, pos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);

        if (this.restoreSlot && slot != oldSlot) {
            client.player.getInventory().selectedSlot = oldSlot;
        }
        return true;
    }

    @Override
    public boolean placeBase(CrystalAuraCore.BlockPos support) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) return false;

        int slot = findItemSlot(client.player, Items.OBSIDIAN);
        if (slot < 0) return false;

        int oldSlot = client.player.getInventory().getSelectedSlot();
        if (slot != oldSlot) {
            client.player.getInventory().selectedSlot = slot;
        }

        BlockPos pos = toMinecraft(support);
        // Place obsidian by clicking the bottom face of the block above
        BlockPos anchor = pos.down();
        Vec3d hitPos = new Vec3d(anchor.getX() + 0.5, anchor.getY() + 1.0, anchor.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, anchor, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);

        if (this.restoreSlot && slot != oldSlot) {
            client.player.getInventory().selectedSlot = oldSlot;
        }
        return true;
    }

    @Override
    public boolean attackCrystal(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null
                || client.interactionManager == null) return false;

        EndCrystalEntity crystal = findCrystalById(client, id);
        if (crystal == null) return false;

        client.interactionManager.attackEntity(client.player, crystal);
        return true;
    }

    @Override
    public boolean attackCrystalPredict(int id, int offset) {
        // ID-predict requires raw PlayerInteractEntityC2SPacket construction, which
        // varies by MC version. For now we attack the real entity if it exists; the
        // offset range only matters on servers with entity-ID prediction in the client.
        // Replace this body with a custom packet writer if your fork already supports it.
        if (attackCrystal(id)) return true;
        // Fallback: try the same attack again — keeps the swing counter honest.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;
        return false;
    }

    @Override
    public void setDeadCrystal(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;
        EndCrystalEntity crystal = findCrystalById(client, id);
        if (crystal != null) {
            crystal.setRemoved(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void swing() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    // =====================================================================
    // Damage estimation
    // =====================================================================

    @Override
    public double selfDamage(CrystalAuraCore.Point crystalCenter) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return 0.0;
        int armor = (int) player.getAttributeValue(EntityAttributes.ARMOR);
        return estimateCrystalDamage(crystalCenter, player.getBoundingBox(), armor);
    }

    @Override
    public double targetDamage(CrystalAuraCore.Point crystalCenter, int targetId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return 0.0;

        for (PlayerEntity p : client.world.getPlayers()) {
            if (p != null && p.getId() == targetId) {
                int armor = (int) p.getAttributeValue(EntityAttributes.ARMOR);
                return estimateCrystalDamage(crystalCenter, p.getBoundingBox(), armor);
            }
        }
        return 0.0;
    }

    /**
     * Simplified crystal explosion damage:
     *   raw   = (1 - dist/12) * exposure * 6
     *   final = raw * (1 - armorReduction)
     * where armorReduction ≈ min(0.8, armor * 0.04).
     *
     * Exposure is approximated by sampling the 8 box corners + center and counting
     * how many are reachable from the crystal position.
     */
    private double estimateCrystalDamage(CrystalAuraCore.Point center, Box box, int armor) {
        Vec3d crystalVec = new Vec3d(center.x, center.y, center.z);
        Vec3d entityCenter = box.getCenter();
        double dist = crystalVec.distanceTo(entityCenter);
        if (dist > 12.0) return 0.0;

        double exposure = computeExposure(crystalVec, box);
        if (exposure <= 0.0) return 0.0;

        double raw = (1.0 - dist / 12.0) * exposure * 6.0;
        double armorReduction = Math.min(0.8, armor * 0.04);
        return raw * (1.0 - armorReduction);
    }

    private double computeExposure(Vec3d from, Box box) {
        Vec3d[] samples = new Vec3d[] {
                box.getCenter(),
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ),
        };
        int clear = 0;
        for (Vec3d s : samples) {
            if (this.rayClear(from, s)) clear++;
        }
        return (double) clear / samples.length;
    }

    private boolean rayClear(Vec3d from, Vec3d to) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return true;

        Vec3d dir = to.subtract(from);
        double len = dir.length();
        if (len < 1e-6) return true;
        Vec3d step = dir.normalize().multiply(0.2);

        Vec3d cur = from;
        for (double t = 0; t < len; t += 0.2) {
            BlockPos bp = BlockPos.ofFloored(cur);
            if (!world.getBlockState(bp).isAir()) return false;
            cur = cur.add(step);
        }
        return true;
    }

    // =====================================================================
    // Inventory helpers
    // =====================================================================

    private static int findItemSlot(ClientPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) return i;
        }
        return -1;
    }

    private static EndCrystalEntity findCrystalById(MinecraftClient client, int id) {
        if (client.player == null || client.world == null) return null;
        List<EndCrystalEntity> found = client.world.getEntitiesByClass(
                EndCrystalEntity.class,
                client.player.getBoundingBox().expand(SCAN_RADIUS),
                c -> c != null && c.isAlive() && c.getId() == id);
        return found.isEmpty() ? null : found.get(0);
    }

    private static BlockPos toMinecraft(CrystalAuraCore.BlockPos block) {
        return new BlockPos(block.x, block.y, block.z);
    }
}
