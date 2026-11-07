package ru.zov.implement.features.modules.combat.killaura.type;

import ru.zov.implement.features.modules.combat.killaura.Lang;
import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;

public enum MaxDamageItem implements DisplayNamed {
    PUNISHER_TALISMAN(
            Lang.ATTACKAURA_MAX_DAMAGE_ITEM_PUNISHER,
            "талисман карателя"),
    ARES_SPHERE(
            Lang.ATTACKAURA_MAX_DAMAGE_ITEM_ARES,
            "сфера ареса"),
    RAGE_TALISMAN(
            Lang.ATTACKAURA_MAX_DAMAGE_ITEM_RAGE,
            "талисман ярости"),
    DISCORD_TALISMAN(
            Lang.ATTACKAURA_MAX_DAMAGE_ITEM_DISCORD,
            "талисман раздора"),
    TOTEM(
            Lang.ATTACKAURA_MAX_DAMAGE_ITEM_TOTEM,
            "");

    private final Translation displayName;
    private final String serverName;

    MaxDamageItem(Translation displayName, String serverName) {
        this.displayName = displayName;
        this.serverName = serverName;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public String serverName() {
        return this.serverName;
    }
}
