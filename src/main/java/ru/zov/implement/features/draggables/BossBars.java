package ru.zov.implement.features.draggables;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.BossBar;
import org.joml.Vector4f;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;

public class BossBars extends AbstractDraggable {
   public BossBars() {
      super("Boss Bars", 0, 0, 0, 0, false);
   }

   public boolean visible() {
      return !mc.inGameHud.getBossBarHud().bossBars.isEmpty();
   }

   public void drawDraggable(DrawContext context) {
      this.setX(mc.getWindow().getScaledWidth() / 2);
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      float y = 10.0F;
      float width = 156.0F;
      float height = 3.5F;
      FontRenderer font = Fonts.getSize(18);

      for(ClientBossBar bossInfo : mc.inGameHud.getBossBarHud().bossBars.values()) {
         Vector4f rounds = bossInfo.getPercent() != 1.0F ? new Vector4f(0.0F, 0.0F, height / 2.0F, height / 2.0F) : new Vector4f(height / 2.0F);
         int color = this.getColor(bossInfo.getColor());
         rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() - width / 2.0F), (double)(y + 10.0F), (double)width, (double)height).color(ColorUtil.getRect(0.8F)).round(1.75F).build());
         rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() - width / 2.0F), (double)(y + 10.0F), (double)width, (double)height).color(ColorUtil.multAlpha(color, 0.2F)).round(1.75F).build());
         rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() - width / 2.0F), (double)(y + 10.0F), (double)(width * bossInfo.getPercent()), (double)height).color(ColorUtil.multAlpha(color, 0.8F)).round(rounds).build());
         font.drawText(matrix, bossInfo.getName(), (double)((int)((float)this.getX() - font.getStringWidth(bossInfo.getName()) / 2.0F)), (double)y);
         y += 22.0F;
      }

   }

   public int getColor(BossBar.Color color) {
      int var10000;
      switch (color) {
         case PINK -> var10000 = (new Color(16734900)).getRGB();
         case PURPLE -> var10000 = (new Color(8469759)).getRGB();
         case RED -> var10000 = (new Color(16725815)).getRGB();
         case BLUE -> var10000 = (new Color(41215)).getRGB();
         case GREEN -> var10000 = (new Color(5635925)).getRGB();
         default -> var10000 = (new Color(color.getTextFormat().getColorValue())).getRGB();
      }

      return var10000;
   }
}
