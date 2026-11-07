package ru.zov.implement.features.modules.combat.killaura.event;
import ru.zov.implement.features.modules.combat.killaura.math.DirectionalInput;

public class MovementUpdateEvent extends CancellableEvent {
    DirectionalInput directionalInput;
    boolean sprint;
    MovementUpdateSource source;

    public MovementUpdateEvent(DirectionalInput class041Var, boolean z, MovementUpdateSource class309Var) {
        this.directionalInput = class041Var;
        this.sprint = z;
        this.source = class309Var;
    }

    public DirectionalInput getDirectionalInput() {
        return this.directionalInput;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public MovementUpdateSource getSource() {
        return this.source;
    }

    public void setSprint(boolean z) {
        this.sprint = z;
    }
}
