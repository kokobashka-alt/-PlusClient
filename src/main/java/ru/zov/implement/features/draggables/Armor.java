package ru.zov.implement.features.draggables;

import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.features.modules.render.Hud;

public class Armor extends AbstractDraggable {
   public Armor() {
      super("Armor", 0, 0, 82, 22, false);
   }

   public boolean visible() {
      return java.util.Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmorSlot).map(mc.player::getEquippedStack).anyMatch((s) -> !s.isEmpty());
   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      int handX = ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getMainArm().equals(Arm.RIGHT) ? 100 : -187;
      if (Hud.getInstance().interfaceSettings.isSelected("HotBar")) {
         this.setX(context.getScaledWindowWidth() / 2 + handX);
         this.setY(context.getScaledWindowHeight() - 27);
         blur.render(ShapeProperties.create(matrix, (double)((float)this.getX() - 0.5F), (double)((float)this.getY() - 0.5F), (double)(this.getWidth() + 1), (double)23.0F).round(3.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
         this.drawArmorStacks(context, 3, 3);
      } else {
         this.setX(context.getScaledWindowWidth() / 2 + handX);
         this.setY(context.getScaledWindowHeight() - 19);
         ScissorManager scissor = main.getScissorManager();
         int width = 41;
         scissor.push(matrix, (float)(this.getX() - 3), (float)(this.getY() - 3), (float)width, 22.0F);
         Render2DUtil.drawTexturedQuad(matrix, InGameHud.HOTBAR_TEXTURE, this.getX() - 3, this.getX() - 3 + 182, this.getY() - 3, this.getY() - 3 + 22, 0, 1, 0, 1, -1);
         scissor.pop();
         scissor.push(matrix, (float)(this.getX() + width - 3), (float)(this.getY() - 3), (float)width, 22.0F);
         Render2DUtil.drawTexturedQuad(matrix, InGameHud.HOTBAR_TEXTURE, this.getX() - 103, this.getX() - 103 + 182, this.getY() - 3, this.getY() - 3 + 22, 0, 1, 0, 1, -1);
         scissor.pop();
         this.drawArmorStacks(context, 0, 0);
      }

   }

   public void drawArmorStacks(DrawContext context, int offsetX, int offsetY) {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (!slot.isArmorSlot()) {
            continue;
         }
         ItemStack itemStack = Objects.requireNonNull(mc.player).getEquippedStack(slot);
         context.drawItem(itemStack, this.getX() + offsetX, this.getY() + offsetY, 1);
         context.drawStackOverlay(mc.textRenderer, itemStack, this.getX() + offsetX, this.getY() + offsetY);
         offsetX += 20;
      }

   }
}
