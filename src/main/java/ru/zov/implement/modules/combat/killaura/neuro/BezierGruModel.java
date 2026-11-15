package ru.zov.implement.features.modules.combat.killaura.type;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import ru.zov.implement.features.modules.combat.killaura.neuro.Gru;

/**
 * Thin holder around {@link Gru} so the rotation code names the model in game terms. Loading and validation happen in
 * {@link Gru}; this class only exposes the sizes and the sampling call.
 */
public final class BezierGruModel {
    public final int features;
    public final int hidden;
    public final int layers;

    private final Gru gru;

    private BezierGruModel(Gru gru) {
        this.gru = gru;
        this.features = gru.features();
        this.hidden = gru.hidden();
        this.layers = gru.layers();
    }

    public static BezierGruModel load(Path path) throws IOException {
        return new BezierGruModel(Gru.load(path));
    }

    /** Degrees the head output is multiplied by. Stored in the model file. */
    public float labelScale() {
        return this.gru.labelScale();
    }

    public void reset() {
        this.gru.reset();
    }

    /**
     * Predicts and samples one tick.
     *
     * @param input    {@link BezierFeatures#COUNT} features for this tick
     * @param rng      randomness source; one instance per mode is fine
     * @param temperature multiplier on the learned sigma, 1 keeps the trained spread
     * @param step     receives yaw and pitch step in degrees
     * @param head     receives mean yaw, mean pitch, log sigma yaw, log sigma pitch (normalized units)
     */
    public void sample(float[] input, Random rng, float temperature, float[] step, float[] head) {
        this.gru.sample(input, rng, temperature, step, head);
    }
}
