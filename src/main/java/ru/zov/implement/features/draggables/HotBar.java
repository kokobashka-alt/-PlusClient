package ru.zov.implement.features.draggables;

import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render2DUtil;

public class HotBar extends AbstractDraggable {
   private float selectItemX;

   public HotBar() {
      super("HotBar", 0, 50, 182, 22, false);
   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      PlayerInventory inventory = ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getInventory();
      ItemStack offHand = mc.player.getOffHandStack();
      this.selectItemX = MathUtil.interpolateSmooth((double)1.0F, this.selectItemX, (float)(inventory.selectedSlot * 20));
      this.setX((mc.getWindow().getScaledWidth() - this.getWidth()) / 2);
      this.setY(mc.getWindow().getScaledHeight() - 27);
      blur.render(ShapeProperties.create(matrix, (double)((float)this.getX() - 0.5F), (double)((float)this.getY() - 0.5F), (double)(this.getWidth() + 1), (double)23.0F).quality(100.0F).round(3.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() + this.selectItemX + 1.0F), (double)(this.getY() + 1), (double)20.0F, (double)20.0F).round(2.25F).thickness(3.0F).outlineColor(ColorUtil.getClientColor()).color(ColorUtil.getRect(0.0F)).build());
      IntStream.range(0, 9).forEach((i) -> this.drawStack(context, (ItemStack)inventory.main.get(i), (float)(this.getX() + i * 20 + 2), (float)(this.getY() + 2), false));
      if (!offHand.isEmpty()) {
         this.drawStack(context, offHand, (float)(this.getX() + (((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getMainArm().equals(Arm.RIGHT) ? -28 : 198)), (float)(this.getY() + 2), true);
      }

      if (!mc.player.isSpectator() && !mc.player.isCreative()) {
         this.drawExperienceBar(matrix);
      }

      this.drawOverlayInfo(matrix);
   }

   public void drawExperienceBar(org.joml.Matrix3x2fStack matrix) {
      Fonts.getSize(16).drawCenteredString(matrix, "" + mc.player.experienceLevel, (double)((float)window.getScaledWidth() / 2.0F), (double)((float)this.getY() - 9.5F), ColorUtil.GREEN);
   }

   public void drawOverlayInfo(org.joml.Matrix3x2fStack matrix) {
      float scaledWidth = (float)mc.getWindow().getScaledWidth() / 2.0F;
      float heightStart = (float)(mc.getWindow().getScaledHeight() - 75);
      float paddingX = 4.0F;
      float paddingY = 3.0F;
      FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
      if (mc.inGameHud.heldItemTooltipFade > 0 && mc.inGameHud.currentStack != null) {
         float alpha = (float)mc.inGameHud.heldItemTooltipFade * 256.0F / 10.0F / 255.0F;
         Text text = mc.inGameHud.currentStack.getName();
         float width = font.getStringWidth(text);
         int x = (int)(scaledWidth - width / 2.0F);
         MathUtil.multAlpha(alpha, () -> {
            blur.render(ShapeProperties.create(matrix, (double)((float)x - paddingX), (double)(heightStart - paddingY), (double)(width + paddingX * 2.0F), (double)(font.getStringHeight(text) / 2.15F + paddingY * 2.0F)).round(2.5F).color(ColorUtil.getRect(0.7F)).build());
            font.drawText(matrix, text, (double)x, (double)(heightStart + 2.5F));
         });
      }

      if (mc.inGameHud.overlayRemaining > 0 && mc.inGameHud.overlayMessage != null && !mc.inGameHud.overlayMessage.getString().isEmpty()) {
         float alpha = (float)mc.inGameHud.overlayRemaining * 256.0F / 10.0F / 255.0F;
         Text text = mc.inGameHud.overlayMessage;
         float width = font.getStringWidth(text);
         int x = (int)(scaledWidth - width / 2.0F);
         MathUtil.multAlpha(alpha, () -> {
            blur.render(ShapeProperties.create(matrix, (double)((float)x - paddingX), (double)(heightStart - paddingY - 17.0F), (double)(width + paddingX * 2.0F), (double)(font.getStringHeight(text) / 2.15F + paddingY * 2.0F)).round(2.5F).color(ColorUtil.getRect(0.7F)).build());
            font.drawText(matrix, text, (double)x, (double)(heightStart - 14.5F));
         });
      }

   }

   public void drawStack(DrawContext context, ItemStack stack, float x, float y, boolean offHand) {
      if (offHand) {
         blur.render(ShapeProperties.create(context.getMatrices(), (double)(x - 2.5F), (double)(y - 2.5F), (double)23.0F, (double)23.0F).round(3.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      }

      Render2DUtil.defaultDrawStack(context, stack, x, y, false, true, 1.0F);
   }
}
