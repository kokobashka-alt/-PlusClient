package ru.zov.implement.features.modules.combat.killaura.neuro;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads bezier_dataset.csv into segments of features and labels. The features come from {@link FeatureVector}, the
 * same code the live rotation uses, so training never sees a second, drifting definition.
 *
 * <p>Rows are grouped by the {@code segment} column; one segment is one continuous tracking of one target. Bad rows
 * (non-finite values, negative distance) are dropped without breaking the segment history.
 */
public final class Dataset {
    public static final int MAX_ROWS = 500_000;

    private static final String[] REQUIRED = {
            "segment", "cur_yaw", "cur_pitch", "tgt_yaw", "tgt_pitch", "distance", "height_delta",
            "tgt_vx", "tgt_vz", "player_vx", "player_vy", "player_vz", "on_ground",
            "ticks_since_attack", "prev_step_yaw", "prev_step_pitch", "label_dyaw", "label_dpitch",
    };

    public static final class Segment {
        public final float[][] x;
        public final float[][] y;

        Segment(float[][] x, float[][] y) {
            this.x = x;
            this.y = y;
        }

        public int ticks() {
            return this.x.length;
        }
    }

    private final Segment[] segments;
    private final int ticks;

    private Dataset(Segment[] segments) {
        this.segments = segments;
        int total = 0;
        for (Segment segment : segments) {
            total += segment.ticks();
        }
        this.ticks = total;
    }

    public Segment[] segments() {
        return this.segments;
    }

    public int ticks() {
        return this.ticks;
    }

    public static Dataset load(Path path) throws IOException {
        Map<String, Integer> columns = new HashMap<>();
        List<float[]> features = new ArrayList<>();
        List<float[]> labels = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();
        FeatureVector vector = new FeatureVector();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                throw new IOException("dataset is empty");
            }
            String[] names = header.split(",");
            for (int i = 0; i < names.length; i++) {
                columns.put(names[i].trim(), i);
            }
            for (String required : REQUIRED) {
                if (!columns.containsKey(required)) {
                    throw new IOException("dataset has no column " + required);
                }
            }

            int currentSegment = Integer.MIN_VALUE;
            int rows = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (++rows > MAX_ROWS) {
                    throw new IOException("dataset has more than " + MAX_ROWS + " rows");
                }
                String[] parts = line.split(",");
                if (parts.length < columns.size()) {
                    continue;
                }

                int segment = Integer.parseInt(cell(parts, columns, "segment"));
                float labelYaw = Float.parseFloat(cell(parts, columns, "label_dyaw"));
                float labelPitch = Float.parseFloat(cell(parts, columns, "label_dpitch"));
                if (!Float.isFinite(labelYaw) || !Float.isFinite(labelPitch)) {
                    continue;
                }

                if (segment != currentSegment) {
                    flush(segments, features, labels);
                    vector.reset();
                    currentSegment = segment;
                }

                RawSample sample = new RawSample(
                        parse(parts, columns, "cur_yaw"),
                        parse(parts, columns, "cur_pitch"),
                        parse(parts, columns, "tgt_yaw"),
                        parse(parts, columns, "tgt_pitch"),
                        parse(parts, columns, "distance"),
                        parse(parts, columns, "height_delta"),
                        parse(parts, columns, "tgt_vx"),
                        parse(parts, columns, "tgt_vz"),
                        parse(parts, columns, "player_vx"),
                        parse(parts, columns, "player_vy"),
                        parse(parts, columns, "player_vz"),
                        Integer.parseInt(cell(parts, columns, "on_ground")) != 0,
                        parse(parts, columns, "ticks_since_attack"),
                        parse(parts, columns, "prev_step_yaw"),
                        parse(parts, columns, "prev_step_pitch"));

                float[] featureRow = new float[FeatureVector.COUNT];
                if (!vector.fill(sample, featureRow)) {
                    continue;
                }
                features.add(featureRow);
                labels.add(new float[]{labelYaw, labelPitch});
            }
            flush(segments, features, labels);
        }

        if (segments.isEmpty()) {
            throw new IOException("dataset has no usable ticks");
        }
        return new Dataset(segments.toArray(new Segment[0]));
    }

    private static void flush(List<Segment> segments, List<float[]> features, List<float[]> labels) {
        if (features.size() >= 2) {
            segments.add(new Segment(features.toArray(new float[0][]), labels.toArray(new float[0][])));
        }
        features.clear();
        labels.clear();
    }

    private static String cell(String[] parts, Map<String, Integer> columns, String name) {
        return parts[columns.get(name)].trim();
    }

    private static float parse(String[] parts, Map<String, Integer> columns, String name) {
        return Float.parseFloat(cell(parts, columns, name));
    }
}
