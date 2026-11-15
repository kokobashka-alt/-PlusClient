package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/** Thin holder around {@link Gru} that exposes model dimensions and sampling. */
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

    /** Predicts and samples one tick. */
    public void sample(float[] input, Random rng, float temperature, float[] step, float[] head) {
        this.gru.sample(input, rng, temperature, step, head);
    }
}
