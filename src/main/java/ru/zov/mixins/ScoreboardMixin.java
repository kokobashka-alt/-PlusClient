package ru.zov.mixins;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Scoreboard.class})
public abstract class ScoreboardMixin {
   @Shadow
   public abstract @Nullable Team getScoreHolderTeam(String var1);

   @Inject(
      method = {"removeScoreHolderFromTeam"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRemoveScoreHolderFromTeam(String playerName, Team team, CallbackInfo ci) {
      Team currentTeam = this.getScoreHolderTeam(playerName);
      if (currentTeam != team) {
         ci.cancel();
      }

   }
}
