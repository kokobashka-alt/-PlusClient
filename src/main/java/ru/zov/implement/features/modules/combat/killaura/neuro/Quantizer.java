package ru.zov.implement.features.modules.combat.killaura.neuro;

/**
 * Snaps rotation steps to whole mouse counts and carries the remainder into the next tick, so the aim moves on the
 * same grid a human mouse would. One instance per axis pair; {@link #reset()} is called on every fresh segment.
 */
public final class Quantizer {
    private final double[] residual = new double[2];

    public void reset() {
        this.residual[0] = 0.0;
        this.residual[1] = 0.0;
    }

    /**
     * @param axis    0 for yaw, 1 for pitch
     * @param desired desired step in degrees for this tick
     * @param step    mouse count in degrees, must be positive
     * @return the step to apply, or 0 when the request lands below one count
     */
    public float step(int axis, float desired, double step) {
        if (axis < 0 || axis > 1) {
            throw new IllegalArgumentException("axis " + axis);
        }
        if (!(step > 1.0E-4) || Double.isNaN(step) || Double.isInfinite(step)) {
            throw new IllegalArgumentException("bad mouse step " + step);
        }
        if (!Float.isFinite(desired)) {
            return 0.0F;
        }

        double pending = desired + this.residual[axis];
        long counts = Math.round(pending / step);
        double exact = counts * step;
        this.residual[axis] = pending - exact;
        if (counts == 0) {
            return 0.0F;
        }
        // The client truncates delta / step, so land a hair above the exact multiple.
        return (float) (exact + Math.signum(exact) * step * 0.01);
    }

    /** Remainder not yet applied, in degrees. Exposed for tests and diagnostics. */
    public double residual(int axis) {
        return this.residual[axis];
    }
}
