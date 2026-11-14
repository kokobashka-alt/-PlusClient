package ru.zov.implement.features.modules.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;

import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.other.Instance;

/**
 * Берёт HP игрока из скорборда (BELOW_NAME/LIST) или из отображаемого ника.
 * Если ничего не найдено — возвращает ванильные getHealth()+absorption.
 */
public class HPFixer extends Module {

    public static HPFixer getInstance() {
        return (HPFixer) Instance.get(HPFixer.class);
    }

    private static final Pattern HEALTH_PATTERN =
            Pattern.compile("(?<!\\d)(\\d+(?:[.,]\\d+)?)(?!\\d)");

    public HPFixer() {
        super("HPFixer", "HP из скорборда/ника для TargetHud", ModuleCategory.MISC);
        // Настроек нет — это утилитарный модуль.
    }

    public static float getHealth(LivingEntity entity) {
        if (entity == null) return 0f;
        float vanilla = Math.max(0f, entity.getHealth() + entity.getAbsorptionAmount());

        if (entity instanceof PlayerEntity player && isHpFixerActive()) {
            Float score = scoreboardHealth(player);
            if (score != null) return clamp(score, entity);

            Float overhead = overheadHealth(player);
            if (overhead != null) return clamp(overhead, entity);
        }
        return clamp(vanilla, entity);
    }

    public static float getMaxHealth(LivingEntity entity) {
        return entity == null ? 0f : entity.getMaxHealth();
    }

    private static boolean isHpFixerActive() {
        HPFixer inst = getInstance();
        return inst != null && inst.isState();
    }

    private static Float scoreboardHealth(PlayerEntity player) {
        try {
            ScoreboardObjective obj = player.getScoreboard()
                    .getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (obj == null) obj = player.getScoreboard()
                    .getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            if (obj == null) return null;

            MutableText formatted = ReadableScoreboardScore.getFormattedScore(
                    player.getScoreboard().getScore(player, obj),
                    obj.getNumberFormatOr(StyledNumberFormat.EMPTY)
            );
            return parseHealthValue(formatted.getString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Float overheadHealth(PlayerEntity player) {
        if (player.getDisplayName() != null) {
            Float v = parseHealthValue(player.getDisplayName().getString());
            if (v != null) return v;
        }
        var team = player.getScoreboardTeam();
        if (team != null) {
            if (team.getPrefix() != null) {
                Float v = parseHealthValue(team.getPrefix().getString());
                if (v != null) return v;
            }
            if (team.getSuffix() != null) {
                Float v = parseHealthValue(team.getSuffix().getString());
                if (v != null) return v;
            }
        }
        return null;
    }

    public static Float parseHealthValue(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        String text = stripFormatting(raw)
                .replace('❤', ' ')
                .replace('♥', ' ')
                .replace("❤", " ")
                .replace("♥", " ")
                .replace("HP", " ")
                .replace("hp", " ")
                .replace("хп", " ")
                .replace("Хп", " ")
                .trim();

        Matcher m = HEALTH_PATTERN.matcher(text);
        Float last = null;
        while (m.find()) {
            String s = m.group(1).replace(',', '.');
            try { last = Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        }
        return last;
    }

    private static String stripFormatting(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-orx]", "")
                .replaceAll("(?i)§x(§[0-9a-f]){6}", "")
                .trim();
    }

    private static float clamp(float v, LivingEntity e) {
        return Math.max(0f, Math.min(v,
                Math.max(1f, e.getMaxHealth() + e.getAbsorptionAmount())));
    }
}
