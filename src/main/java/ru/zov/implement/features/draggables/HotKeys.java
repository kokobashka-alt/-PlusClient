package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.module.Module;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;

public class HotKeys extends AbstractDraggable {
   private List<Module> keysList = new ArrayList();

   public HotKeys() {
      super("Hot Keys", 300, 10, 80, 23, true);
   }

   public boolean visible() {
      return !this.keysList.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   public void tick() {
      this.keysList = main.getModuleProvider().getModules().stream().filter((module) -> module.getAnimation().getOutput().floatValue() != 0.0F && module.getKey() != -1).toList();
   }

   public void drawDraggable(DrawContext e) {
      org.joml.Matrix3x2fStack matrix = e.getMatrices();
      float centerX = (float)this.getX() + (float)this.getWidth() / 2.0F;
      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)17.5F).round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)(this.getY() + 17), (double)this.getWidth(), (double)(this.getHeight() - 17)).round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      font.drawString(matrix, this.getName(), (double)((int)(centerX - font.getStringWidth(this.getName()) / 2.0F)), (double)(this.getY() + 7), ColorUtil.getText());
      int offset = 23;
      int maxWidth = 80;

      for(Module module : this.keysList) {
         String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";
         float centerY = (float)(this.getY() + offset);
         float animation = module.getAnimation().getOutput().floatValue();
         String var10001 = module.getName();
         float width = fontModule.getStringWidth(var10001 + bind) + 15.0F;
         MathUtil.scale(matrix, centerX, centerY, 1.0F, animation, () -> {
            fontModule.drawString(matrix, module.getName(), (double)(this.getX() + 6), (double)(centerY + 1.0F), ColorUtil.getText());
            fontModule.drawString(matrix, bind, (double)((float)(this.getX() + this.getWidth() - 6) - fontModule.getStringWidth(bind)), (double)(centerY + 1.0F), ColorUtil.getText());
         });
         offset += (int)(animation * 11.0F);
         maxWidth = (int)Math.max(width, (float)maxWidth);
      }

      this.setWidth(maxWidth);
      this.setHeight(offset);
   }
}
