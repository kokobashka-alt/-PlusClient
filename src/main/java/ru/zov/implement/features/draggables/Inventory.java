package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.render.Render2DUtil;

public class Inventory extends AbstractDraggable {
   List<ItemStack> stacks = new ArrayList();

   public Inventory() {
      super("Inventory", 390, 10, 123, 60, true);
   }

   public boolean visible() {
      return !this.stacks.stream().filter((stack) -> !stack.isEmpty()).toList().isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   public void tick() {
      this.stacks = IntStream.range(9, 36).mapToObj((i) -> mc.player.inventory.getStack(i)).toList();
   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)17.5F).round(5.0F, 0.0F, 5.0F, 0.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)(this.getY() + 17), (double)this.getWidth(), (double)(this.getHeight() - 17)).quality(25.0F).round(0.0F, 5.0F, 0.0F, 5.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      Fonts.getSize(15, Fonts.Type.DEFAULT).drawCenteredString(matrix, this.getName(), (double)((float)this.getX() + (float)this.getWidth() / 2.0F), (double)(this.getY() + 7), ColorUtil.getText());
      int offsetY = 20;
      int offsetX = 4;

      for(ItemStack stack : this.stacks) {
         Render2DUtil.defaultDrawStack(context, stack, (float)(this.getX() + offsetX), (float)(this.getY() + offsetY), false, true, 0.5F);
         offsetX += 13;
         if (offsetX > this.getWidth() - 13) {
            offsetY += 13;
            offsetX = 4;
         }
      }

   }
}
