package ru.zov.api.repository;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.render.DrawEvent;

public class WayRepository implements QuickImports, QuickLogger {
   public List<Way> wayList = new ArrayList();

   public WayRepository(EventManager eventManager) {
      eventManager.register(this);
   }

   public boolean isEmpty() {
      return this.wayList.isEmpty();
   }

   @Native
   public void addWay(String name, BlockPos pos, String server) {
      this.wayList.add(new Way(name, pos, server));
   }

   public boolean hasWay(String text) {
      return this.wayList.stream().anyMatch((s) -> s.name().equalsIgnoreCase(text));
   }

   @Native
   public void deleteWay(String name) {
      this.wayList.removeIf((macro) -> macro.name().equalsIgnoreCase(name));
   }

   @Native
   public void clearList() {
      if (!this.isEmpty()) {
         this.wayList.clear();
      }

   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (!this.isEmpty() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
         org.joml.Matrix3x2fStack matrix = e.context().getMatrices();
         this.wayList.forEach((way) -> {
            Vec3d wayVec = way.pos().toCenterPos();
            Vec3d vec = ProjectionUtil.worldToScreen(wayVec);
            if (ProjectionUtil.canSee(wayVec) && way.server().equalsIgnoreCase(ServerUtil.server)) {
               String var10000 = way.name();
               String text = var10000 + " - " + MathUtil.round(mc.getEntityRenderDispatcher().camera.getCameraPos().distanceTo(wayVec), (double)0.1F) + "m";
               FontRenderer font = Fonts.getSize(14);
               float height = font.getStringHeight(text) / 4.0F;
               float width = font.getStringWidth(text);
               float padding = 3.0F;
               double x = vec.getX() - (double)(width / 2.0F);
               double y = vec.getY() - (double)(height / 2.0F);
               blur.render(ShapeProperties.create(matrix, x - (double)padding, y - (double)padding, (double)(width + padding * 2.0F), (double)(height + padding * 2.0F)).round(2.0F).softness(1.0F).color(ColorUtil.HALF_BLACK).build());
               font.drawString(matrix, text, x, y, ColorUtil.getText());
            }

         });
      }
   }

   public static record Way(String name, BlockPos pos, String server) {
   }
}
