package ru.zov.implement.features.modules.combat.killaura.type;

import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.Lang;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;

public enum AuraRotationMode implements DisplayNamed {
    FUNTIME(Lang.ATTACKAURA_ROTATION_MODE_FUNTIME),
    SPOOKYTIME(Lang.ATTACKAURA_ROTATION_MODE_SPOOKYTIME),
    SMOOTH(Translation.clearText("Smooth")),
    GRIM(Lang.ATTACKAURA_ROTATION_MODE_GRIM),
    HOLYWORLD(Lang.ATTACKAURA_ROTATION_MODE_HOLYWORLD),
    SLOTH_AC(Translation.clearText("SlothAC")),
    BEZIER(Translation.clearText("Bezier")),
    MLS(Translation.clearText("MLS")),
    NEURO(Translation.clearText("Neuro")),
    WHITERISE(Translation.clearText("WhiteRise")),
    SHARD_AC(Translation.clearText("ShardAC")),
    SHARD_AC_AI(Translation.clearText("ShardAC-AI"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    AuraRotationMode(Translation class254Var) {
        this.displayName = class254Var;
    }
}
