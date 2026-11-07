package ru.zov.implement.features.modules.combat.killaura.util;

import ru.zov.implement.features.modules.combat.killaura.model.DelayStep;
import ru.zov.implement.features.modules.combat.killaura.model.TickStep;
import ru.zov.implement.features.modules.combat.killaura.type.PerformAction;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Script executor with the exact tick-step semantics used by Arizona.
 *
 * <p>The original executor walks the whole step list on every update and
 * decrements the currently active step on every list iteration.  This is
 * intentionally different from {@link ActionScheduler}, which treats tick
 * values as absolute scheduler ticks and executes at most one tick step per
 * update.</p>
 */
public final class ArizonaScript extends ActionScheduler {
    private Map<TickStep, Integer> remainingTicks;

    public ArizonaScript() {
        super();
        this.remainingTicks = new IdentityHashMap<>();
    }

    @Override
    public ArizonaScript cleanup() {
        super.cleanup();
        if (this.remainingTicks != null) {
            this.remainingTicks.clear();
        }
        return this;
    }

    @Override
    public ArizonaScript cleanupIfFinished() {
        if (isFinished()) {
            cleanup();
        }
        return this;
    }

    @Override
    public ArizonaScript addStep(
            int delay,
            PerformAction action,
            BooleanSupplier condition,
            int priority) {
        this.scriptSteps.add(
                new DelayStep(delay, action, condition, priority));
        this.scriptSteps.sort(
                Comparator.comparingInt(DelayStep::priority).reversed());
        return this;
    }

    @Override
    public ArizonaScript addTickStep(
            int ticks,
            PerformAction action,
            BooleanSupplier condition,
            int priority) {
        this.scriptTickSteps.add(
                new TickStep(ticks, action, condition, priority));
        this.scriptTickSteps.sort(
                Comparator.comparingInt(TickStep::priority).reversed());
        return this;
    }

    @Override
    public ArizonaScript update() {
        if ((this.scriptSteps.isEmpty() && this.scriptTickSteps.isEmpty()) || this.interrupt) {
            return this;
        }

        this.scriptSteps.forEach(ignored -> {
            if (this.currentStepIndex < this.scriptSteps.size()) {
                var currentStep = this.scriptSteps.get(this.currentStepIndex);
                if (currentStep.condition().getAsBoolean()
                        && this.time.hasElapsed(currentStep.delay())) {
                    currentStep.action().perform();
                    this.currentStepIndex++;
                    resetTime();
                    if (this.loopStrategy.shouldLoop(
                            this.currentStepIndex, this.scriptSteps.size())) {
                        resetStepIndex();
                        this.loopStrategy.onLoop();
                    }
                }
            }
        });

        this.scriptTickSteps.forEach(ignored -> {
            if (this.currentTickStepIndex < this.scriptTickSteps.size()) {
                TickStep currentStep =
                        this.scriptTickSteps.get(this.currentTickStepIndex);
                int ticks = this.remainingTicks.computeIfAbsent(
                        currentStep, step -> step.ticks());
                if (currentStep.condition().getAsBoolean() && ticks <= 0) {
                    currentStep.action().perform();
                    this.currentTickStepIndex++;
                    resetTime();
                    if (this.loopStrategy.shouldLoop(
                            this.currentTickStepIndex,
                            this.scriptTickSteps.size())) {
                        resetStepIndex();
                        this.loopStrategy.onLoop();
                    }
                }
                this.remainingTicks.put(currentStep, ticks - 1);
            }
        });

        this.currentStepIndex =
                Math.min(this.currentStepIndex, this.scriptSteps.size());
        this.currentTickStepIndex =
                Math.min(this.currentTickStepIndex, this.scriptTickSteps.size());
        return this;
    }
}
