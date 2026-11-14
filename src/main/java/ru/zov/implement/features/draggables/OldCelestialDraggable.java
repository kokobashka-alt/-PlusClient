package ru.zov.implement.features.draggables.oldcelestial;

import ru.zov.implement.features.draggables.celestial.CelestialDraggable;

/**
 * Base for the OldCelestial HUD elements. Reuses the text/rectangle helpers of {@link CelestialDraggable} and adds
 * a red palette plus lazy default placement (QuickImports must not be touched in constructors, see CelestialListPanel).
 * xRatio/yRatio are fractions of the scaled window; with anchorRight the xRatio is the element's right edge and the
 * element keeps that edge while its width changes, with anchorBottom the yRatio is the bottom edge.
 */
abstract class OldCelestialDraggable extends CelestialDraggable {
   protected static final int OLD_BODY = 0xC80C0C10;
   protected static final int OLD_RED = 0xFFD92B3A;
   protected static final int OLD_RED_DARK = 0xFF3B0E14;
   protected static final int OLD_DIM = 0xFFB8B8C0;

   private final float xRatio;
   private final float yRatio;
   private final boolean anchorRight;
   private final boolean anchorBottom;
   private boolean placed;
   private float rightEdge = -1.0F;

   protected OldCelestialDraggable(String name, int width, int height, float xRatio, float yRatio, boolean anchorRight, boolean anchorBottom) {
      super(name, 0, 0, width, height);
      this.xRatio = xRatio;
      this.yRatio = yRatio;
      this.anchorRight = anchorRight;
      this.anchorBottom = anchorBottom;
   }

   @Override
   public void setX(int x) {
      super.setX(x);
      this.placed = true;
   }

   @Override
   public void setY(int y) {
      super.setY(y);
      this.placed = true;
   }

   protected void ensurePlaced() {
      if (this.placed) {
         return;
      }

      this.placed = true;
      float windowW = window.getScaledWidth();
      float windowH = window.getScaledHeight();
      if (this.anchorRight) {
         this.rightEdge = windowW * this.xRatio;
         super.setX((int) (this.rightEdge - this.getWidth()));
      } else {
         super.setX((int) (windowW * this.xRatio));
      }

      super.setY((int) (this.anchorBottom ? windowH * this.yRatio - this.getHeight() : windowH * this.yRatio));
   }

   protected void layout(float width, float height) {
      this.ensurePlaced();
      int w = (int) Math.ceil(width);
      int h = (int) Math.ceil(height);
      if (this.anchorRight) {
         if (this.rightEdge < 0.0F || this.isDragging()) {
            this.rightEdge = this.getX() + this.getWidth();
         }

         this.setWidth(w);
         if (!this.isDragging()) {
            super.setX(Math.round(this.rightEdge) - w);
         }
      } else {
         this.setWidth(w);
      }

      this.setHeight(h);
   }
}
