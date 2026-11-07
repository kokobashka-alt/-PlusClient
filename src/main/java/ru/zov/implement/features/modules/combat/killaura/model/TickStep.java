package ru.zov.implement.features.modules.combat.killaura.model;
import ru.zov.implement.features.modules.combat.killaura.type.PerformAction;

import java.util.function.BooleanSupplier;

public class TickStep implements Comparable<TickStep> {
    private final int ticks;
    private final PerformAction action;
    private final BooleanSupplier condition;
    private final int id;

    public TickStep(int ticks, PerformAction action, BooleanSupplier condition, int id) {
        this.ticks = ticks;
        this.action = action;
        this.condition = condition;
        this.id = id;
    }

    @Override
    public int compareTo(TickStep o) {
        return Integer.compare(this.ticks, o.ticks);
    }

    public PerformAction action() {
        return this.action;
    }

    public BooleanSupplier condition() {
        return this.condition;
    }

    public int ticks() {
        return this.ticks;
    }

    public int priority() {
        return this.id;
    }
}
