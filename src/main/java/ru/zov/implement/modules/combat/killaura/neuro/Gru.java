package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * GRU inference for the Bezier rotation. Pure Java, no game dependency.
 *
 * <p>File layout, little endian, produced by tools/train_bezier.py:
 * <pre>
 *   u32 magic 'BZGR'      u32 version (1)
 *   u32 features          u32 hidden        u32 layers
 *   f32 label_scale       f32 log_sigma_min f32 log_sigma_max
 *   f32 input_mean[features]   f32 input_scale[features]
 *   per layer: f32 w_ih[3*hidden][inputs]  f32 w_hh[3*hidden][hidden]
 *              f32 b_ih[3*hidden]          f32 b_hh[3*hidden]
 *   head:      f32 w[4][hidden]            f32 b[4]
 * </pre>
 * Gate order inside the 3*hidden rows is [reset, update, new], matching torch.nn.GRU. The hidden part of the new
 * gate is multiplied by the reset gate after adding its bias, which is also PyTorch's order.
 *
 * <p>All arithmetic is float, same as the training side. The head returns mean yaw step, mean pitch step, log sigma
 * yaw, log sigma pitch, in units of {@link #labelScale()} degrees.
 */
public final class Gru {
    private static final int MAGIC = 0x52475A42; // "BZGR" read as little-endian u32
    private static final int VERSION = 1;
    private static final float MAX_ABS_WEIGHT = 128.0F;

    public static final int HEAD = 4;

    private final int features;
    private final int hidden;
    private final int layers;
    private final float labelScale;
    private final float logSigmaMin;
    private final float logSigmaMax;
    private final float[] mean;
    private final float[] scale;
    private final float[][] wih;
    private final float[][] whh;
    private final float[][] bih;
    private final float[][] bhh;
    private final float[] headW;
    private final float[] headB;

    private final float[] state;   // layers * hidden
    private final float[] next;    // hidden
    private final float[] input;   // max(features, hidden)

    private Gru(int features, int hidden, int layers, float labelScale, float logSigmaMin, float logSigmaMax,
                float[] mean, float[] scale, float[][] wih, float[][] whh, float[][] bih, float[][] bhh,
                float[] headW, float[] headB) {
        this.features = features;
        this.hidden = hidden;
        this.layers = layers;
        this.labelScale = labelScale;
        this.logSigmaMin = logSigmaMin;
        this.logSigmaMax = logSigmaMax;
        this.mean = mean;
        this.scale = scale;
        this.wih = wih;
        this.whh = whh;
        this.bih = bih;
        this.bhh = bhh;
        this.headW = headW;
        this.headB = headB;
        this.state = new float[layers * hidden];
        this.next = new float[hidden];
        this.input = new float[Math.max(features, hidden)];
    }

    public static Gru load(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        if (bytes.length < 32) {
            throw new IOException("model is " + bytes.length + " bytes, shorter than the 32 byte header");
        }
        if (buf.getInt() != MAGIC) {
            throw new IOException("not a Bezier GRU model (bad magic)");
        }
        int version = buf.getInt();
        if (version != VERSION) {
            throw new IOException("model version " + version + ", this build reads version " + VERSION);
        }
        int features = buf.getInt();
        int hidden = buf.getInt();
        int layers = buf.getInt();
        if (features < 1 || features > 64) {
            throw new IOException("feature count " + features + " outside 1..64");
        }
        if (hidden < 1 || hidden > 1024) {
            throw new IOException("hidden size " + hidden + " outside 1..1024");
        }
        if (layers < 1 || layers > 4) {
            throw new IOException("layer count " + layers + " outside 1..4");
        }

        float labelScale = buf.getFloat();
        float logSigmaMin = buf.getFloat();
        float logSigmaMax = buf.getFloat();
        if (!Float.isFinite(labelScale) || labelScale <= 0.0F || labelScale > 1000.0F) {
            throw new IOException("label_scale " + labelScale + " is not usable");
        }
        if (!Float.isFinite(logSigmaMin) || !Float.isFinite(logSigmaMax) || logSigmaMin >= logSigmaMax) {
            throw new IOException("bad sigma bounds " + logSigmaMin + ".." + logSigmaMax);
        }

        float[] mean = new float[features];
        float[] scale = new float[features];
        for (int i = 0; i < features; i++) {
            mean[i] = readFinite(buf, "input_mean[" + i + "]");
        }
        for (int i = 0; i < features; i++) {
            float v = readFinite(buf, "input_scale[" + i + "]");
            if (!(v > 1.0E-6F)) {
                throw new IOException("input_scale[" + i + "] = " + v);
            }
            scale[i] = v;
        }

        float[][] wih = new float[layers][];
        float[][] whh = new float[layers][];
        float[][] bih = new float[layers][];
        float[][] bhh = new float[layers][];
        for (int l = 0; l < layers; l++) {
            int in = l == 0 ? features : hidden;
            wih[l] = readFinite(buf, "w_ih[" + l + "]", 3 * hidden * in);
            whh[l] = readFinite(buf, "w_hh[" + l + "]", 3 * hidden * hidden);
            bih[l] = readFinite(buf, "b_ih[" + l + "]", 3 * hidden);
            bhh[l] = readFinite(buf, "b_hh[" + l + "]", 3 * hidden);
        }
        float[] headW = readFinite(buf, "head_w", HEAD * hidden);
        float[] headB = readFinite(buf, "head_b", HEAD);

        if (buf.hasRemaining()) {
            throw new IOException("model has " + buf.remaining() + " trailing bytes");
        }
        return new Gru(features, hidden, layers, labelScale, logSigmaMin, logSigmaMax,
                mean, scale, wih, whh, bih, bhh, headW, headB);
    }

    public int features() {
        return this.features;
    }

    public int hidden() {
        return this.hidden;
    }

    public int layers() {
        return this.layers;
    }

    public float labelScale() {
        return this.labelScale;
    }

    /** Clears the recurrent state. Call on every new target or after a gap. */
    public void reset() {
        java.util.Arrays.fill(this.state, 0.0F);
    }

    /**
     * Deterministic part of the head for one tick: mean yaw, mean pitch, log sigma yaw, log sigma pitch. Advances the
     * recurrent state, so calls must follow tick order.
     */
    public void forward(float[] x, float[] out) {
        if (x.length < this.features) {
            throw new IllegalArgumentException("input needs " + this.features + " values, got " + x.length);
        }
        if (out.length < HEAD) {
            throw new IllegalArgumentException("head output needs " + HEAD + " slots");
        }

        for (int i = 0; i < this.features; i++) {
            float v = x[i];
            if (!Float.isFinite(v)) {
                throw new IllegalArgumentException("feature " + i + " is not finite: " + v);
            }
            this.input[i] = (v - this.mean[i]) / this.scale[i];
        }

        int in = this.features;
        int src = 0;
        for (int l = 0; l < this.layers; l++) {
            float[] wi = this.wih[l];
            float[] wh = this.whh[l];
            float[] bi = this.bih[l];
            float[] bh = this.bhh[l];
            int base = l * this.hidden;

            for (int j = 0; j < this.hidden; j++) {
                int r = j;
                int z = this.hidden + j;
                int n = 2 * this.hidden + j;

                float gr = bi[r] + dot(wi, r * in, this.input, src, in)
                        + bh[r] + dot(wh, r * this.hidden, this.state, base, this.hidden);
                float gz = bi[z] + dot(wi, z * in, this.input, src, in)
                        + bh[z] + dot(wh, z * this.hidden, this.state, base, this.hidden);
                float gnIn = bi[n] + dot(wi, n * in, this.input, src, in);
                float gnHid = bh[n] + dot(wh, n * this.hidden, this.state, base, this.hidden);

                float rr = sigmoid(gr);
                float zz = sigmoid(gz);
                float nn = tanh(gnIn + rr * gnHid);
                this.next[j] = (1.0F - zz) * nn + zz * this.state[base + j];
            }

            System.arraycopy(this.next, 0, this.state, base, this.hidden);
            // The next layer reads this layer's fresh output.
            System.arraycopy(this.next, 0, this.input, 0, this.hidden);
            src = 0;
            in = this.hidden;
        }

        int last = (this.layers - 1) * this.hidden;
        for (int k = 0; k < HEAD; k++) {
            out[k] = this.headB[k] + dot(this.headW, k * this.hidden, this.state, last, this.hidden);
        }
    }

    /**
     * Samples one step for each axis from the Gaussian head at {@code temperature} (1 keeps the trained spread).
     * Returns degrees per tick.
     */
    public void sample(float[] x, Random rng, float temperature, float[] step, float[] head) {
        if (step.length < 2) {
            throw new IllegalArgumentException("step needs 2 slots");
        }
        if (head.length < HEAD) {
            throw new IllegalArgumentException("head needs " + HEAD + " slots");
        }
        forward(x, head);

        float temp = clamp(Float.isFinite(temperature) ? temperature : 1.0F, 0.05F, 4.0F);
        float sy = (float) Math.exp(clamp(head[2], this.logSigmaMin, this.logSigmaMax)) * temp;
        float sp = (float) Math.exp(clamp(head[3], this.logSigmaMin, this.logSigmaMax)) * temp;

        // 40 degrees is above any plausible single-tick human flick; keeps one wild sample from spinning the view.
        step[0] = clamp((head[0] + sy * (float) rng.nextGaussian()) * this.labelScale, -40.0F, 40.0F);
        step[1] = clamp((head[1] + sp * (float) rng.nextGaussian()) * this.labelScale, -40.0F, 40.0F);
    }

    /** Plain copy of everything the trainer or a test may need. */
    public record Snapshot(float[] mean, float[] scale, float[][] wih, float[][] whh,
                           float[][] bih, float[][] bhh, float[] headW, float[] headB) {
    }

    public Snapshot snapshot() {
        return new Snapshot(this.mean.clone(), this.scale.clone(),
                deepCopy(this.wih), deepCopy(this.whh), deepCopy(this.bih), deepCopy(this.bhh),
                this.headW.clone(), this.headB.clone());
    }

    public static long encodedSize(int features, int hidden, int layers) {
        long size = 32L + 8L * features;
        for (int l = 0; l < layers; l++) {
            int in = l == 0 ? features : hidden;
            size += 4L * (3L * hidden * in + 3L * hidden * hidden + 3L * hidden + 3L * hidden);
        }
        return size + 4L * (4L * hidden + 4L);
    }

    /**
     * Writes a model file atomically and reads it back through {@link #load(Path)} before publishing it, so a
     * half-written or inconsistent file can never replace a working model. Returns the loaded model.
     */
    public static Gru write(Path path, int features, int hidden, int layers,
                            float labelScale, float logSigmaMin, float logSigmaMax,
                            float[] mean, float[] scale,
                            float[][] wih, float[][] whh, float[][] bih, float[][] bhh,
                            float[] headW, float[] headB) throws IOException {
        if (features < 1 || hidden < 1 || layers < 1 || layers > 4) {
            throw new IOException("bad shape F=" + features + " H=" + hidden + " L=" + layers);
        }
        if (mean.length != features || scale.length != features) {
            throw new IOException("mean/scale length mismatch");
        }
        if (wih.length != layers || whh.length != layers || bih.length != layers || bhh.length != layers) {
            throw new IOException("weight layer count mismatch");
        }
        long size = encodedSize(features, hidden, layers);
        if (size > 64L * 1024L * 1024L) {
            throw new IOException("model would be " + size + " bytes");
        }

        ByteBuffer buf = ByteBuffer.allocate((int) size).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(MAGIC);
        buf.putInt(VERSION);
        buf.putInt(features);
        buf.putInt(hidden);
        buf.putInt(layers);
        buf.putFloat(labelScale);
        buf.putFloat(logSigmaMin);
        buf.putFloat(logSigmaMax);
        for (float v : mean) {
            buf.putFloat(v);
        }
        for (float v : scale) {
            buf.putFloat(v);
        }
        for (int l = 0; l < layers; l++) {
            int in = l == 0 ? features : hidden;
            if (wih[l].length != 3 * hidden * in || whh[l].length != 3 * hidden * hidden
                    || bih[l].length != 3 * hidden || bhh[l].length != 3 * hidden) {
                throw new IOException("layer " + l + " weight length mismatch");
            }
            putAll(buf, wih[l]);
            putAll(buf, whh[l]);
            putAll(buf, bih[l]);
            putAll(buf, bhh[l]);
        }
        if (headW.length != HEAD * hidden || headB.length != HEAD) {
            throw new IOException("head length mismatch");
        }
        putAll(buf, headW);
        putAll(buf, headB);

        Path absolute = path.toAbsolutePath();
        Path tmp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(tmp, buf.array());
        Gru loaded = load(tmp);
        try {
            Files.move(tmp, absolute, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, absolute, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return loaded;
    }

    private static void putAll(ByteBuffer buf, float[] values) {
        for (float v : values) {
            buf.putFloat(v);
        }
    }

    private static float[][] deepCopy(float[][] source) {
        float[][] copy = new float[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private static float readFinite(ByteBuffer buf, String what) throws IOException {
        if (buf.remaining() < 4) {
            throw new IOException("model truncated at " + what);
        }
        float v = buf.getFloat();
        if (!Float.isFinite(v) || Math.abs(v) > MAX_ABS_WEIGHT) {
            throw new IOException(what + " = " + v);
        }
        return v;
    }

    private static float[] readFinite(ByteBuffer buf, String what, int count) throws IOException {
        if (buf.remaining() < count * 4) {
            throw new IOException("model truncated at " + what + " (needs " + count + " floats)");
        }
        float[] out = new float[count];
        for (int i = 0; i < count; i++) {
            float v = buf.getFloat();
            if (!Float.isFinite(v) || Math.abs(v) > MAX_ABS_WEIGHT) {
                throw new IOException(what + "[" + i + "] = " + v);
            }
            out[i] = v;
        }
        return out;
    }

    private static float dot(float[] w, int wOff, float[] v, int vOff, int n) {
        float sum = 0.0F;
        for (int i = 0; i < n; i++) {
            sum += w[wOff + i] * v[vOff + i];
        }
        return sum;
    }

    private static float sigmoid(float v) {
        return 1.0F / (1.0F + (float) Math.exp(-v));
    }

    private static float tanh(float v) {
        return (float) Math.tanh(v);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
