package ru.zov.implement.features.modules.combat.killaura.type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zov.implement.features.modules.combat.killaura.type.Mc;
import ru.zov.implement.features.modules.combat.killaura.math.Rotation;
import ru.zov.implement.features.modules.combat.killaura.neuro.BezierRecorder;
import ru.zov.implement.features.modules.combat.killaura.neuro.HumanStep;
import ru.zov.implement.features.modules.combat.killaura.neuro.Quantizer;
import ru.zov.implement.features.modules.combat.killaura.neuro.RawSample;
import ru.zov.implement.features.modules.combat.killaura.neuro.Trainer;
import ru.zov.implement.features.modules.combat.killaura.neuro.TrainingJob;
import ru.zov.implement.features.modules.combat.killaura.util.MathUtil;

/**
 * Neural rotation: a GRU trained on your own fights, either by the built-in trainer (Trainer/TrainingJob, runs in
 * the client on a background thread) or by tools/train_bezier.py. The model predicts the yaw and pitch step of
 * every tick as a Gaussian and one sample is taken from it. The step is snapped to the mouse sensitivity grid.
 *
 * <p>Inputs are built by {@link BezierFeatures}, the same code the recorder uses. Weights are read from
 * bezier_gru.bin in the game directory and reloaded when the file changes. Without a valid file, and for the
 * return-to-camera phase ({@code entity == null}), a damped human-like pursuit drives the rotation.
 *
 * <p>File format and validation live in {@link ru.zov.implement.features.modules.combat.killaura.neuro.Gru}.
 * Nothing here is known to bypass any anticheat; that has to be measured on the server you play.
 */
public class BezierRotationMode extends RotationMode {
    private static final Path MODEL_FILE = Path.of("bezier_gru.bin");
    private static final Path DATASET_FILE = Path.of("bezier_dataset.csv");
    private static final int RECHECK_CALLS = 200;
    private static final float TEMPERATURE = 1.0F;
    private static final float PITCH_LIMIT = 89.0F;

    private final HumanStep human = new HumanStep();
    private final BezierFeatures features = new BezierFeatures();
    private final BezierRecorder recorder = new BezierRecorder(DATASET_FILE);
    private final Quantizer quantizer = new Quantizer();
    private final Random rng = new Random();
    private final float[] input = new float[BezierFeatures.COUNT];
    private final float[] headOut = new float[4];
    private final float[] step = new float[2];
    private final float[] humanWant = new float[2];

    private BezierGruModel model;
    private TrainingJob training;
    private long modelStamp = Long.MIN_VALUE;
    private int calls;
    private int nextCheck;
    private String lastNotice = "";
    private Entity tracked;
    private float prevYaw;
    private float prevPitch;

    public BezierRotationMode() {
        super("Bezier");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        double gcd = MathUtil.computeGcd();
        if (player == null || current == null || target == null || !(gcd > 1.0E-4)) {
            return current;
        }
        if (!Float.isFinite(current.getYaw()) || !Float.isFinite(current.getPitch())) {
            return current;
        }

        this.drainTraining(player);
        this.calls++;
        this.refreshModel(player);
        this.features.tick();

        float nowYaw = current.getYaw();
        float nowPitch = current.getPitch();
        LivingEntity living = entity instanceof LivingEntity le ? le : null;

        if (this.recorder.isRecording()) {
            if (living == null) {
                this.recorder.tick(-1, player.getYaw(), player.getPitch(), null);
            } else {
                RawSample sample = this.features.sample(nowYaw, nowPitch, target.getYaw(), target.getPitch(),
                        player, living, this.prevYaw, this.prevPitch);
                // Labels come from the player's own aim, so record with the rotation off or on Human.
                this.recorder.tick(living.getId(), player.getYaw(), player.getPitch(), sample);
            }
        }

        if (living == null || this.model == null) {
            this.endSegment();
            return this.fallback(current, target, gcd);
        }

        if (this.tracked != entity) {
            this.startSegment(entity);
        }

        RawSample sample = this.features.sample(nowYaw, nowPitch, target.getYaw(), target.getPitch(),
                player, living, this.prevYaw, this.prevPitch);
        if (!this.features.fill(this.input, sample)) {
            this.endSegment();
            return this.fallback(current, target, gcd);
        }

        this.model.sample(this.input, this.rng, TEMPERATURE, this.step, this.headOut);
        float dy = this.quantizer.step(0, this.step[0], gcd);
        float dp = this.quantizer.step(1, this.step[1], gcd);
        this.prevYaw = dy;
        this.prevPitch = dp;
        return this.apply(current, dy, dp);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    /**
     * Starts or stops collecting training rows into bezier_dataset.csv. Call it from your keybind while playing
     * normally; the file is truncated on every start.
     */
    public void toggleRecording() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (this.recorder.isRecording()) {
            this.recorder.stop();
            this.notice(player, "[Bezier] запись остановлена: " + DATASET_FILE.toAbsolutePath());
        } else if (this.recorder.start()) {
            this.notice(player, "[Bezier] запись в " + DATASET_FILE.toAbsolutePath()
                    + " (ротацию выключи, иначе модель учится сама у себя)");
        } else {
            this.notice(player, "[Bezier] не удалось открыть датасет: " + this.recorder.lastError());
        }
    }

    /**
     * Starts or cancels training of bezier_gru.bin from bezier_dataset.csv. Call it from a keybind; the work happens
     * on a background daemon thread and progress is printed to chat. The trained model is reloaded automatically.
     */
    public void toggleTraining() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        TrainingJob job = this.training;
        if (job != null && job.isRunning()) {
            job.cancel();
            this.notice(player, "[Bezier] обучение отменяется");
            return;
        }
        this.startTraining(new Trainer.Config());
    }

    /** Same as {@link #toggleTraining()} but with explicit hyperparameters, e.g. from a settings screen. */
    public void startTraining(Trainer.Config config) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (this.training != null) {
            // A finished job may still hold the final messages and the pending model reload.
            this.drainTraining(player);
        }
        if (this.training != null && this.training.isRunning()) {
            this.notice(player, "[Bezier] обучение уже идёт");
            return;
        }
        if (this.recorder.isRecording()) {
            this.notice(player, "[Bezier] сначала останови запись: модель не должна учиться на своих же шагах");
            return;
        }
        if (!Files.exists(DATASET_FILE)) {
            this.notice(player, "[Bezier] нет датасета " + DATASET_FILE.toAbsolutePath());
            return;
        }
        TrainingJob job = new TrainingJob(DATASET_FILE, MODEL_FILE, config);
        this.training = job;
        job.start();
        this.notice(player, "[Bezier] обучение запущено на фоне, прогресс будет в чате");
    }

    public boolean isTraining() {
        return this.training != null && this.training.isRunning();
    }

    private void drainTraining(ClientPlayerEntity player) {
        TrainingJob job = this.training;
        if (job == null) {
            return;
        }
        String message;
        while ((message = job.poll()) != null) {
            this.notice(player, "[Bezier] " + message);
        }
        if (job.isRunning()) {
            return;
        }
        this.training = null;
        if (job.succeeded()) {
            this.model = null;
            this.modelStamp = Long.MIN_VALUE;
            this.nextCheck = 0;
            this.notice(player, "[Bezier] новая модель будет загружена в следующем тике");
        }
    }

    private Rotation fallback(Rotation current, Rotation target, double gcd) {        this.human.desired(current.getYaw(), current.getPitch(), target.getYaw(), target.getPitch(), this.humanWant);
        float dy = this.quantizer.step(0, this.humanWant[0], gcd);
        float dp = this.quantizer.step(1, this.humanWant[1], gcd);
        return this.apply(current, dy, dp);
    }

    private Rotation apply(Rotation current, float dyaw, float dpitch) {
        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw() + dyaw),
                MathHelper.clamp(current.getPitch() + dpitch, -PITCH_LIMIT, PITCH_LIMIT));
    }

    private void startSegment(Entity entity) {
        this.tracked = entity;
        this.features.reset();
        this.human.reset();
        this.quantizer.reset();
        if (this.model != null) {
            this.model.reset();
        }
        this.prevYaw = 0.0F;
        this.prevPitch = 0.0F;
    }

    private void endSegment() {
        this.tracked = null;
        this.features.reset();
        this.human.reset();
        this.quantizer.reset();
        if (this.model != null) {
            this.model.reset();
        }
        this.prevYaw = 0.0F;
        this.prevPitch = 0.0F;
    }

    private void refreshModel(ClientPlayerEntity player) {
        if (this.calls < this.nextCheck) {
            return;
        }

        this.nextCheck = this.calls + RECHECK_CALLS;
        try {
            if (!Files.exists(MODEL_FILE)) {
                this.model = null;
                this.modelStamp = Long.MIN_VALUE;
                this.notice(player, "[Bezier] нет файла " + MODEL_FILE.toAbsolutePath() + ", работает режим Human");
                return;
            }

            long stamp = Files.getLastModifiedTime(MODEL_FILE).toMillis();
            if (this.model != null && stamp == this.modelStamp) {
                return;
            }

            BezierGruModel loaded = BezierGruModel.load(MODEL_FILE);
            if (loaded.features != BezierFeatures.COUNT) {
                throw new IOException("в модели " + loaded.features + " входов, режим ожидает " + BezierFeatures.COUNT);
            }

            this.model = loaded;
            this.modelStamp = stamp;
            this.endSegment();
            this.notice(player, "[Bezier] модель загружена: H=" + loaded.hidden + ", слоёв " + loaded.layers);
        } catch (IOException e) {
            this.model = null;
            this.modelStamp = Long.MIN_VALUE;
            this.notice(player, "[Bezier] не удалось загрузить модель (" + e.getMessage() + "), работает режим Human");
        }
    }

    private void notice(ClientPlayerEntity player, String message) {
        if (player == null || message.equals(this.lastNotice)) {
            return;
        }
        this.lastNotice = message;
        player.sendMessage(Text.literal(message), false);
    }
}
