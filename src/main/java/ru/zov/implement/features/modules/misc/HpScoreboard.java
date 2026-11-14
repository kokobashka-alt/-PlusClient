package ru.zov.implement.features.modules.misc;

import static ru.zov.common.QuickImports.mc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;

final class HpScoreboard {
    private HpScoreboard() {
    }

    static Float read(PlayerEntity player) {
        if (mc.world == null) return null;
        try {
            Scoreboard board = mc.world.getScoreboard();
            ScoreboardObjective obj = board.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (obj == null) obj = board.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            if (obj == null) return null;
            MutableText text = ReadableScoreboardScore.getFormattedScore(
                    board.getScore(player, obj), obj.getNumberFormatOr(StyledNumberFormat.EMPTY));
            return HpParser.parse(text.getString());
        } catch (Exception e) {
            return null;
        }
    }
}
