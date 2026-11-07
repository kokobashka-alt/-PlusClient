package ru.zov.implement.features.modules.combat.killaura.type;

public class CountLoopStrategy extends LoopStrategy {
    public int maxLoops;
    public int currentLoops = 0;

    public CountLoopStrategy() {
        this.maxLoops = 1;
    }

    public CountLoopStrategy(int maxLoops) {
        this.maxLoops = maxLoops;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void onLoop() {
        this.currentLoops++;
    }

    @Override
    public boolean shouldLoop(int currentStepIndex, int totalSteps) {
        return currentStepIndex >= totalSteps && this.currentLoops < this.maxLoops - 1;
    }
}
