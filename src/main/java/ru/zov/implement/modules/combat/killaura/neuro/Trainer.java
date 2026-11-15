package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Trains the Bezier GRU on the phone, inside the client: no Python, no PC. The math is plain double precision;
 * parameters are exported as float32 in the same file the runtime already loads.
 *
 * <p>Loss is negative log-likelihood of the Gaussian head, averaged over both axes and over ticks, exactly like
 * tools/train_bezier.py. The hand-written backward pass is checked against PyTorch autograd by GradCheckTest, so a
 * change here that breaks the math fails the build instead of silently producing bad weights.
 *
 * <p>Training is CPU bound. Call it from a background thread (see {@link TrainingJob}) and keep the game in a menu
 * for the minute or two it takes; do not expect a phone to train and render at the same time without stutter.
 */
public final class Trainer {
    public static final int FEATURES = FeatureVector.COUNT;
    public static final float LABEL_SCALE = 20.0F;
    public static final float LOG_SIGMA_MIN = -4.0F;
    public static final float LOG_SIGMA_MAX = 1.0F;

    private Trainer() {
    }

    /** Hyperparameters. Defaults are sized for a phone: about 15k parameters, a few hundred steps. */
    public static final class Config {
        public int hidden = 64;
        public int layers = 1;
        public int seq = 64;
        public int batch = 16;
        public int steps = 250;
        public int evalEvery = 25;
        public double learningRate = 2.0e-3;
        public double clip = 1.0;
        public long seed = 1337L;
        public int sleepMs = 1;

        public Config validate() {
            this.hidden = clamp(this.hidden, 4, 256);
            this.layers = clamp(this.layers, 1, 3);
            this.seq = clamp(this.seq, 8, 512);
            this.batch = clamp(this.batch, 1, 64);
            this.steps = clamp(this.steps, 10, 100_000);
            this.evalEvery = clamp(this.evalEvery, 1, this.steps);
            if (!(this.learningRate > 0.0) || !Double.isFinite(this.learningRate)) {
                this.learningRate = 2.0e-3;
            }
            if (!(this.clip > 0.0) || !Double.isFinite(this.clip)) {
                this.clip = 1.0;
            }
            this.sleepMs = clamp(this.sleepMs, 0, 50);
            return this;
        }

        private static int clamp(int value, int lo, int hi) {
            return value < lo ? lo : (value > hi ? hi : value);
        }
    }

    public interface Progress {
        void onEval(int step, int total, double trainLoss, double valLoss);
    }

    public static final class Result {
        public final int features;
        public final int hidden;
        public final int layers;
        public final float[] mean;
        public final float[] scale;
        public final float[][] wih;
        public final float[][] whh;
        public final float[][] bih;
        public final float[][] bhh;
        public final float[] headW;
        public final float[] headB;
        public final double trainLoss;
        public final double valLoss;
        public final int steps;

        Result(Net net, double[] mean, double[] scale, double trainLoss, double valLoss, int steps) {
            this.features = net.features;
            this.hidden = net.hidden;
            this.layers = net.layers;
            this.mean = toFloat(mean);
            this.scale = toFloat(scale);
            this.wih = toFloat(net.wih);
            this.whh = toFloat(net.whh);
            this.bih = toFloat(net.bih);
            this.bhh = toFloat(net.bhh);
            this.headW = toFloat(net.headW);
            this.headB = toFloat(net.headB);
            this.trainLoss = trainLoss;
            this.valLoss = valLoss;
            this.steps = steps;
        }

        /** Writes bezier_gru.bin atomically and returns the model that was read back from disk. */
        public Gru save(Path path) throws IOException {
            return Gru.write(path, this.features, this.hidden, this.layers,
                    LABEL_SCALE, LOG_SIGMA_MIN, LOG_SIGMA_MAX,
                    this.mean, this.scale, this.wih, this.whh, this.bih, this.bhh, this.headW, this.headB);
        }
    }

    public static Result train(Dataset data, Config config, Progress progress, BooleanSupplier cancelled) {
        Config cfg = config.validate();
        Dataset.Segment[] all = data.segments();
        if (all.length == 0) {
            throw new IllegalArgumentException("empty dataset");
        }

        // Hold out whole segments: neighbouring ticks are almost identical, a per-tick split would leak.
        int split;
        if (all.length == 1) {
            split = 1;
        } else {
            split = Math.max(1, (int) Math.round(all.length * 0.8));
            if (split >= all.length) {
                split = all.length - 1;
            }
        }
        Dataset.Segment[] trainSegments = Arrays.copyOfRange(all, 0, split);
        Dataset.Segment[] valSegments = split < all.length ? Arrays.copyOfRange(all, split, all.length) : trainSegments;

        double[] mean = new double[FEATURES];
        double[] scale = new double[FEATURES];
        computeStats(trainSegments, mean, scale);

        Random rnd = new Random(cfg.seed);
        Net net = Net.random(cfg, rnd);
        Net best = net.copy();
        double bestVal = Double.POSITIVE_INFINITY;

        Workspace ws = new Workspace(FEATURES, net.hidden, net.layers, cfg.seq);
        Grad grad = new Grad(net);
        Adam adam = new Adam(net);
        double[][] x = new double[cfg.seq][FEATURES];
        double[][] y = new double[cfg.seq][2];

        double trainSum = 0.0;
        int trainCount = 0;
        double lastTrain = Double.NaN;

        for (int step = 1; step <= cfg.steps; step++) {
            if (cancelled.getAsBoolean()) {
                return null;
            }

            grad.zero();
            for (int b = 0; b < cfg.batch; b++) {
                Dataset.Segment segment = trainSegments[rnd.nextInt(trainSegments.length)];
                int len = Math.min(cfg.seq, segment.ticks());
                int start = segment.ticks() > len ? rnd.nextInt(segment.ticks() - len + 1) : 0;
                for (int t = 0; t < len; t++) {
                    for (int i = 0; i < FEATURES; i++) {
                        x[t][i] = segment.x[start + t][i];
                    }
                    y[t][0] = segment.y[start + t][0] / LABEL_SCALE;
                    y[t][1] = segment.y[start + t][1] / LABEL_SCALE;
                }

                forward(net, ws, x, len, mean, scale);
                trainSum += loss(ws.out, y, len);
                trainCount++;
                backward(net, ws, grad, y, len);
            }
            grad.scale(1.0 / cfg.batch);
            adam.step(net, grad, cfg, step);

            if (step % cfg.evalEvery == 0 || step == cfg.steps) {
                lastTrain = trainSum / Math.max(1, trainCount);
                double val = evaluate(net, valSegments, cfg.seq, mean, scale);
                if (val < bestVal) {
                    bestVal = val;
                    best = net.copy();
                }
                if (progress != null) {
                    progress.onEval(step, cfg.steps, lastTrain, val);
                }
                trainSum = 0.0;
                trainCount = 0;
            }

            if (cfg.sleepMs > 0) {
                try {
                    Thread.sleep(cfg.sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        return new Result(best, mean, scale, lastTrain, bestVal, cfg.steps);
    }

    /**
     * Loss and gradients for one sequence, flattened in exactly the file order: per layer w_ih, w_hh, b_ih, b_hh,
     * then head weight, head bias. Index 0 is the loss. Used by GradCheckTest to compare against torch.autograd.
     */
    public static double[] gradientsForTest(Gru model, double[][] x, double[][] y) {
        Gru.Snapshot snapshot = model.snapshot();
        Net net = Net.from(model);
        int len = x.length;
        Workspace ws = new Workspace(net.features, net.hidden, net.layers, len);
        Grad grad = new Grad(net);
        double[] mean = toDouble(snapshot.mean());
        double[] scale = toDouble(snapshot.scale());

        forward(net, ws, x, len, mean, scale);
        double value = loss(ws.out, y, len);
        backward(net, ws, grad, y, len);

        int count = 1;
        for (int l = 0; l < net.layers; l++) {
            count += net.wih[l].length + net.whh[l].length + net.bih[l].length + net.bhh[l].length;
        }
        count += net.headW.length + net.headB.length;

        double[] flat = new double[count];
        int at = 0;
        flat[at++] = value;
        for (int l = 0; l < net.layers; l++) {
            at = copy(grad.wih[l], flat, at);
            at = copy(grad.whh[l], flat, at);
            at = copy(grad.bih[l], flat, at);
            at = copy(grad.bhh[l], flat, at);
        }
        at = copy(grad.headW, flat, at);
        copy(grad.headB, flat, at);
        return flat;
    }

    // --- network ---------------------------------------------------------

    static final class Net {
        final int features;
        final int hidden;
        final int layers;
        final double[][] wih;
        final double[][] whh;
        final double[][] bih;
        final double[][] bhh;
        final double[] headW;
        final double[] headB;

        Net(int features, int hidden, int layers) {
            this.features = features;
            this.hidden = hidden;
            this.layers = layers;
            this.wih = new double[layers][];
            this.whh = new double[layers][];
            this.bih = new double[layers][];
            this.bhh = new double[layers][];
            for (int l = 0; l < layers; l++) {
                int in = l == 0 ? features : hidden;
                this.wih[l] = new double[3 * hidden * in];
                this.whh[l] = new double[3 * hidden * hidden];
                this.bih[l] = new double[3 * hidden];
                this.bhh[l] = new double[3 * hidden];
            }
            this.headW = new double[4 * hidden];
            this.headB = new double[4];
        }

        static Net random(Config cfg, Random rnd) {
            Net net = new Net(FEATURES, cfg.hidden, cfg.layers);
            double k = 1.0 / Math.sqrt(cfg.hidden);
            for (int l = 0; l < net.layers; l++) {
                fill(net.wih[l], rnd, k);
                fill(net.whh[l], rnd, k);
                Arrays.fill(net.bih[l], 0.0);
                Arrays.fill(net.bhh[l], 0.0);
            }
            fill(net.headW, rnd, 0.05);
            Arrays.fill(net.headB, 0.0);
            // Start with sigma of about exp(-2) = 0.135 normalized, i.e. 2.7 degrees per tick.
            net.headB[2] = -2.0;
            net.headB[3] = -2.0;
            return net;
        }

        static Net from(Gru model) {
            Gru.Snapshot s = model.snapshot();
            Net net = new Net(model.features(), model.hidden(), model.layers());
            for (int l = 0; l < net.layers; l++) {
                copyInto(s.wih()[l], net.wih[l]);
                copyInto(s.whh()[l], net.whh[l]);
                copyInto(s.bih()[l], net.bih[l]);
                copyInto(s.bhh()[l], net.bhh[l]);
            }
            copyInto(s.headW(), net.headW);
            copyInto(s.headB(), net.headB);
            return net;
        }

        Net copy() {
            Net copy = new Net(this.features, this.hidden, this.layers);
            for (int l = 0; l < this.layers; l++) {
                System.arraycopy(this.wih[l], 0, copy.wih[l], 0, this.wih[l].length);
                System.arraycopy(this.whh[l], 0, copy.whh[l], 0, this.whh[l].length);
                System.arraycopy(this.bih[l], 0, copy.bih[l], 0, this.bih[l].length);
                System.arraycopy(this.bhh[l], 0, copy.bhh[l], 0, this.bhh[l].length);
            }
            System.arraycopy(this.headW, 0, copy.headW, 0, this.headW.length);
            System.arraycopy(this.headB, 0, copy.headB, 0, this.headB.length);
            return copy;
        }

        private static void fill(double[] target, Random rnd, double bound) {
            for (int i = 0; i < target.length; i++) {
                target[i] = (rnd.nextDouble() * 2.0 - 1.0) * bound;
            }
        }

        private static void copyInto(float[] source, double[] target) {
            for (int i = 0; i < target.length; i++) {
                target[i] = source[i];
            }
        }
    }

    private static final class Workspace {
        final double[][] x0;
        final double[][][] h;
        final double[][][] r;
        final double[][][] z;
        final double[][][] n;
        final double[][][] nh;
        final double[][] out;
        final double[][] dhNext;

        Workspace(int features, int hidden, int layers, int seq) {
            this.x0 = new double[seq][features];
            this.h = new double[layers][seq][hidden];
            this.r = new double[layers][seq][hidden];
            this.z = new double[layers][seq][hidden];
            this.n = new double[layers][seq][hidden];
            this.nh = new double[layers][seq][hidden];
            this.out = new double[seq][4];
            this.dhNext = new double[layers][hidden];
        }
    }

    private static final class Grad {
        final int layers;
        final double[][] wih;
        final double[][] whh;
        final double[][] bih;
        final double[][] bhh;
        final double[] headW;
        final double[] headB;

        Grad(Net net) {
            this.layers = net.layers;
            this.wih = sizes(net.wih);
            this.whh = sizes(net.whh);
            this.bih = sizes(net.bih);
            this.bhh = sizes(net.bhh);
            this.headW = new double[net.headW.length];
            this.headB = new double[net.headB.length];
        }

        private static double[][] sizes(double[][] source) {
            double[][] out = new double[source.length][];
            for (int i = 0; i < source.length; i++) {
                out[i] = new double[source[i].length];
            }
            return out;
        }

        void zero() {
            for (int l = 0; l < this.layers; l++) {
                Arrays.fill(this.wih[l], 0.0);
                Arrays.fill(this.whh[l], 0.0);
                Arrays.fill(this.bih[l], 0.0);
                Arrays.fill(this.bhh[l], 0.0);
            }
            Arrays.fill(this.headW, 0.0);
            Arrays.fill(this.headB, 0.0);
        }

        double norm() {
            double sum = 0.0;
            for (int l = 0; l < this.layers; l++) {
                sum += squares(this.wih[l]) + squares(this.whh[l]) + squares(this.bih[l]) + squares(this.bhh[l]);
            }
            return Math.sqrt(sum + squares(this.headW) + squares(this.headB));
        }

        void scale(double factor) {
            for (int l = 0; l < this.layers; l++) {
                mul(this.wih[l], factor);
                mul(this.whh[l], factor);
                mul(this.bih[l], factor);
                mul(this.bhh[l], factor);
            }
            mul(this.headW, factor);
            mul(this.headB, factor);
        }

        private static double squares(double[] values) {
            double sum = 0.0;
            for (double value : values) {
                sum += value * value;
            }
            return sum;
        }

        private static void mul(double[] values, double factor) {
            for (int i = 0; i < values.length; i++) {
                values[i] *= factor;
            }
        }
    }

    private static final class Adam {
        private final Grad m;
        private final Grad v;

        Adam(Net net) {
            this.m = new Grad(net);
            this.v = new Grad(net);
        }

        void step(Net net, Grad grad, Config cfg, int step) {
            double norm = grad.norm();
            double scale = norm > cfg.clip && norm > 0.0 ? cfg.clip / norm : 1.0;
            double b1 = 0.9;
            double b2 = 0.999;
            double eps = 1.0e-8;
            double c1 = 1.0 - Math.pow(b1, step);
            double c2 = 1.0 - Math.pow(b2, step);

            for (int l = 0; l < net.layers; l++) {
                update(net.wih[l], this.m.wih[l], this.v.wih[l], grad.wih[l], scale, cfg.learningRate, b1, b2, c1, c2, eps);
                update(net.whh[l], this.m.whh[l], this.v.whh[l], grad.whh[l], scale, cfg.learningRate, b1, b2, c1, c2, eps);
                update(net.bih[l], this.m.bih[l], this.v.bih[l], grad.bih[l], scale, cfg.learningRate, b1, b2, c1, c2, eps);
                update(net.bhh[l], this.m.bhh[l], this.v.bhh[l], grad.bhh[l], scale, cfg.learningRate, b1, b2, c1, c2, eps);
            }
            update(net.headW, this.m.headW, this.v.headW, grad.headW, scale, cfg.learningRate, b1, b2, c1, c2, eps);
            update(net.headB, this.m.headB, this.v.headB, grad.headB, scale, cfg.learningRate, b1, b2, c1, c2, eps);
        }

        private static void update(double[] p, double[] m, double[] v, double[] g,
                                   double scale, double lr, double b1, double b2,
                                   double c1, double c2, double eps) {
            for (int i = 0; i < p.length; i++) {
                double grad = g[i] * scale;
                m[i] = b1 * m[i] + (1.0 - b1) * grad;
                v[i] = b2 * v[i] + (1.0 - b2) * grad * grad;
                double mh = m[i] / c1;
                double vh = v[i] / c2;
                p[i] -= lr * mh / (Math.sqrt(vh) + eps);
            }
        }
    }

    // --- math ------------------------------------------------------------

    static void forward(Net net, Workspace ws, double[][] x, int len, double[] mean, double[] scale) {
        for (int t = 0; t < len; t++) {
            for (int i = 0; i < net.features; i++) {
                ws.x0[t][i] = (x[t][i] - mean[i]) / scale[i];
            }
        }
        int hidden = net.hidden;
        for (int t = 0; t < len; t++) {
            for (int l = 0; l < net.layers; l++) {
                int in = l == 0 ? net.features : hidden;
                double[] src = l == 0 ? ws.x0[t] : ws.h[l - 1][t];
                double[] hp = t == 0 ? null : ws.h[l][t - 1];
                double[] rr = ws.r[l][t];
                double[] zz = ws.z[l][t];
                double[] nn = ws.n[l][t];
                double[] nh = ws.nh[l][t];
                double[] hh = ws.h[l][t];
                double[] wih = net.wih[l];
                double[] whh = net.whh[l];
                double[] bih = net.bih[l];
                double[] bhh = net.bhh[l];

                for (int j = 0; j < hidden; j++) {
                    int r = j;
                    int z = hidden + j;
                    int n = 2 * hidden + j;

                    double gr = bih[r] + dot(wih, r * in, src, 0, in) + bhh[r]
                            + (hp == null ? 0.0 : dot(whh, r * hidden, hp, 0, hidden));
                    double gz = bih[z] + dot(wih, z * in, src, 0, in) + bhh[z]
                            + (hp == null ? 0.0 : dot(whh, z * hidden, hp, 0, hidden));
                    double nIn = bih[n] + dot(wih, n * in, src, 0, in);
                    double nHid = bhh[n] + (hp == null ? 0.0 : dot(whh, n * hidden, hp, 0, hidden));

                    double rv = sigmoid(gr);
                    double zv = sigmoid(gz);
                    double nv = Math.tanh(nIn + rv * nHid);

                    rr[j] = rv;
                    zz[j] = zv;
                    nn[j] = nv;
                    nh[j] = nHid;
                    hh[j] = (1.0 - zv) * nv + zv * (hp == null ? 0.0 : hp[j]);
                }
            }

            double[] last = ws.h[net.layers - 1][t];
            for (int k = 0; k < 4; k++) {
                ws.out[t][k] = net.headB[k] + dot(net.headW, k * hidden, last, 0, hidden);
            }
        }
    }

    private static void backward(Net net, Workspace ws, Grad grad, double[][] y, int len) {
        int hidden = net.hidden;
        for (int l = 0; l < net.layers; l++) {
            Arrays.fill(ws.dhNext[l], 0.0);
        }

        double factor = 1.0 / (len * 2.0);
        double[] dh = new double[hidden];
        double[] dhPrev = new double[hidden];
        double[] dX = new double[hidden];
        double[] dOut = new double[4];
        double[] sumHead = new double[hidden];
        double[] upper = null;

        for (int t = len - 1; t >= 0; t--) {
            upper = null;
            for (int l = net.layers - 1; l >= 0; l--) {
                double[] hp = t == 0 ? null : ws.h[l][t - 1];
                System.arraycopy(ws.dhNext[l], 0, dh, 0, hidden);

                if (l == net.layers - 1) {
                    headGrad(ws.out[t], y[t], factor, dOut);
                    double[] last = ws.h[l][t];
                    for (int a = 0; a < 4; a++) {
                        grad.headB[a] += dOut[a];
                        for (int j = 0; j < hidden; j++) {
                            grad.headW[a * hidden + j] += dOut[a] * last[j];
                        }
                    }
                    for (int j = 0; j < hidden; j++) {
                        sumHead[j] = 0.0;
                    }
                    for (int a = 0; a < 4; a++) {
                        for (int j = 0; j < hidden; j++) {
                            sumHead[j] += net.headW[a * hidden + j] * dOut[a];
                        }
                    }
                    for (int j = 0; j < hidden; j++) {
                        dh[j] += sumHead[j];
                    }
                } else if (upper != null) {
                    for (int j = 0; j < hidden; j++) {
                        dh[j] += upper[j];
                    }
                }
                if (l > 0) {
                    // The upper layer's gradient into this layer's input has just been consumed; reuse the buffer.
                    Arrays.fill(dX, 0.0);
                }

                int in = l == 0 ? net.features : hidden;
                double[] src = l == 0 ? ws.x0[t] : ws.h[l - 1][t];
                double[] rr = ws.r[l][t];
                double[] zz = ws.z[l][t];
                double[] nn = ws.n[l][t];
                double[] nh = ws.nh[l][t];
                double[] wih = net.wih[l];
                double[] whh = net.whh[l];
                double[] gBih = grad.bih[l];
                double[] gBhh = grad.bhh[l];
                double[] gWih = grad.wih[l];
                double[] gWhh = grad.whh[l];

                for (int j = 0; j < hidden; j++) {
                    dhPrev[j] = dh[j] * zz[j];
                }
                for (int j = 0; j < hidden; j++) {
                    int r = j;
                    int z = hidden + j;
                    int n = 2 * hidden + j;

                    double dz = dh[j] * ((hp == null ? 0.0 : hp[j]) - nn[j]);
                    double dn = dh[j] * (1.0 - zz[j]);
                    double da = dn * (1.0 - nn[j] * nn[j]);
                    double dnh = da * rr[j];
                    double dgr = da * nh[j] * rr[j] * (1.0 - rr[j]);
                    double dgz = dz * zz[j] * (1.0 - zz[j]);

                    // b_hh is part of the pre-activation even when h_{t-1} is the zero vector at t = 0.
                    gBih[r] += dgr;
                    gBih[z] += dgz;
                    gBih[n] += da;
                    gBhh[r] += dgr;
                    gBhh[z] += dgz;
                    gBhh[n] += dnh;

                    for (int k = 0; k < in; k++) {
                        gWih[r * in + k] += dgr * src[k];
                        gWih[z * in + k] += dgz * src[k];
                        gWih[n * in + k] += da * src[k];
                    }
                    if (hp != null) {
                        for (int k = 0; k < hidden; k++) {
                            gWhh[r * hidden + k] += dgr * hp[k];
                            gWhh[z * hidden + k] += dgz * hp[k];
                            gWhh[n * hidden + k] += dnh * hp[k];
                        }
                        for (int k = 0; k < hidden; k++) {
                            dhPrev[k] += dgr * whh[r * hidden + k]
                                    + dgz * whh[z * hidden + k]
                                    + dnh * whh[n * hidden + k];
                        }
                    }

                    if (l > 0) {
                        for (int k = 0; k < hidden; k++) {
                            dX[k] += dgr * wih[r * in + k]
                                    + dgz * wih[z * in + k]
                                    + da * wih[n * in + k];
                        }
                    }
                }
                System.arraycopy(dhPrev, 0, ws.dhNext[l], 0, hidden);

                upper = l > 0 ? dX : null;
            }
        }
    }

    private static void headGrad(double[] out, double[] target, double factor, double[] dOut) {
        for (int a = 0; a < 2; a++) {
            double mu = out[a];
            double raw = out[2 + a];
            double logSigma = clamp(raw, LOG_SIGMA_MIN, LOG_SIGMA_MAX);
            double sigma = Math.exp(logSigma);
            double err = (mu - target[a]) / sigma;
            dOut[a] = err / sigma * factor;
            dOut[2 + a] = (raw >= LOG_SIGMA_MIN && raw <= LOG_SIGMA_MAX) ? (1.0 - err * err) * factor : 0.0;
        }
    }

    private static double loss(double[][] out, double[][] y, int len) {
        double total = 0.0;
        for (int t = 0; t < len; t++) {
            for (int a = 0; a < 2; a++) {
                double logSigma = clamp(out[t][2 + a], LOG_SIGMA_MIN, LOG_SIGMA_MAX);
                double sigma = Math.exp(logSigma);
                double err = (y[t][a] - out[t][a]) / sigma;
                total += 0.5 * err * err + logSigma;
            }
        }
        return total / (len * 2.0);
    }

    private static double evaluate(Net net, Dataset.Segment[] segments, int seq, double[] mean, double[] scale) {
        Workspace ws = new Workspace(FEATURES, net.hidden, net.layers, seq);
        double[][] x = new double[seq][FEATURES];
        double[][] y = new double[seq][2];
        double total = 0.0;
        int ticks = 0;

        for (Dataset.Segment segment : segments) {
            for (int start = 0; start < segment.ticks(); start += seq) {
                int len = Math.min(seq, segment.ticks() - start);
                if (len < 2) {
                    break;
                }
                for (int t = 0; t < len; t++) {
                    for (int i = 0; i < FEATURES; i++) {
                        x[t][i] = segment.x[start + t][i];
                    }
                    y[t][0] = segment.y[start + t][0] / LABEL_SCALE;
                    y[t][1] = segment.y[start + t][1] / LABEL_SCALE;
                }
                forward(net, ws, x, len, mean, scale);
                total += loss(ws.out, y, len) * len;
                ticks += len;
            }
        }
        return ticks > 0 ? total / ticks : Double.POSITIVE_INFINITY;
    }

    private static void computeStats(Dataset.Segment[] segments, double[] mean, double[] scale) {
        double[] sum = new double[FEATURES];
        double[] sumSq = new double[FEATURES];
        long count = 0;
        for (Dataset.Segment segment : segments) {
            for (float[] row : segment.x) {
                for (int i = 0; i < FEATURES; i++) {
                    double v = row[i];
                    sum[i] += v;
                    sumSq[i] += v * v;
                }
                count++;
            }
        }
        for (int i = 0; i < FEATURES; i++) {
            double m = count > 0 ? sum[i] / count : 0.0;
            double variance = count > 0 ? Math.max(0.0, sumSq[i] / count - m * m) : 1.0;
            mean[i] = m;
            scale[i] = Math.max(Math.sqrt(variance), 0.05);
        }
    }

    private static double dot(double[] w, int wOff, double[] v, int vOff, int n) {
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += w[wOff + i] * v[vOff + i];
        }
        return sum;
    }

    private static double sigmoid(double v) {
        return 1.0 / (1.0 + Math.exp(-v));
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int copy(double[] source, double[] target, int at) {
        System.arraycopy(source, 0, target, at, source.length);
        return at + source.length;
    }

    private static float[] toFloat(double[] values) {
        float[] out = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (float) values[i];
        }
        return out;
    }

    private static float[][] toFloat(double[][] values) {
        float[][] out = new float[values.length][];
        for (int i = 0; i < values.length; i++) {
            out[i] = toFloat(values[i]);
        }
        return out;
    }

    private static double[] toDouble(float[] values) {
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }
}
