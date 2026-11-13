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
