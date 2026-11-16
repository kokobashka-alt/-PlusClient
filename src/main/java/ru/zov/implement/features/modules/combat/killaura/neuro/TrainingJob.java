package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runs {@link Trainer} on a daemon background thread and reports progress as chat lines.
 *
 * <p>The game thread must never call {@link Trainer#train} directly: one session takes a minute or two even at phone
 * sized settings. Instead the mode polls {@link #poll()} once per tick and forwards the strings to chat. The model is
 * written atomically to a temporary file, read back through the normal loader, and only then moved over
 * bezier_gru.bin, so a crash or a cancel can never leave a half-written model behind.
 */
public final class TrainingJob implements Runnable {
    private final Path datasetPath;
    private final Path modelPath;
    private final Trainer.Config config;
    private final Queue<String> messages = new ConcurrentLinkedQueue<>();

    private volatile boolean running = true;
    private volatile boolean cancelled;
    private volatile boolean succeeded;
    private volatile Throwable failure;
    private Thread thread;

    public TrainingJob(Path datasetPath, Path modelPath, Trainer.Config config) {
        this.datasetPath = datasetPath;
        this.modelPath = modelPath;
        this.config = config.validate();
    }

    public void start() {
        this.thread = new Thread(this, "BezierTrainer");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.MIN_PRIORITY);
        this.thread.start();
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean succeeded() {
        return this.succeeded;
    }

    public Throwable failure() {
        return this.failure;
    }

    /** Next chat line, or null when the queue is empty. Safe from any thread. */
    public String poll() {
        return this.messages.poll();
    }

    @Override
    public void run() {
        try {
            if (!Files.exists(this.datasetPath)) {
                throw new IOException("нет файла " + this.datasetPath.toAbsolutePath());
            }

            Dataset dataset = Dataset.load(this.datasetPath);
            this.post(String.format(Locale.ROOT, "датасет: %d тиков в %d сегментах",
                    dataset.ticks(), dataset.segments().length));
            if (dataset.ticks() < 2000) {
                this.post("мало данных: лучше 20+ минут записи, иначе модель выучит шум");
            }
            this.post(String.format(Locale.ROOT, "обучение: H=%d, слоёв %d, шагов %d (не выходи из игры, можно в меню)",
                    this.config.hidden, this.config.layers, this.config.steps));

            Trainer.Result result = Trainer.train(dataset, this.config,
                    (step, total, trainLoss, valLoss) -> this.post(String.format(Locale.ROOT,
                            "шаг %d/%d: train %.4f, val %.4f", step, total, trainLoss, valLoss)),
                    () -> this.cancelled);

            if (result == null || this.cancelled) {
                this.post("обучение отменено, модель не изменена");
                return;
            }

            this.backup();
            result.save(this.modelPath);
            this.succeeded = true;
            this.post(String.format(Locale.ROOT, "готово: val %.4f, модель %s",
                    result.valLoss, this.modelPath.toAbsolutePath()));
        } catch (Throwable e) {
            this.failure = e;
            this.post("ошибка обучения: " + e);
        } finally {
            this.running = false;
        }
    }

    private void backup() {
        try {
            if (Files.exists(this.modelPath)) {
                Files.copy(this.modelPath, this.modelPath.resolveSibling(this.modelPath.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // A missing backup is not worth failing an otherwise good training run over.
        }
    }

    private void post(String message) {
        this.messages.add(message);
    }
}
