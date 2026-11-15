package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Appends training rows to a CSV file. One row per tick: the raw inputs plus the rotation step the player actually
 * performed that tick. tools/train_bezier.py turns the raw columns into features, so the CSV stays readable and the
 * feature code has exactly one definition on each side.
 *
 * <p>Recording is meant for normal play with the killaura rotation off (or on Human): the labels are whatever the
 * player did, so recording while Bezier drives the aim would only teach the model to copy itself.
 */
public final class BezierRecorder {
    public static final String HEADER = "segment,tick,cur_yaw,cur_pitch,tgt_yaw,tgt_pitch,distance,height_delta,"
            + "tgt_vx,tgt_vz,player_vx,player_vy,player_vz,on_ground,ticks_since_attack,"
            + "prev_step_yaw,prev_step_pitch,label_dyaw,label_dpitch";

    private static final int FLUSH_ROWS = 128;

    private final Path file;
    private BufferedWriter writer;
    private int rows;
    private int segment;
    private int tick;
    private int trackedId;
    private int idleTicks;
    private boolean primed;
    private float lastYaw;
    private float lastPitch;
    private String error = "";

    public BezierRecorder(Path file) {
        this.file = file;
    }

    public boolean isRecording() {
        return this.writer != null;
    }

    public String lastError() {
        return this.error;
    }

    /** Starts a new file, truncating an existing one. Returns false when the file cannot be opened. */
    public boolean start() {
        stop();
        try {
            Path parent = this.file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8);
            this.writer.write(HEADER);
            this.writer.newLine();
            this.rows = 0;
            this.segment = 0;
            this.tick = 0;
            this.trackedId = Integer.MIN_VALUE;
            this.idleTicks = 100;
            this.primed = false;
            this.error = "";
            return true;
        } catch (IOException e) {
            this.writer = null;
            this.error = e.toString();
            return false;
        }
    }

    public void stop() {
        if (this.writer == null) {
            return;
        }
        try {
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            this.error = e.toString();
        } finally {
            this.writer = null;
        }
    }

    /**
     * Adds one tick. {@code playerYaw}/{@code playerPitch} must be read after the mouse input for that tick and before
     * the cheat adds its own rotation, otherwise the label contains the cheat's movement.
     */
    public void tick(int targetId, float playerYaw, float playerPitch, RawSample sample) {
        if (this.writer == null) {
            return;
        }
        // No target this tick: drop the pending pair so the next label is not taken across the gap.
        if (targetId < 0 || sample == null) {
            this.primed = false;
            this.trackedId = Integer.MIN_VALUE;
            return;
        }
        if (targetId != this.trackedId) {
            this.segment++;
            this.trackedId = targetId;
            this.primed = false;
            this.idleTicks = 0;
        }
        if (!this.primed) {
            this.lastYaw = playerYaw;
            this.lastPitch = playerPitch;
            this.primed = true;
            return;
        }

        float labelYaw = FeatureVector.wrapDegrees(playerYaw - this.lastYaw);
        float labelPitch = playerPitch - this.lastPitch;
        this.lastYaw = playerYaw;
        this.lastPitch = playerPitch;

        // A jump that large is a teleport or a respawn, not aim.
        if (Math.abs(labelYaw) > 90.0F || Math.abs(labelPitch) > 90.0F) {
            this.primed = false;
            return;
        }

        StringBuilder line = new StringBuilder(192);
        line.append(this.segment).append(',')
                .append(this.tick++).append(',')
                .append(playerYaw).append(',')
                .append(playerPitch).append(',')
                .append(sample.targetYaw()).append(',')
                .append(sample.targetPitch()).append(',')
                .append(sample.distance()).append(',')
                .append(sample.heightDelta()).append(',')
                .append(sample.targetVx()).append(',')
                .append(sample.targetVz()).append(',')
                .append(sample.playerVx()).append(',')
                .append(sample.playerVy()).append(',')
                .append(sample.playerVz()).append(',')
                .append(sample.onGround() ? 1 : 0).append(',')
                .append(sample.ticksSinceAttack()).append(',')
                .append(sample.prevStepYaw()).append(',')
                .append(sample.prevStepPitch()).append(',')
                .append(labelYaw).append(',')
                .append(labelPitch);

        try {
            this.writer.write(line.toString());
            this.writer.newLine();
            if (++this.rows % FLUSH_ROWS == 0) {
                this.writer.flush();
            }
        } catch (IOException e) {
            this.error = e.toString();
            stop();
        }
    }
}
