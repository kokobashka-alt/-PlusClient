package ru.zov.implement.features.modules.combat.killaura.model;
import ru.zov.implement.features.modules.combat.killaura.type.PerformAction;

import java.util.function.BooleanSupplier;

public class DelayStep implements Comparable<DelayStep> {
    private final int delay;
    private final PerformAction action;
    private final BooleanSupplier condition;
    private final int id;

    public DelayStep(int delay, PerformAction action, BooleanSupplier condition, int id) {
        this.delay = delay;
        this.action = action;
        this.condition = condition;
        this.id = id;
    }

    @Override
    public int compareTo(DelayStep o) {
        return Integer.compare(this.delay, o.delay);
    }

    public PerformAction action() {
        return this.action;
    }

    public BooleanSupplier condition() {
        return this.condition;
    }

    public int delay() {
        return this.delay;
    }

    public int priority() {
        return this.id;
    }
}
