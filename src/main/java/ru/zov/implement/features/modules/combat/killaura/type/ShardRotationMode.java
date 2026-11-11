package ru.zov.implement.features.modules.combat.killaura.type;

import java.security.SecureRandom;
import java.util.Locale;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.type.RotationMode;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;

/**
 * Aura rotation mode "Shard". Glue between {@link RotationMode} and the Minecraft-free parts:
 * {@link ShardPatternScheduler} (which hand style is active, switching at randomised minutes),
 * {@link ShardMotionEngine} (the per-tick motion) and {@link ShardStreamAuditor} (self-check of the
 * rotations that were really applied after sensitivity quantisation).
 *
 * Called once per client tick from RotationManager.updateRotation. {@code from} is the rotation the
 * client currently has (already on the sensitivity grid), {@code to} the wanted rotation. With a null
 * {@code entity} the call is the return-to-camera phase.
 *
 * Sensitivity quantisation is left to Rotation.normalize(); this class never quantises on its own.
 */
public final class ShardRotationMode extends RotationMode {
    private static final int AUDIT_INTERVAL_TICKS = 100;
    private static final int ATTACK_IMMINENT_TICKS = 2;
    private static final float EXTERNAL_JUMP_DEGREES = 60.0F;

    private final ShardPatternScheduler scheduler;
    private final ShardMotionEngine engine;
    private final ShardStreamAuditor auditor = new ShardStreamAuditor();

    private ClientPlayerEntity lastPlayer;
    private int lastPlayerAge = Integer.MIN_VALUE;
    private Rotation lastResult;
    private Rotation previousFrom;
    private boolean previousEngaged;
    private int ticksSinceAudit;
    private ShardStreamAuditor.Report lastReport;

    public ShardRotationMode() {
        super("Shard");
        long seed = new SecureRandom().nextLong();
        this.scheduler = new ShardPatternScheduler(seed, System::currentTimeMillis);
        this.engine = new ShardMotionEngine(seed ^ 0x9E3779B97F4A7C15L);
    }

    @Override
    public Rotation process(Rotation from, Rotation to, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || from == null || to == null) {
            return to != null ? to : from;
        }
        if (!Float.isFinite(from.getYaw()) || !Float.isFinite(from.getPitch())
                || !Float.isFinite(to.getYaw()) || !Float.isFinite(to.getPitch())) {
            return from;
        }
        if (player == lastPlayer && player.age == lastPlayerAge && lastResult != null) {
            return lastResult;
        }
        lastPlayer = player;
        lastPlayerAge = player.age;

        Aura aura = Aura.getInstance();
        boolean engaged = entity != null;
        if (aura != null) {
            scheduler.setIntervalMinutes(aura.shardMinMinutes.currentValue(), aura.shardMaxMinutes.currentValue());
        }
        observeAppliedRotation(from, engaged);

        ShardProfile profile = scheduler.profileFor(engaged);
        boolean attackImminent = engaged && aura != null && aura.canAttack(player, ATTACK_IMMINENT_TICKS);
        ShardMotionEngine.Output out = engine.step(new ShardMotionEngine.Input(
                from.getYaw(), from.getPitch(), to.getYaw(), to.getPitch(),
                engaged ? entity.getId() : -1, engaged, attackImminent), profile);

        lastResult = new Rotation(out.yaw(), MathHelper.clamp(out.pitch(), -90.0F, 90.0F));
        previousFrom = from;
        previousEngaged = engaged;
        runAudit(engaged);
        return lastResult;
    }

    /** Zero: the engine's own correlated drift replaces the per-tick white-noise offset. */
    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    public void reset() {
        engine.reset();
        scheduler.reset();
        auditor.clear();
        lastPlayer = null;
        lastPlayerAge = Integer.MIN_VALUE;
        lastResult = null;
        previousFrom = null;
        previousEngaged = false;
        ticksSinceAudit = 0;
        lastReport = null;
    }

    public String debugLine() {
        long ms = scheduler.millisUntilSwitch();
        String next = ms < 0L ? "-" : String.format(Locale.ROOT, "%d:%02d", ms / 60_000L, (ms / 1000L) % 60L);
        return "Shard[profile=" + scheduler.activeName() + ", next switch in " + next
                + ", audit=" + (lastReport == null ? "n/a" : lastReport) + "]";
    }

    /** The difference between consecutive {@code from} values is what the client really applied. */
    private void observeAppliedRotation(Rotation from, boolean engaged) {
        if (engaged && previousEngaged && previousFrom != null) {
            float dYaw = MathHelper.wrapDegrees(from.getYaw() - previousFrom.getYaw());
            float dPitch = from.getPitch() - previousFrom.getPitch();
            if (Math.abs(dYaw) < EXTERNAL_JUMP_DEGREES && Math.abs(dPitch) < EXTERNAL_JUMP_DEGREES) {
                auditor.record(dYaw, dPitch, engine.lastErrorDegrees());
            }
        }
    }

    private void runAudit(boolean engaged) {
        if (!engaged || ++ticksSinceAudit < AUDIT_INTERVAL_TICKS) {
            return;
        }
        ticksSinceAudit = 0;
        ShardStreamAuditor.Report report = auditor.analyze(MathUtil.computeGcd());
        if (report == null) {
            return;
        }
        lastReport = report;
        // REPETITIVE is report-only: a target that strafes rhythmically makes any tracker periodic too.
        if (report.flags().contains(ShardStreamAuditor.Flag.TOO_SMOOTH)
                || report.flags().contains(ShardStreamAuditor.Flag.TOO_PERFECT_LOCK)) {
            scheduler.requestEarlySwitch();
        }
    }
}
