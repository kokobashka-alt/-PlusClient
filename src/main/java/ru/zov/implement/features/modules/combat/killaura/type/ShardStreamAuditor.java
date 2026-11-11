package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.EnumSet;
import java.util.Locale;

/**
 * Looks at the rotation deltas that were actually applied (after sensitivity quantisation) and
 * reports statistics that generic rotation heuristics tend to look at.
 *
 * The thresholds below are ASSUMPTIONS taken from common anticheat techniques. They are not
 * Shard's real limits; they only tell the mode when its own output looks degenerate (too smooth,
 * repetitive, snapping, or not on the sensitivity grid).
 *
 * Units: degrees per tick; the window holds the last {@link #CAPACITY} engaged ticks.
 */
public final class ShardStreamAuditor {
    public static final int CAPACITY = 200;
    public static final int MIN_SAMPLES = 80;

    public enum Flag { TOO_SMOOTH, TOO_PERFECT_LOCK, SNAP, REPETITIVE, GCD_MISMATCH }

    public record Report(int samples, float meanSpeed, float maxStep, float yawAccelStdDev,
                         float zeroRatio, float lockRatio, float maxAutocorrelation,
                         float gcdAlignedRatio, EnumSet<Flag> flags) {
        public boolean isClean() {
            return flags.isEmpty();
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "n=%d speed=%.2f max=%.1f accSd=%.2f zero=%.2f lock=%.2f ac=%.2f gcd=%.2f flags=%s",
                    samples, meanSpeed, maxStep, yawAccelStdDev, zeroRatio, lockRatio,
                    maxAutocorrelation, gcdAlignedRatio, flags);
        }
    }

    static final float SMOOTH_ACCEL_SD = 0.08F;
    static final float PERFECT_LOCK_ERROR = 0.05F;
    static final float PERFECT_LOCK_RATIO = 0.30F;
    static final float SNAP_SPEED = 25.0F;
    static final float SNAP_PREV_RATIO = 0.25F;
    static final float REPEAT_AUTOCORR = 0.80F;
    static final int REPEAT_MIN_LAG = 10;
    static final int REPEAT_MAX_LAG = 40;
    static final float GCD_ALIGNED_MIN = 0.90F;
    static final float GCD_TOLERANCE = 0.02F;
    static final float MOVING_EPSILON = 1.0E-4F;

    private final float[] yaw = new float[CAPACITY];
    private final float[] pitch = new float[CAPACITY];
    private final float[] error = new float[CAPACITY];
    private int head;
    private int count;

    public void clear() {
        head = 0;
        count = 0;
    }

    public int size() {
        return count;
    }

    /** Records one engaged tick. Non-finite samples are dropped. */
    public void record(float deltaYaw, float deltaPitch, float aimErrorDegrees) {
        if (!Float.isFinite(deltaYaw) || !Float.isFinite(deltaPitch) || !Float.isFinite(aimErrorDegrees)) {
            return;
        }
        yaw[head] = deltaYaw;
        pitch[head] = deltaPitch;
        error[head] = aimErrorDegrees;
        head = (head + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }
    }

    /** Returns null while the window is too short to say anything. {@code gcd} is the sensitivity step in degrees. */
    public Report analyze(double gcd) {
        if (count < MIN_SAMPLES) {
            return null;
        }
        float[] dy = ordered(yaw);
        float[] dp = ordered(pitch);
        float[] er = ordered(error);
        int n = dy.length;

        float speedSum = 0.0F;
        float maxStep = 0.0F;
        int zero = 0;
        int lock = 0;
        int snaps = 0;
        float prevSpeed = 0.0F;
        float[] speed = new float[n];
        for (int i = 0; i < n; i++) {
            speed[i] = (float) Math.hypot(dy[i], dp[i]);
            speedSum += speed[i];
            maxStep = Math.max(maxStep, speed[i]);
            if (speed[i] < MOVING_EPSILON) {
                zero++;
            }
            if (er[i] < PERFECT_LOCK_ERROR) {
                lock++;
            }
            if (i > 0 && speed[i] > SNAP_SPEED && prevSpeed < SNAP_PREV_RATIO * speed[i]) {
                snaps++;
            }
            prevSpeed = speed[i];
        }

        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        float accelSd = accelStdDev(dy);
        float zeroRatio = (float) zero / n;
        float lockRatio = (float) lock / n;
        float autocorr = maxAutocorrelation(dy);
        float aligned = gcdAligned(dy, dp, gcd);

        if (accelSd < SMOOTH_ACCEL_SD && zeroRatio < 0.9F) {
            flags.add(Flag.TOO_SMOOTH);
        }
        if (lockRatio > PERFECT_LOCK_RATIO) {
            flags.add(Flag.TOO_PERFECT_LOCK);
        }
        if (snaps > 0) {
            flags.add(Flag.SNAP);
        }
        if (autocorr > REPEAT_AUTOCORR) {
            flags.add(Flag.REPETITIVE);
        }
        if (aligned >= 0.0F && aligned < GCD_ALIGNED_MIN) {
            flags.add(Flag.GCD_MISMATCH);
        }
        return new Report(n, speedSum / n, maxStep, accelSd, zeroRatio, lockRatio, autocorr, aligned, flags);
    }

    private float[] ordered(float[] ring) {
        float[] out = new float[count];
        int start = count < CAPACITY ? 0 : head;
        for (int i = 0; i < count; i++) {
            out[i] = ring[(start + i) % CAPACITY];
        }
        return out;
    }

    /** Standard deviation of tick-to-tick yaw acceleration, over moving ticks only. */
    static float accelStdDev(float[] dy) {
        double sum = 0.0;
        double sumSq = 0.0;
        int m = 0;
        for (int i = 1; i < dy.length; i++) {
            if (Math.abs(dy[i]) < MOVING_EPSILON && Math.abs(dy[i - 1]) < MOVING_EPSILON) {
                continue;
            }
            double a = dy[i] - dy[i - 1];
            sum += a;
            sumSq += a * a;
            m++;
        }
        if (m < 10) {
            return Float.MAX_VALUE;
        }
        double mean = sum / m;
        return (float) Math.sqrt(Math.max(0.0, sumSq / m - mean * mean));
    }

    /**
     * Largest absolute normalised autocorrelation of the yaw deltas for lags 10..40 ticks. It also
     * reflects the target's own rhythm (for example A/D strafing), so it cannot prove that the
     * rotation itself repeats; treat the flag as a hint only.
     */
    static float maxAutocorrelation(float[] dy) {
        int n = dy.length;
        if (n < REPEAT_MAX_LAG * 2) {
            return 0.0F;
        }
        double mean = 0.0;
        for (float v : dy) {
            mean += v;
        }
        mean /= n;
        double variance = 0.0;
        for (float v : dy) {
            variance += (v - mean) * (v - mean);
        }
        if (variance < 1.0E-9) {
            return 0.0F;
        }
        double best = 0.0;
        for (int lag = REPEAT_MIN_LAG; lag <= REPEAT_MAX_LAG; lag++) {
            double c = 0.0;
            for (int i = lag; i < n; i++) {
                c += (dy[i] - mean) * (dy[i - lag] - mean);
            }
            best = Math.max(best, Math.abs(c / variance));
        }
        return (float) best;
    }

    /** Share of non-zero deltas that sit on the sensitivity grid; -1 when it cannot be judged. */
    static float gcdAligned(float[] dy, float[] dp, double gcd) {
        if (!(gcd > 1.0E-6) || !Double.isFinite(gcd)) {
            return -1.0F;
        }
        int total = 0;
        int ok = 0;
        for (int i = 0; i < dy.length; i++) {
            for (float d : new float[] {dy[i], dp[i]}) {
                if (Math.abs(d) < MOVING_EPSILON) {
                    continue;
                }
                total++;
                double steps = d / gcd;
                if (Math.abs(steps - Math.rint(steps)) < GCD_TOLERANCE) {
                    ok++;
                }
            }
        }
        return total < 20 ? -1.0F : (float) ok / total;
    }
}
