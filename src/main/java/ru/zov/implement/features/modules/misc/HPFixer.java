package ru.zov.implement.features.modules.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
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
    
