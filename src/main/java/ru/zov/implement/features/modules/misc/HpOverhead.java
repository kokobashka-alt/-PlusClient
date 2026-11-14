package ru.zov.implement.features.modules.misc;

import net.minecraft.entity.player.PlayerEntity;

final class HpOverhead {
    private HpOverhead() {
    }

    static Float read(PlayerEntity player) {
        if (player.getDisplayName() != null) {
            Float v = HpParser.parse(player.getDisplayName().getString());
            if (v != null) return v;
        }
        var team = player.getScoreboardTeam();
        if (team == null) return null;
        Float prefix = team.getPrefix() == null ? null : HpParser.parse(team.getPrefix().getString());
        if (prefix != null) return prefix;
        return team.getSuffix() == null ? null : HpParser.parse(team.getSuffix().getString());
    }
}
