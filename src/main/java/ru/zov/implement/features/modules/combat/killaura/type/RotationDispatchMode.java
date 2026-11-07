package ru.zov.implement.features.modules.combat.killaura.type;
import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.Lang;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;

public enum RotationDispatchMode implements DisplayNamed {
    AUTO(Lang.SWAP_METHOD_AUTO),
    VANILLA(Lang.SWAP_METHOD_VANILLA),
    GRIM(Lang.SWAP_METHOD_GRIM),
    DELAYED(Lang.SWAP_METHOD_DELAYED),
    SEQUENTIAL(Lang.SWAP_METHOD_SEQUENTIAL);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    RotationDispatchMode(Translation class254Var) {
        this.displayName = class254Var;
    }
}
