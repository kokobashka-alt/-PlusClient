package ru.zov.implement.features.draggables.oldcelestial;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import org.joml.Matrix3x2fStack;

public class OldCelestialStats extends OldCelestialDraggable {
   private static final float SIZE = 8.5F;
   private static final float LINE_H = 11.0F;
   private static final float BARS_W = 14.0F;

   public OldCelestialStats() {
      super("OldCelestialStats", 70, 24, 0.997F, 0.993F, true, true);
   }

   @Override
   public boolean visible() {
      this.ensurePlaced();
      return mc.player != null;
   }

   private int ping() {
      if (mc.getNetworkHandler() == null || mc.player == null) {
         return 0;
      }

      PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      return entry == null ? 0 : entry.getLatency();
   }

   private static int litBars(int ping) {
      if (ping <= 0) {
         return 0;
      }

      return ping < 50 ? 4 : ping < 100 ? 3 : ping < 150 ? 2 : ping < 300 ? 1 : 0;
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player == null) {
         return;
      }

      this.clock();
      int ping = this.ping();
      String fps = "FPS: " + mc.getCurrentFps();
      String pingText = "Ping: " + ping;
      float w = Math.max(textWidth(fps, SIZE), textWidth(pingText, SIZE) + 4.0F + BARS_W);
      this.layout(w, LINE_H * 2.0F);

      float x = this.getX();
      float y = this.getY();
      float right = x + this.getWidth();
      Matrix3x2fStack m = context.getMatrices();
      this.text(m, fps, right - textWidth(fps, SIZE), y + LINE_H / 2.0F, SIZE, WHITE);
      this.text(m, pingText, right - BARS_W - 4.0F - textWidth(pingText, SIZE), y + LINE_H * 1.5F, SIZE, WHITE);

      int lit = litBars(ping);
      float barBottom = y + LINE_H * 2.0F - 2.0F;
      for (int i = 0; i < 4; i++) {
         float barH = 3.0F + i * 2.0F;
         int color = i < lit ? GREEN : 0x66FFFFFF;
         this.fill(m, right - BARS_W + i * 3.5F, barBottom - barH, 2.5F, barH, 0.5F, color);
      }
   }
}
