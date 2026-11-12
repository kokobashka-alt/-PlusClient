package ru.zov.implement.features.draggables.celestial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ru.zov.api.feature.module.Module;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.implement.features.modules.render.CelestialHud;

public class CelestialArrayList extends CelestialDraggable {
   private static final float ROW_HEIGHT = 11.0F;
   private static final float TEXT_SIZE = 8.5F;

   private static final class ArrEntry {
      final Module module;
      float t;
      boolean present;
      String name;
      float w;

      ArrEntry(Module module) {
         this.module = module;
      }
   }

   private final List<ArrEntry> entries = new ArrayList<>();
   private float edge = Float.NaN;

   public CelestialArrayList() {
      super("CelestialArrayList", 100000, 1, 80, (int) ROW_HEIGHT);
   }

   private float colorPosition(float yy, float total) {
      float u = yy / Math.max(60.0F, total);
      return clamp01(u * 0.95F + 0.06F * (float) Math.sin(this.time * 1.2F - yy * 0.05F));
   }

   @Override
   public void drawDraggable(DrawContext context) {
      CelestialHud hud = CelestialHud.getInstance();
      if (hud == null) {
         return;
      }

      this.clock();
      for (ArrEntry entry : this.entries) {
         entry.present = false;
      }

      for (Module module : main.getModuleProvider().getModules()) {
         if (!module.isState() || module == hud) {
            continue;
         }

         ArrEntry found = null;
         for (ArrEntry entry : this.entries) {
            if (entry.module == module) {
               found = entry;
               break;
            }
         }

         if (found == null) {
            found = new ArrEntry(module);
            this.entries.add(found);
         }

         found.present = true;
      }

      for (Iterator<ArrEntry> it = this.entries.iterator(); it.hasNext(); ) {
         ArrEntry entry = it.next();
         entry.t = this.lin(entry.t, entry.present ? 1.0F : 0.0F, 0.3F);
         if (!entry.present && entry.t <= 0.0F) {
            it.remove();
            continue;
         }

         String name = entry.module.getVisibleName();
         entry.name = hud.arrayLower.isValue() ? name.toLowerCase() : name;
         entry.w = textWidth(entry.name, TEXT_SIZE) + 9.0F;
      }

      if (this.entries.isEmpty()) {
         return;
      }

      this.entries.sort((a, b) -> Float.compare(b.w, a.w));
      int n = this.entries.size();
      float[] ys = new float[n];
      float[] hs = new float[n];
      float y = 0.0F;
      float maxW = 0.0F;
      for (int i = 0; i < n; i++) {
         ArrEntry entry = this.entries.get(i);
         ys[i] = y;
         hs[i] = ROW_HEIGHT * easeOutCubic(entry.t);
         y += hs[i];
         maxW = Math.max(maxW, entry.w);
      }

      float total = y;
      float screenW = window.getScaledWidth();
      if (this.isDragging() || Float.isNaN(this.edge)) {
         boolean rightSide = this.getX() + maxW / 2.0F > screenW / 2.0F;
         this.edge = rightSide ? this.getX() + maxW : this.getX();
      }

      boolean left = this.edge < screenW / 2.0F;
      float origin = left ? this.edge : this.edge - maxW;
      if (!this.isDragging()) {
         this.setX((int) origin);
      }

      this.setWidth((int) Math.ceil(maxW));
      this.setHeight((int) Math.ceil(total));
      float top = this.getY();
      Matrix3x2fStack m = context.getMatrices();

      if (hud.arrayGlow.isValue()) {
         for (int i = 0; i < n; i++) {
            ArrEntry entry = this.entries.get(i);
            if (hs[i] < 0.5F) {
               continue;
            }

            int color = gcol(this.colorPosition(ys[i] + hs[i] / 2.0F, total));
            float rx = left ? this.edge : this.edge - entry.w;
            this.glow(m, rx, top + ys[i], entry.w, hs[i], 0.0F, withAlpha(color, 0x78 / 255.0F * easeOutCubic(entry.t)));
         }
      }

      for (int i = 0; i < n; i++) {
         ArrEntry entry = this.entries.get(i);
         if (hs[i] < 0.3F) {
            continue;
         }

         float rx = left ? this.edge : this.edge - entry.w;
         this.fill(m, rx, top + ys[i], entry.w, hs[i] + 0.6F, 0.0F, 0xFF07070A);
      }

      for (int i = 0; i < n; i++) {
         ArrEntry entry = this.entries.get(i);
         float e = easeOutCubic(entry.t);
         if (e < 0.35F) {
            continue;
         }

         int color = gcol(this.colorPosition(ys[i] + hs[i] / 2.0F, total));
         float rx = left ? this.edge : this.edge - entry.w;
         this.text(m, entry.name, rx + 4.5F, top + ys[i] + hs[i] / 2.0F, TEXT_SIZE, ColorUtil.multAlpha(color, clamp01((e - 0.35F) / 0.65F)));
      }
   }
}
