package ru.zov.implement.features.modules.combat.killaura.internal;
import ru.zov.implement.features.modules.combat.killaura.type.EventPhase;

public class EventPhaseSwitchMap {
    public static final int[] phaseOrdinals = new int[EventPhase.values().length];

    static {
        try {
            phaseOrdinals[EventPhase.PRE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            phaseOrdinals[EventPhase.POST.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
    }
}
