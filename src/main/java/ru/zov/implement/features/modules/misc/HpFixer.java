package dev.leg1tka.modules.player;


import dev.leg1tka.core.Module;
import dev.leg1tka.animation.*;
import dev.leg1tka.clickgui.*;
import dev.leg1tka.core.*;
import dev.leg1tka.discord.*;
import dev.leg1tka.hud.*;
import dev.leg1tka.protection.*;
import dev.leg1tka.render.*;
import dev.leg1tka.rotation.*;
import dev.leg1tka.settings.*;
import dev.leg1tka.storage.*;
import dev.leg1tka.utils.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;

public class HpFixer extends Module {
   public static final HpFixer INSTANCE = new HpFixer();
   private static final Pattern hpat = Pattern.compile("(?<!\\d)(\\d+(?:[.,]\\d+)?)(?!\\d)");

   public HpFixer() {
      super(
         "HPFixer",
         "Берет HP из скорборда или ника",
         Category.PLAYER
      );
   }

   public static float getHealth(
      LivingEntity val
   ) {
      if (val
         == null) {
         return 0.0F;
      } else {
         float x2 = Math.max(
            0.0F,
            val.getHealth()
               + val.getAbsorptionAmount()
         );
         if (val instanceof PlayerEntity val2
            && INSTANCE.isEnable()) {
            Float val3 = getScoreboardHealth(
               val2
            );
            if (val3
               != null) {
               return clampHealth(
                  val3,
                  val
               );
            }

            Float val4 = getOverheadHealth(
               val2
            );
            if (val4
               != null) {
               return clampHealth(
                  val4,
                  val
               );
            }

            return clampHealth(
               x2,
               val
            );
         }

         return clampHealth(
            x2,
            val
         );
      }
   }

   public static float getMaxHealth(
      LivingEntity val
   ) {
      return val
            == null
         ? 0.0F
         : val.getMaxHealth();
   }

   private static Float getScoreboardHealth(
      PlayerEntity val
   ) {
      try {
         ScoreboardObjective val2 = val.getScoreboard()
            .getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
         if (val2
            == null) {
            val2 = val.getScoreboard()
               .getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
         }

         if (val2
            == null) {
            return null;
         } else {
            MutableText val3 = ReadableScoreboardScore.getFormattedScore(
               val.getScoreboard()
                  .getScore(
                     val,
                     val2
                  ),
               val2.getNumberFormatOr(
                  StyledNumberFormat.EMPTY
               )
            );
            return parseHealthValue(
               val3.getString()
            );
         }
      } catch (Exception var3) {
         return null;
      }
   }

   private static Float getOverheadHealth(
      PlayerEntity val
   ) {
      if (val.getDisplayName()
         != null) {
         Float val2 = parseHealthValue(
            val.getDisplayName()
               .getString()
         );
         if (val2
            != null) {
            return val2;
         }
      }

      if (val.getScoreboardTeam()
         != null) {
         if (val.getScoreboardTeam()
               .getPrefix()
            != null) {
            Float val3 = parseHealthValue(
               val.getScoreboardTeam()
                  .getPrefix()
                  .getString()
            );
            if (val3
               != null) {
               return val3;
            }
         }

         if (val.getScoreboardTeam()
               .getSuffix()
            != null) {
            Float val4 = parseHealthValue(
               val.getScoreboardTeam()
                  .getSuffix()
                  .getString()
            );
            if (val4
               != null) {
               return val4;
            }
         }
      }

      return null;
   }

   public static Float parseHealthValue(
      String val
   ) {
      if (val
            != null
         && !val.isEmpty()
         )
       {
         String text2 = stripFormatting(
            val
         );
         text2 = text2.replace(
               (char)10084,
               (char)32
            )
            .replace(
               (char)9829,
               (char)32
            )
            .replace(
               (char)9825,
               (char)32
            )
            .replace("❤", " ")
            .replace("♥", " ")
            .replace("HP", " ")
            .replace("hp", " ")
            .replace("хп", " ")
            .replace("Хп", " ")
            .trim();
         Matcher matcher2 = hpat.matcher(
            text2
         );
         Float val2 = null;

         while (
            matcher2.find()
         ) {
            String str = matcher2.group(
                  1
               )
               .replace(
                  (char)44,
                  (char)46
               );

            try {
               val2 = Float.parseFloat(
                  str
               );
            } catch (NumberFormatException var6) {
            }
         }

         return val2;
      } else {
         return null;
      }
   }

   private static String stripFormatting(
      String val
   ) {
      return val
            == null
         ? ""
         : val.replaceAll(
               "§[0-9a-fk-orx]", ""
            )
            .replaceAll("(?i)§x(§[0-9a-f]){6}", "")
            .trim();
   }

   private static float clampHealth(
      float val,
      LivingEntity val2
   ) {
      return Math.max(
         0.0F,
         Math.min(
            val,
            Math.max(
               1.0F,
               val2.getMaxHealth()
                  + val2.getAbsorptionAmount()
            )
         )
      );
   }
}
