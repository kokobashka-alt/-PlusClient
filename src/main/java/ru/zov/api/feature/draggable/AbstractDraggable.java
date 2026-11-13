package ru.zov.api.feature.draggable;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.common.QuickImports;
import ru.zov.common.QuickLogger;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.events.container.OpenScreenEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.features.modules.render.Hud;
import ru.zov.implement.features.modules.render.CelestialHud;
import ru.zov.implement.features.modules.render.PouchHud;

public abstract class AbstractDraggable implements Draggable, QuickImports, QuickLogger {
   private String name;
   private boolean dragging;
   private boolean canDrag;
   private boolean changeSize;
   private boolean visible = true;
   private int x;
   private int y;
   private int width;
   private int height;
   double dragX;
   double dragY;
   private double size = (double)1.0F;
   public final Animation scaleAnimation = (new DecelerateAnimation()).setValue((double)1.0F).setMs(200);

   public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.canDrag = canDrag;
   }

   public boolean visible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public void tick() {
   }

   public void packet(PacketEvent e) {
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      Hud hud = Hud.getInstance();
      double mouseDragX = (double)mouseX + this.dragX;
      double mouseDragY = (double)mouseY + this.dragY;
      int windowWidth = window.getScaledWidth();
      int windowHeight = window.getScaledHeight();
      if (this.dragging) {
         int radius = 3;
         this.x = (int)Math.max((double)0.0F, Math.min(mouseDragX, (double)(windowWidth - this.getWidth())));
         this.y = (int)Math.max((double)0.0F, Math.min(mouseDragY, (double)(windowHeight - this.getHeight())));

         for(AbstractDraggable drag : main.getDraggableRepository().draggable()) {
            if (drag.canDraw(hud, drag) && drag.canDrag && drag != this) {
               int x1 = drag.x + drag.getWidth() + radius;
               int x2 = drag.x - this.getWidth() - radius;
               int y1 = drag.y + drag.getHeight() + radius;
               int y2 = drag.y - this.getHeight() - radius;
               int y3 = drag.y;
               if (Math.abs((double)x1 - mouseDragX) <= (double)radius) {
                  this.drawRect((float)x1 - 1.5F, 0.0F, 1.0F, (float)windowHeight);
                  this.x = x1;
               }

               if (Math.abs((double)x2 - mouseDragX) <= (double)radius) {
                  this.drawRect((float)(x2 + this.getWidth() + 1), 0.0F, 1.0F, (float)windowHeight);
                  this.x = x2;
               }

               if (Math.abs((double)y1 - mouseDragY) <= (double)radius) {
                  this.drawRect(0.0F, (float)y1 - 1.5F, (float)windowWidth, 1.0F);
                  this.y = y1;
               }

               if (Math.abs((double)y2 - mouseDragY) <= (double)radius) {
                  this.drawRect(0.0F, (float)(y2 + this.getHeight() + 1), (float)windowWidth, 1.0F);
                  this.y = y2;
               }

               if (Math.abs((double)y3 - mouseDragY) <= (double)radius) {
                  this.drawRect(0.0F, (float)y3 - 1.5F, (float)windowWidth, 1.0F);
                  this.y = y3;
               }
            }
         }

         if (Math.abs(this.x + (this.getWidth() - windowWidth) / 2) <= radius) {
            this.drawRect((float)windowWidth / 2.0F - 0.5F, 0.0F, 1.0F, (float)windowHeight);
            this.x = (windowWidth - this.getWidth()) / 2;
         }

         if (Math.abs(this.y + (this.getHeight() - windowHeight) / 2) <= radius) {
            this.drawRect(0.0F, (float)windowHeight / 2.0F - 0.5F, (float)windowWidth, 1.0F);
            this.y = (windowHeight - this.getHeight()) / 2;
         }
      }

   }

   public void openScreen(OpenScreenEvent e) {
      this.reset();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.getWidth(), (double)this.getHeight()) && button == 0 && this.canDrag) {
         this.dragX = (double)this.x - mouseX;
         this.dragY = (double)this.y - mouseY;
         this.dragging = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.reset();
      return true;
   }

   public abstract void drawDraggable(DrawContext var1);

   public void drawRect(float x, float y, float width, float height) {
      Render2DUtil.drawQuad(x, y, width, height, ColorUtil.getText(0.5F));
   }

   public void stopAnimation() {
      this.scaleAnimation.setDirection(Direction.BACKWARDS);
   }

   public void startAnimation() {
      this.scaleAnimation.setDirection(Direction.FORWARDS);
   }

   public void validPosition() {
      if (this.x + this.getWidth() > window.getScaledWidth()) {
         this.x = window.getScaledWidth() - this.getWidth();
      }

      if (this.y + this.getHeight() > window.getScaledHeight()) {
         this.y = window.getScaledHeight() - this.getHeight();
      }

      if (this.y < 0) {
         this.y = 0;
      }

      if (this.x < 0) {
         this.x = 0;
      }

   }

   private void reset() {
      GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
      this.changeSize = false;
      this.dragging = false;
   }

   public boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public boolean isBackwardsFinished() {
      return this.scaleAnimation.isFinished(Direction.BACKWARDS);
   }

   public boolean canDraw(Hud hud, AbstractDraggable draggable) {
      if (draggable.getName().startsWith("Celestial")) {
         CelestialHud celestialHud = CelestialHud.getInstance();
         return celestialHud != null && celestialHud.isState() && celestialHud.elements.isSelected(draggable.getName()) && this.visible();
      }

      if (draggable.getName().startsWith("Pouch")) {
         PouchHud pouchHud = PouchHud.getInstance();
         return pouchHud != null && pouchHud.isState() && pouchHud.elements.isSelected(draggable.getName()) && this.visible();
      }

      return hud.isState() && hud.interfaceSettings.isSelected(draggable.getName()) && this.visible();
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   public void setCanDrag(boolean canDrag) {
      this.canDrag = canDrag;
   }

   public void setChangeSize(boolean changeSize) {
      this.changeSize = changeSize;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setDragX(double dragX) {
      this.dragX = dragX;
   }

   public void setDragY(double dragY) {
      this.dragY = dragY;
   }

   public void setSize(double size) {
      this.size = size;
   }

   public String getName() {
      return this.name;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public boolean isCanDrag() {
      return this.canDrag;
   }

   public boolean isChangeSize() {
      return this.changeSize;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public double getDragX() {
      return this.dragX;
   }

   public double getDragY() {
      return this.dragY;
   }

   public double getSize() {
      return this.size;
   }

   public Animation getScaleAnimation() {
      return this.scaleAnimation;
   }
}
