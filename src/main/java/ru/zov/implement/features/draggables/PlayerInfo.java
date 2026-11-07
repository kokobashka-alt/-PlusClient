package ru.zov.implement.features.draggables;

import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.MovingUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.world.ServerUtil;

public class PlayerInfo extends AbstractDraggable {
   public PlayerInfo() {
      super("Player Info", 0, 0, 60, 0, false);
   }

   @Override
   public void drawDraggable(DrawContext context) {
      int offset = PlayerIntersectionUtil.isChat(mc.currentScreen) ? -13 : 0;
      BlockPos blockPos = Objects.requireNonNull(mc.player).getBlockPos();
      FontRenderer font = Fonts.getSize(15);
      this.setY(window.getScaledHeight() + offset);
      String bps = "bps: " + Formatting.RED + MathUtil.round(MovingUtil.getSpeedSqrt(mc.player) * 20.0, 0.1F) + Formatting.RESET + "\n";
      String tps = "tps: " + Formatting.GOLD + MathUtil.round(ServerUtil.TPS, 0.1) + Formatting.RESET + "\n";
      String xyz = "xyz: "
         + Formatting.GREEN
         + blockPos.getX()
         + Formatting.RESET
         + ", "
         + Formatting.GREEN
         + blockPos.getY()
         + Formatting.RESET
         + ", "
         + Formatting.GREEN
         + blockPos.getZ()
         + Formatting.RESET;
      font.drawString(context.getMatrices(), bps + tps + xyz, 3.0, this.getY() - 24, ColorUtil.getText());
   }
}
