package ru.zov.implement.features.modules.combat.killaura.type;
import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.Lang;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;

public enum MoveCorrectionMode implements DisplayNamed {
    NONE(Lang.ATTACKAURA_MOVE_CORRECTION_NONE),
    FREE(Lang.ATTACKAURA_MOVE_CORRECTION_FREE),
    TARGET(Lang.ATTACKAURA_MOVE_CORRECTION_TARGET),
    FOCUS(Lang.ATTACKAURA_MOVE_CORRECTION_FOCUS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    MoveCorrectionMode(Translation class254Var) {
        this.displayName = class254Var;
    }
}
