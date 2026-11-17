package ru.zov.implement.features.modules.render.betterrain;

import java.util.Random;

/**
 * Rain simulation with no Minecraft dependencies. Drops are spawned in a cylinder around the camera, fall with a
 * wind drift, and are recycled when they hit the ground, leave the cylinder, or sink below the floor margin.
 * A ground impact is reported through {@link Impact} and starts a splash ring.
 *
 * Coordinates are world coordinates. Units are blocks and seconds.
 */
public final class RainField {
    public static final int MAX_DROPS = 2400;
    public static final int SPLASH_CAPACITY = 128;
    public static final double SPAWN_CEILING = 9.0;
    public static final double SPAWN_SPAN = 3.0;
    public static final double FLOOR_MARGIN = 6.0;
    public static final double RECYCLE_RADIUS_GAIN = 1.3;
    public static final double FALL_SPEED_BASE = 9.0;
    public static final double SPEED_JITTER_MIN = 0.82;
    public static final double SPEED_JITTER_MAX = 1.18;
    public static final double ALPHA_JITTER_MIN = 0.55;
    public static final double ALPHA_JITTER_MAX = 1.15;
    public static final double NEAR_FADE_START = 0.35;
    public static final double NEAR_FADE_END = 1.4;
    public static final double SURFACE_LIFT = 0.012;
    public static final double WIND_SPEED_MAX = 3.2;
    public static final double WIND_GUST_AMPLITUDE = 0.22;
    public static final double WIND_GUST_RATE = 0.55;
    public static final double WIND_TURN_RATE = 0.035;
    public static final double DROPS_PER_SQUARE_BLOCK = 0.4;
    public static final double MIN_RAIN = 0.02;
    public static final long SPLASH_LIFETIME_MS = 360L;

    /** Top solid surface at (x, z), or NaN when the column is unknown. */
    public interface Ground {
        double surfaceY(int x, int z);
    }

    /** Called when a drop reaches a surface. */
    public interface Impact {
        void onImpact(double x, double y, double z);
    }

    public static final class Frame {
        public double camX;
        public double camY;
        public double camZ;
        public double radius;
        public double rain;
        public double density;
        public double speedSetting;
        public double opacity;
        public double windX;
        public double windZ;
    }

    private final double[] x = new double[MAX_DROPS];
    private final double[] y = new double[MAX_DROPS];
    private final double[] z = new double[MAX_DROPS];
    private final double[] speed = new double[MAX_DROPS];
    private final double[] alpha = new double[MAX_DROPS];
    private final Random rng;
    private int count;

    private final double[] splashX = new double[SPLASH_CAPACITY];
    private final double[] splashY = new double[SPLASH_CAPACITY];
    private final double[] splashZ = new double[SPLASH_CAPACITY];
    private final long[] splashStart = new long[SPLASH_CAPACITY];
    private int splashHead;
    private int splashCount;

    public RainField(long seed) {
        this.rng = new Random(seed);
    }

    public int dropCount() {
        return this.count;
    }

    public void clear() {
        this.count = 0;
        this.splashCount = 0;
        this.splashHead = 0;
    }

    /** One simulation step. dt is in seconds and should be clamped by the caller. */
    public void step(Frame f, double dt, Ground ground, Impact impact, long nowMs) {
        double recycleRadius = f.radius * RECYCLE_RADIUS_GAIN;
        double recycleSq = recycleRadius * recycleRadius;
        for (int i = 0; i < this.count; i++) {
            this.x[i] += f.windX * dt;
            this.z[i] += f.windZ * dt;
            this.y[i] -= this.speed[i] * dt;

            double surface = ground.surfaceY(floorInt(this.x[i]), floorInt(this.z[i]));
            if (!Double.isNaN(surface) && this.y[i] <= surface + SURFACE_LIFT) {
                impact.onImpact(this.x[i], surface + SURFACE_LIFT, this.z[i]);
                this.addSplash(this.x[i], surface + SURFACE_LIFT, this.z[i], nowMs);
                this.respawn(i, f);
                continue;
            }

            double dx = this.x[i] - f.camX;
            double dz = this.z[i] - f.camZ;
            if (this.y[i] < f.camY - FLOOR_MARGIN || dx * dx + dz * dz > recycleSq) {
                this.respawn(i, f);
            }
        }

        if (f.rain < MIN_RAIN) {
            return;
        }

        double area = Math.PI * f.radius * f.radius;
        int target = (int) Math.min(MAX_DROPS, Math.round(area * DROPS_PER_SQUARE_BLOCK * f.density * f.rain));
        while (this.count < target) {
            this.spawn(this.count, f);
            this.count++;
        }
    }

    public void respawn(int i, Frame f) {
        double angle = this.rng.nextDouble() * Math.PI * 2.0;
        double dist = f.radius * Math.sqrt(this.rng.nextDouble());
        this.x[i] = f.camX + Math.cos(angle) * dist;
        this.z[i] = f.camZ + Math.sin(angle) * dist;
        this.y[i] = f.camY + SPAWN_CEILING + this.rng.nextDouble() * SPAWN_SPAN;
        this.spawnKinematics(i, f);
    }

    private void spawn(int i, Frame f) {
        this.respawn(i, f);
    }

    private void spawnKinematics(int i, Frame f) {
        double jitter = lerp(SPEED_JITTER_MIN, SPEED_JITTER_MAX, this.rng.nextDouble());
        this.speed[i] = FALL_SPEED_BASE * f.speedSetting * jitter;
        double horizontal = Math.hypot(this.x[i] - f.camX, this.z[i] - f.camZ);
        double near = smoothstep(NEAR_FADE_START, NEAR_FADE_END, horizontal);
        double flicker = lerp(ALPHA_JITTER_MIN, ALPHA_JITTER_MAX, this.rng.nextDouble());
        this.alpha[i] = clamp01(f.opacity * flicker * near);
    }

    /** Wind speed and direction for a given time, as in the original module. */
    public static double[] wind(double timeSeconds, double windSetting) {
        double turn = timeSeconds * WIND_TURN_RATE;
        double gust = 1.0 + Math.sin(timeSeconds * WIND_GUST_RATE) * WIND_GUST_AMPLITUDE;
        double strength = windSetting * WIND_SPEED_MAX * gust;
        return new double[]{Math.cos(turn) * strength, Math.sin(turn) * strength};
    }

    public double x(int i) {
        return this.x[i];
    }

    public double y(int i) {
        return this.y[i];
    }

    public double z(int i) {
        return this.z[i];
    }

    public double speed(int i) {
        return this.speed[i];
    }

    public double alpha(int i) {
        return this.alpha[i];
    }

    public void addSplash(double sx, double sy, double sz, long nowMs) {
        splashX[splashHead] = sx;
        splashY[splashHead] = sy;
        splashZ[splashHead] = sz;
        splashStart[splashHead] = nowMs;
        splashHead = (splashHead + 1) % SPLASH_CAPACITY;
        if (splashCount < SPLASH_CAPACITY) {
            splashCount++;
        }
    }

    /** Number of splash slots still alive at nowMs. Fill them through {@link #splashAge}. */
    public int aliveSplashes(long nowMs) {
        int alive = 0;
        for (int i = 0; i < this.splashCount; i++) {
            if (nowMs - this.splashStart[i] < SPLASH_LIFETIME_MS) {
                alive++;
            }
        }
        return alive;
    }

    /** Age of splash slot i in [0, 1], or -1 when it has expired. */
    public double splashAge(int i, long nowMs) {
        long age = nowMs - this.splashStart[i];
        return age < SPLASH_LIFETIME_MS ? (double) age / SPLASH_LIFETIME_MS : -1.0;
    }

    public double splashX(int i) {
        return this.splashX[i];
    }

    public double splashY(int i) {
        return this.splashY[i];
    }

    public double splashZ(int i) {
        return this.splashZ[i];
    }

    public int splashSlots() {
        return this.splashCount;
    }

    /** Points of a horizontal ring around (cx, cy, cz). Returns segments + 1 points so the last joins the first. */
    public static double[][] ring(double cx, double cy, double cz, double radius, int segments) {
        double[][] points = new double[segments + 1][3];
        for (int k = 0; k <= segments; k++) {
            double a = Math.PI * 2.0 * k / segments;
            points[k][0] = cx + Math.cos(a) * radius;
            points[k][1] = cy;
            points[k][2] = cz + Math.sin(a) * radius;
        }
        return points;
    }

    private static int floorInt(double v) {
        return (int) Math.floor(v);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(1.0, v);
    }

    private static double smoothstep(double edge0, double edge1, double v) {
        double t = clamp01((v - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }
}
