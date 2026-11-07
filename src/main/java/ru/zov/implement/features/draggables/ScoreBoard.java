package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class ScoreBoard extends AbstractDraggable {
   private List<ScoreboardEntry> scoreboardEntries = new ArrayList();
   private ScoreboardObjective objective;

   public ScoreBoard() {
      super("Score Board", 10, 100, 100, 120, true);
   }

   public boolean visible() {
      return !this.scoreboardEntries.isEmpty();
   }

   public void tick() {
      this.objective = ((ClientWorld)Objects.requireNonNull(mc.world)).getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      this.scoreboardEntries = mc.world.getScoreboard().getScoreboardEntries(this.objective).stream().sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).toList();
   }

   public void drawDraggable(DrawContext context) {
      if (mc.world != null) {
         org.joml.Matrix3x2fStack matrix = context.getMatrices();
         FontRenderer font = Fonts.getSize(16);
         Text mainText = (Text)(this.objective != null ? this.objective.getDisplayName() : Text.empty());
         MutableText text = Text.empty();
         this.scoreboardEntries.stream().map((entry) -> Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(entry.owner()), entry.name())).filter(Objects::nonNull).filter((s) -> !s.getString().isEmpty()).forEach((mutable) -> text.append(mutable).append("\n"));
         int padding = 3;
         int offsetText = 14;
         int width = (int)Math.max(font.getStringWidth((Text)text) + (float)(padding * 2) + 1.0F, 100.0F);
         blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)offsetText).round(4.0F, 0.0F, 4.0F, 0.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
         blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)((float)(this.getY() + offsetText) - 0.5F), (double)this.getWidth(), (double)(this.getHeight() - offsetText)).quality(40.0F).round(0.0F, 4.0F, 0.0F, 4.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
         font.drawText(matrix, mainText, (double)((int)((float)this.getX() + ((float)this.getWidth() - font.getStringWidth(mainText)) / 2.0F)), (double)((float)(this.getY() + padding) + 1.5F));
         font.drawText(matrix, text, (double)(this.getX() + padding), (double)(this.getY() + offsetText + padding));
         if (this.getX() > mc.getWindow().getScaledWidth() / 2) {
            this.setX(this.getX() + this.getWidth() - width);
         }

         this.setWidth(width);
         this.setHeight((int)((double)font.getStringHeight((Text)text) / 2.16 + (double)offsetText + (double)padding));
      }
   }
}
